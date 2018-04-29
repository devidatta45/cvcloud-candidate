package com.cvcloud.controllers

import javax.inject.{Inject, Singleton}

import akka.actor.Props
import akka.util.Timeout
import com.cvcloud.services._
import com.cvcloud.utils.BaseAuthentication._
import com.cvcloud.utils.JsonImplicits._
import com.cvcloud.utils.{ActorCreator, Constants, FindAllCommand, FindByIdCommand}
import play.api.mvc.{Controller, Session => _, _}
import akka.pattern.ask
import reactivemongo.bson.BSONObjectID
import Constants._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/**
  * Created by Donald Pollock on 23/05/2017.
  */
@Singleton
class DegreeController @Inject()(creator: ActorCreator) extends Controller {
  implicit val timeout: Timeout = Constants.TIMEOUT
  val degreeActor = creator.createActorRef(Props(classOf[DegreeActor],DegreeRepositoryImpl), "DegreeActor")
  val sessionActor = creator.createActorRef(Props(classOf[SessionActor],SessionRepositoryImpl), "SessionActor3")

  val getAllDegree = Action.async { request =>
    val apiKey = request.headers.get("apiKey")
    val authResult = auth(sessionActor, apiKey)
    authResult.flatMap(auth => {
      if (auth.nonEmpty) {
        val actualResult = ask(degreeActor, FindAllCommand).
          mapTo[Future[List[Degree]]].flatMap(identity)
        actualResult.map(result => {
          if (result.nonEmpty) Ok(toJson(result))
          else NoContent
        }).recover({
          case ex =>
            degreeActor ! ex
            InternalServerError(toJson(StatusMessage(ex.getMessage)))
        })
      }
      else {
        Future(InternalServerError(toJson(StatusMessage(SESSION_EXPIRED))))
      }
    })
  }

  def getDegreeById(id: String) = Action.async { request =>
    val apiKey = request.headers.get("apiKey")
    val authResult = auth(sessionActor, apiKey)
    authResult.flatMap(auth => {
      if (auth.nonEmpty) {
        val actualResult = ask(degreeActor, FindByIdCommand(BSONObjectID.parse(id).get)).
          mapTo[Future[List[Degree]]].flatMap(identity)
        actualResult.map(result => {
          if (result.nonEmpty) Ok(toJson(result.head))
          else NoContent
        }).recover({
          case ex =>
            degreeActor ! ex
            InternalServerError(toJson(StatusMessage(ex.getMessage)))
        })
      }
      else {
        Future(InternalServerError(toJson(StatusMessage(SESSION_EXPIRED))))
      }
    })
  }
}
