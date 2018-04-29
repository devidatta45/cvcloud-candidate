package com.cvcloud.services

import com.cvcloud.utils.mail._
import com.cvcloud.utils.{BaseActor, CVUserColumnConstants, Constants, UserKeyColumnConstants}
import reactivemongo.bson.BSONDocument

import scala.concurrent.ExecutionContext.Implicits.global

/**
  * Created by Donald Pollock on 28/06/2017.
  */
class MailService extends BaseActor {
  override def normalExecution: Receive = {
    case cmd: ForgetPassword => {
      val futureResult = for {
        filteredResult <- CVUserRepositoryImpl.filterQuery(BSONDocument(CVUserColumnConstants.EMAIL -> cmd.email))
        filteredKey <- UserKeyMappingRepositoryImpl.filterQuery(BSONDocument(UserKeyColumnConstants.EMAIL -> cmd.email))
        result = if (filteredResult.nonEmpty && filteredKey.nonEmpty) {
          val mail = Mail(
            from = (Constants.SENDER_EMAIL, Constants.SENDER_NAME),
            to = cmd.email,
            subject = "Reset Password",
            message = "please reset you password using link: " + cmd.link + "/" + filteredKey.head._id
          )
          send a mail
          filteredResult
        }
        else {
          filteredResult
        }
      } yield result

      sender ! futureResult
    }
  }
}

case class ForgetPassword(email: String, link: String)