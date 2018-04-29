package com.cvcloud.controllers

import javax.inject.Inject

import akka.actor.Props
import akka.pattern.ask
import akka.util.Timeout
import com.cvcloud.services._
import com.cvcloud.utils.BaseAuthentication._
import com.cvcloud.utils.JsonImplicits._
import com.cvcloud.utils.{ActorCreator, Constants}
import play.api.mvc.{Action, Controller}
import Constants._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/**
  * Created by Donald Pollock on 08/05/2017.
  */
class MessageController @Inject()(creator: ActorCreator) extends Controller {
  implicit val timeout: Timeout = Constants.TIMEOUT
  val messageActor = creator.createActorRef(Props(classOf[ChatMessageActor],ChatMessageRepositoryImpl), "ChatMessageActor")
  val sessionActor = creator.createActorRef(Props(classOf[SessionActor],SessionRepositoryImpl), "SessionActor2")

  val sendMessage = Action.async { request =>
    val apiKey = request.headers.get("apiKey")
    val authResult = auth(sessionActor, apiKey)
    authResult.flatMap(auth => {
      if (auth.nonEmpty) {
        val json = request.body.asJson.get
        val message = json.toString()
        val chatMessage = extractEntity[ChatMessage](message)
        val result = ask(messageActor, chatMessage).mapTo[Future[Either[Future[Boolean], Future[List[ChatMessage]]]]]
        val actualResult = result.flatMap(identity)
        actualResult.map {
          case Left(s) => Ok(toJson(StatusMessage("Message Sent")))
          case Right(i) => Ok(toJson(StatusMessage("Message Sent")))
        }.recover({
          case ex =>
            messageActor ! ex
            InternalServerError(toJson(StatusMessage(ex.getMessage)))
        })
      }
      else {
        Future(InternalServerError(toJson(StatusMessage(SESSION_EXPIRED))))
      }
    })
  }

  def showAllMessages(userId: String) = Action.async { request =>
    val apiKey = request.headers.get("apiKey")
    val authResult = auth(sessionActor, apiKey)
    authResult.flatMap(auth => {
      if (auth.nonEmpty) {
        val result = ask(messageActor, userId).mapTo[Future[List[ChatMessageView]]]
        val actualResult = result.flatMap(identity)
        actualResult.map { res =>
          if (res.nonEmpty) {
            val groupedResult: List[(String, List[ChatMessageView])] = res.groupBy(_.firstUserId.get._id).toList
            val finalResult = groupedResult.map { result =>
              val chatMessage = result._2.find(x => x.firstUserId.get._id == result._1)
              MessageWithOwner(chatMessage.get.firstUserId, result._2.head.consoleId, result._2)
            }
            Ok(toJson(finalResult))
          }
          else NoContent
        }.recover({
          case ex =>
            messageActor ! ex
            InternalServerError(toJson(StatusMessage(ex.getMessage)))
        })
      }
      else {
        Future(InternalServerError(toJson(StatusMessage(SESSION_EXPIRED))))
      }
    })
  }

  def showAllClientMessages(userId: String) = Action.async { request =>
    val apiKey = request.headers.get("apiKey")
    val authResult = auth(sessionActor, apiKey)
    authResult.flatMap(auth => {
      if (auth.nonEmpty) {
        val result = ask(messageActor, ClientCommand(userId)).mapTo[Future[List[ChatMessageClientView]]]
        val actualResult = result.flatMap(identity)
        actualResult.map { res =>
          if (res.nonEmpty) {
            val groupedResult: List[(String, List[ChatMessageClientView])] = res.groupBy(_.secondUserId.get._id).toList
            val finalResult = groupedResult.map { result =>
              val chatMessage = result._2.find(x => x.secondUserId.get._id == result._1)
              MessageWithOwnerClient(chatMessage.get.secondUserId, result._2.head.consoleId, result._2)
            }
            Ok(toJson(finalResult))
          }
          else NoContent
        }.recover({
          case ex =>
            messageActor ! ex
            InternalServerError(toJson(StatusMessage(ex.getMessage)))
        })
      }
      else {
        Future(InternalServerError(toJson(StatusMessage(SESSION_EXPIRED))))
      }
    })
  }
}

case class MessageWithOwner(owner: Option[CVUser], consoleId: String, messages: List[ChatMessageView])

case class MessageWithOwnerClient(owner: Option[CVUser], consoleId: String, messages: List[ChatMessageClientView])