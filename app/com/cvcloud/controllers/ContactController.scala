package com.cvcloud.controllers

import javax.inject.{Inject, Singleton}

import akka.actor.Props
import akka.util.Timeout
import com.cvcloud.services._
import com.cvcloud.utils.BaseAuthentication._
import com.cvcloud.utils.JsonImplicits._
import com.cvcloud.utils.{ActorCreator, Constants, FindAllCommand}
import play.api.mvc.{Action, Controller}

import scala.concurrent.Future
import akka.pattern.ask

import scala.concurrent.ExecutionContext.Implicits.global
import Constants._

/**
  * Created by Donald Pollock on 25/05/2017.
  */
@Singleton
class ContactController @Inject()(creator: ActorCreator) extends Controller {
  implicit val timeout: Timeout = Constants.TIMEOUT
  val contactActor = creator.createActorRef(Props(classOf[ContactActor],ContactRepositoryImpl), "ContactActor")
  val sessionActor = creator.createActorRef(Props(classOf[SessionActor],SessionRepositoryImpl), "SessionActor4")

  val getAllContact = Action.async { request =>
    val apiKey = request.headers.get("apiKey")
    val authResult = auth(sessionActor, apiKey)
    authResult.flatMap(auth => {
      if (auth.nonEmpty) {
        val actualResult = ask(contactActor, FindAllCommand).
          mapTo[Future[List[Contact]]].flatMap(identity)
        actualResult.map(result => {
          if (result.nonEmpty) Ok(toJson(result))
          else NoContent
        }).recover({
          case ex =>
            contactActor ! ex
            InternalServerError(toJson(StatusMessage(ex.getMessage)))
        })
      }
      else {
        Future(InternalServerError(toJson(StatusMessage(SESSION_EXPIRED))))
      }
    })
  }

  val createContact = Action.async { request =>
    val apiKey = request.headers.get("apiKey")
    val authResult = auth(sessionActor, apiKey)
    authResult.flatMap(auth => {
      if (auth.nonEmpty) {
        val json = request.body.asJson.get
        val message = json.toString()
        val contact = extractEntity[Contact](message)
        val result = ask(contactActor, contact).mapTo[Future[Boolean]]
        val actualResult = result.flatMap(identity)
        actualResult.map { res =>
          if (res) {
            Ok(toJson(StatusMessage("Message Sent")))
          }
          else {
            InternalServerError(toJson(StatusMessage("Message Not Sent")))
          }
        }.recover({
          case ex =>
            contactActor ! ex
            InternalServerError(toJson(StatusMessage(ex.getMessage)))
        })
      }
      else {
          Future(InternalServerError(toJson(StatusMessage(SESSION_EXPIRED))))
      }
    })
  }
}
