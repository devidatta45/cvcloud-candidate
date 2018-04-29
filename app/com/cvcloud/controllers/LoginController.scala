package com.cvcloud.controllers

import javax.inject.{Inject, Singleton}

import akka.actor.Props
import akka.pattern.ask
import akka.util.Timeout
import com.cvcloud.services._
import com.cvcloud.utils.JsonImplicits._
import com.cvcloud.utils._
import play.api.mvc.{Action, Controller}
import reactivemongo.bson.BSONObjectID

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import Constants._

/**
  * Created by DDM on 19-04-2017.
  */
@Singleton
class LoginController @Inject()(creator: ActorCreator) extends Controller {
  implicit val timeout: Timeout = Constants.TIMEOUT
  val userActor = creator.createActorRef(Props(classOf[CVUserActor], CVUserRepositoryImpl), "CVUserActor")
  val userMappingActor = creator.createActorRef(Props(classOf[UserKeyMappingActor], UserKeyMappingRepositoryImpl), "UserKeyMappingActor")
  val sessionActor = creator.createActorRef(Props(classOf[SessionActor], SessionRepositoryImpl), "SessionActor")

  import BaseAuthentication._

  val index = Action.async { request =>
    val apiKey = request.headers.get("apiKey")
    val authResult = auth(sessionActor, apiKey)
    authResult.flatMap(auth => {
      if (auth.nonEmpty) {
        val result = ask(userActor, FindAllCommand).mapTo[Future[List[CVUser]]]
        val actualResult = result.flatMap(identity)
        actualResult.map { res =>
          if (res.nonEmpty) Ok(toJson(res))
          else NoContent
        }.recover({
          case ex =>
            userActor ! ex
            InternalServerError(toJson(StatusMessage(ex.getMessage)))
        })
      }
      else {
        Future(InternalServerError(toJson(StatusMessage(SESSION_EXPIRED))))
      }
    })
  }

  val getTest = Action.async { request =>
    val result = Future(StatusMessage("Hope to get final result here"))
    result.map { res =>
      Ok(toJson(res))
    }
  }

  val login = Action.async { request =>
    val json = request.body.asJson.get
    val loginData = json.toString()
    val login = extractEntity[Login](loginData)
    val loginWithSession = LoginWithSession(login, sessionActor)
    val result = ask(userActor, loginWithSession).mapTo[Future[LoginResult]]
    val actualResult = result.flatMap(identity)
    actualResult.map { act =>
      Ok(toJson(act))
    }.recover({
      case ex =>
        userActor ! ex
        InternalServerError(toJson(StatusMessage(ex.getMessage)))
    })
  }

  val changePassword = Action.async { request =>
    val json = request.body.asJson.get
    val data = json.toString()
    val changePassword = extractEntity[ForgetPassword](data)
    val result = ask(userActor, changePassword).mapTo[Future[List[CVUser]]]
    val actualResult = result.flatMap(identity)
    actualResult.map { act =>
      Ok(toJson(act))
    }.recover({
      case ex =>
        userActor ! ex
        InternalServerError(toJson(StatusMessage(ex.getMessage)))
    })
  }

  def getMailByKey(key: String) = Action.async { request =>
    val apiKey = request.headers.get("apiKey")
    val authResult = auth(sessionActor, apiKey)
    authResult.flatMap(auth => {
      if (auth.nonEmpty) {
        val result = ask(userMappingActor, key).mapTo[Future[String]]
        val actualResult = result.flatMap(identity)
        actualResult.map { res =>
          Ok(toJson(StatusMessage(res)))
        }.recover({
          case ex =>
            userActor ! ex
            InternalServerError(toJson(StatusMessage(ex.getMessage)))
        })
      }
      else {
        Future(InternalServerError(toJson(StatusMessage(SESSION_EXPIRED))))
      }
    })
  }

  val createUser = Action.async { request =>
    val json = request.body.asJson.get
    val userData = json.toString()
    val user = extractEntity[CVUser](userData)
    val result = ask(userActor, user).mapTo[Future[List[CVCCandidate]]]
    val actualResult = result.flatMap(identity)
    actualResult.map { act =>
      if (act.nonEmpty) Ok(toJson(act.head))
      else NoContent
    }.recover({
      case ex =>
        userActor ! ex
        InternalServerError(toJson(StatusMessage(ex.getMessage)))
    })
  }

  def findUser(id: String) = Action.async { request =>
    val apiKey = request.headers.get("apiKey")
    val authResult = auth(sessionActor, apiKey)
    authResult.flatMap(auth => {
      if (auth.nonEmpty) {
        val result = ask(userActor, FindByIdCommand(BSONObjectID.parse(id).get)).mapTo[Future[List[CVUser]]]
        val actualResult = result.flatMap(identity)
        actualResult.map { res =>
          if (res.nonEmpty) Ok(toJson(res.head))
          else NoContent
        }.recover({
          case ex =>
            userActor ! ex
            InternalServerError(toJson(StatusMessage(ex.getMessage)))
        })
      }
      else {
        Future(InternalServerError(toJson(StatusMessage(SESSION_EXPIRED))))
      }
    })
  }

  val getUsersByIds = Action.async { request =>
    val apiKey = request.headers.get("apiKey")
    val authResult = auth(sessionActor, apiKey)
    authResult.flatMap(auth => {
      if (auth.nonEmpty) {
        val json = request.body.asText.get
        val details = extractEntity[List[BSONObjectID]](json)
        val req = FindByIdsCommand(details)
        val result = ask(userActor, req).mapTo[Future[List[CVUser]]]
        val actualResult = result.flatMap(identity)
        actualResult.map { res =>
          Ok(toJson(res))
        }.recover({
          case ex =>
            userActor ! ex
            InternalServerError(toJson(StatusMessage(ex.getMessage)))
        })
      }
      else {
        Future(InternalServerError(toJson(StatusMessage(SESSION_EXPIRED))))
      }
    })
  }

  def logout(id: String) = Action.async { request =>
    val apiKey = request.headers.get("apiKey")
    val authResult = auth(sessionActor, apiKey)
    authResult.flatMap(auth => {
      if (auth.nonEmpty) {
        val result = ask(sessionActor, id).mapTo[Future[Int]]
        val actualResult = result.flatMap(identity)
        actualResult.map { res =>
          Ok(toJson(StatusMessage("Logged Out")))
        }.recover({
          case ex =>
            sessionActor ! ex
            InternalServerError(toJson(StatusMessage(ex.getMessage)))
        })
      }
      else {
        Future(InternalServerError(toJson(StatusMessage(SESSION_EXPIRED))))
      }
    })
  }

  def deleteUser(id: String) = Action.async { request =>
    val apiKey = request.headers.get("apiKey")
    val authResult = auth(sessionActor, apiKey)
    authResult.flatMap(auth => {
      if (auth.nonEmpty) {
        val result = ask(userActor, DeleteByIdCommand(BSONObjectID.parse(id).get)).mapTo[Future[Int]]
        val actualResult = result.flatMap(identity)
        actualResult.map { res =>
          if (res > 0) Ok(toJson(StatusMessage("User Deleted")))
          else InternalServerError(toJson(StatusMessage("Could Not Delete User ")))
        }.recover({
          case ex =>
            userActor ! ex
            InternalServerError(toJson(StatusMessage(ex.getMessage)))
        })
      }
      else {
        Future(InternalServerError(toJson(StatusMessage(SESSION_EXPIRED))))
      }
    })
  }
}

case class StatusMessage(status: String)
