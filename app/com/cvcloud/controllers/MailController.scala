package com.cvcloud.controllers

import javax.inject.{Inject, Singleton}

import akka.actor.Props
import akka.pattern.ask
import akka.util.Timeout
import com.cvcloud.services.{CVUser, ForgetPassword, MailService}
import com.cvcloud.utils.JsonImplicits._
import com.cvcloud.utils.{ActorCreator, Constants}
import play.api.mvc.{Action, Controller}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/**
  * Created by Donald Pollock on 28/06/2017.
  */
@Singleton
class MailController @Inject()(creator: ActorCreator) extends Controller {
  implicit val timeout: Timeout = Constants.MAIL_TIMEOUT
  val mailActor = creator.createActorRef(Props(classOf[MailService]), "MailServiceActor")
  val resetPassword = Action.async { request =>
    val json = request.body.asJson.get
    val data = json.toString()
    val passwordData = extractEntity[ForgetPassword](data)
    val futureResult = ask(mailActor, passwordData).mapTo[Future[List[CVUser]]]
    val result = futureResult.flatMap(identity)
    result.map { act =>
      Ok(toJson(act))
    }.recover({
      case ex =>
        mailActor ! ex
        InternalServerError(toJson(StatusMessage(ex.getMessage)))
    })
  }
}
