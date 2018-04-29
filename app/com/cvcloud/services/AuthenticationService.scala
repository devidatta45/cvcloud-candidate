package com.cvcloud.services

import akka.actor.Actor
import com.cvcloud.utils._
import reactivemongo.bson.{BSONDocument, BSONDocumentReader, BSONDocumentWriter, BSONObjectID}

import scala.concurrent.ExecutionContext.Implicits.global
import SessionColumnConstants._

import scala.concurrent.Future
import JsonImplicits._

/**
  * Created by Donald Pollock on 19/05/2017.
  */
case class Session(override val _id: String, userId: String, token: String) extends BaseEntity

object Session {

  implicit object SessionReader extends BSONDocumentReader[Session] {
    def read(doc: BSONDocument): Session = {
      val id = doc.getAs[BSONObjectID](ID).get
      val userId = doc.getAs[BSONObjectID](USER_ID).get
      val token = doc.getAs[String](TOKEN).get
      Session(id.stringify, userId.stringify, token)
    }
  }

  implicit object SessionWriter extends BSONDocumentWriter[Session] {
    def write(session: Session): BSONDocument = {
      val id = BSONObjectID.generate()
      val userId = BSONObjectID.parse(session.userId).get
      BSONDocument(ID -> id,
        USER_ID -> userId,
        TOKEN -> session.token,
        ISREMOVED -> session.isRemoved)
    }
  }

}

class SessionRepository extends BaseRepository[Session] {
  override def table: String = Constants.SESSION

  def updateRunningStatus(token: String): Future[Int] = {
    val decodedApiKey = if (JwtUtility.isValidToken(token)) {
      JwtUtility.decodePayload(token)
    } else None
    val key = if (decodedApiKey.isDefined) Some(extractEntity[Token](decodedApiKey.get)) else None
    val finalToken = if (key.isDefined) key.get.key else ""
    for {
      result <- filterQuery(BSONDocument(TOKEN -> finalToken))
      deleteSession <- deleteById(BSONObjectID.parse(result.head._id).get)
    } yield deleteSession
  }
}

object SessionRepositoryImpl extends SessionRepository

class SessionActor(repository: SessionRepository) extends BaseActor {

  override def normalExecution: Receive = {
    case cmd: Session => sender ! repository.save(cmd)
    case token: String => sender ! repository.updateRunningStatus(token)
    case command: AuthenticationCommand => sender ! repository.filterQuery(BSONDocument(TOKEN -> command.token))
  }
}

case class AuthenticationCommand(token: String)