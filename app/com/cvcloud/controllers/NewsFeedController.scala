package com.cvcloud.controllers

import javax.inject.{Inject, Singleton}

import akka.actor.Props
import akka.util.Timeout
import com.cvcloud.services._
import com.cvcloud.utils.BaseAuthentication._
import com.cvcloud.utils.JsonImplicits._
import com.cvcloud.utils.{ActorCreator, Constants}
import play.api.mvc.{Action, Controller}
import akka.pattern.ask
import Constants._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/**
  * Created by Donald Pollock on 09/06/2017.
  */
@Singleton
class NewsFeedController @Inject()(creator: ActorCreator) extends Controller {
  implicit val timeout: Timeout = Constants.TIMEOUT
  val newsFeedActor = creator.createActorRef(Props(classOf[NewsFeedActor],NewsFeedRepositoryImpl), "NewsFeedActor")
  val sessionActor = creator.createActorRef(Props(classOf[SessionActor],SessionRepositoryImpl), "SessionActor8")

  val createNewsFeed = Action.async { request =>
    val apiKey = request.headers.get("apiKey")
    val authResult = auth(sessionActor, apiKey)
    authResult.flatMap(auth => {
      if (auth.nonEmpty) {
        val json = request.body.asText.get
        val feed = extractEntity[NewsFeed](json)
        val result = ask(newsFeedActor, feed).mapTo[Future[Boolean]]
        val actualResult = result.flatMap(identity)
        actualResult.map { res =>
          if (res) {
            Ok(toJson(StatusMessage("Posted to News Feed")))
          }
          else {
            InternalServerError(toJson(StatusMessage("Not Posted to News Feed")))
          }
        }.recover({
          case ex =>
            newsFeedActor ! ex
            InternalServerError(toJson(StatusMessage(ex.getMessage)))
        })
      }
      else {
        Future(InternalServerError(toJson(StatusMessage(SESSION_EXPIRED))))
      }
    })
  }

  def getAllNews(id: String) = Action.async { request =>
    val apiKey = request.headers.get("apiKey")
    val authResult = auth(sessionActor, apiKey)
    authResult.flatMap(auth => {
      if (auth.nonEmpty) {
        val actualResult = ask(newsFeedActor, id).
          mapTo[Future[List[NewsFeed]]].flatMap(identity)
        actualResult.map { res =>
          if (res.nonEmpty) {
            Ok(toJson(res))
          }
          else {
            NoContent
          }
        }.recover({
          case ex =>
            newsFeedActor ! ex
            InternalServerError(toJson(StatusMessage(ex.getMessage)))
        })
      }
      else {
        Future(InternalServerError(toJson(StatusMessage(SESSION_EXPIRED))))
      }
    })
  }
}
