package com.cvcloud.services

/**
  * Created by DDM on 20-04-2017.
  */

import java.util.UUID

import akka.actor.{Actor, ActorRef}
import akka.pattern.ask
import com.cvcloud.utils._
import reactivemongo.bson.{BSONDocument, BSONDocumentReader, BSONDocumentWriter, BSONObjectID}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._
import CVUserColumnConstants._
import JsonImplicits._
import akka.actor.Actor.Receive
import akka.actor.Status.Success

//Model class and helper classes
case class CVUser(override val _id: String, title: String, firstName: String, lastName: String,
                  email: String, password: String, clientRef: Option[String],
                  mobile: String, address: String, country: String, postCode: String) extends BaseEntity

case class Login(email: String, password: String)

case class LoginWithSession(login: Login, actorRef: ActorRef)

case class LoginResult(user: CVUser, token: Option[String])

//Companion object for Mapping
object CVUser {

  implicit object UserReader extends BSONDocumentReader[CVUser] {
    def read(doc: BSONDocument): CVUser = {
      val id = doc.getAs[BSONObjectID](ID).get
      val title = doc.getAs[String](TITLE).get
      val firstName = doc.getAs[String](FIRST_NAME).get
      val lastName = doc.getAs[String](LAST_NAME).get
      val email = doc.getAs[String](EMAIL).get
      val password = doc.getAs[String](PASSWORD).get
      val clientRef = doc.getAs[String](CLIENT_REF_NUMBER)
      val mobile = doc.getAs[String](MOBILE).get
      val address = doc.getAs[String](ADDRESS).get
      val country = doc.getAs[String](COUNTRY).get
      val postCode = doc.getAs[String](POST_CODE).get
      CVUser(id.stringify, title, firstName, lastName, email, password, clientRef, mobile, address, country, postCode)
    }
  }

  implicit object UserWriter extends BSONDocumentWriter[CVUser] {
    def write(user: CVUser): BSONDocument = {
      val id = BSONObjectID.generate()
      BSONDocument(ID -> id,
        TITLE -> user.title,
        FIRST_NAME -> user.firstName,
        LAST_NAME -> user.lastName,
        EMAIL -> user.email,
        PASSWORD -> user.password,
        MOBILE -> user.mobile,
        ADDRESS -> user.address,
        COUNTRY -> user.country,
        POST_CODE -> user.postCode,
        ISREMOVED -> user.isRemoved)
    }
  }

}

//Repository Based on Model. if any extra db related method required apart from Base that can be done here
class CVUserRepository extends BaseRepository[CVUser] {
  override def table: String = Constants.USER_TABLE

  def getUsersByIds(cmd: List[BSONObjectID]): Future[List[CVUser]] = {
    val query = BSONDocument(ID -> BSONDocument("$in" -> cmd))
    filterQuery(query)
  }

  def changePassword(cmd: ForgetPassword): Future[List[CVUser]] = {
    for {
      filteredQuery <- filterQuery(BSONDocument(EMAIL -> cmd.email))
      updateQuery <- updateById(BSONObjectID.parse(filteredQuery.head._id).get, BSONDocument("$set" -> BSONDocument(PASSWORD -> cmd.link)))
    } yield updateQuery
  }

  def updateUser(cmd: UserUpdateCommand[CVUser]): Future[List[CVUser]] = {
    val document = BSONDocument(
      "$set" -> BSONDocument(
        TITLE -> cmd.user.title,
        FIRST_NAME -> cmd.user.firstName,
        LAST_NAME -> cmd.user.lastName,
        EMAIL -> cmd.user.email,
        PASSWORD -> cmd.user.password,
        MOBILE -> cmd.user.mobile,
        ADDRESS -> cmd.user.address,
        COUNTRY -> cmd.user.country,
        POST_CODE -> cmd.user.postCode))
    updateById(cmd.id, document)
  }

  def checkAuthentication(loginWithSession: LoginWithSession): Future[LoginResult] = {
    val document = BSONDocument(EMAIL -> loginWithSession.login.email, PASSWORD -> loginWithSession.login.password)
    val uuid = UUID.randomUUID().toString
    val token = JwtUtility.createToken(toJson(Token(uuid)))
    for {
      result <- filterQuery(document)
      finalResult <- if (result.nonEmpty) {
        val session = Session("", result.head._id, uuid)
        val res = ask(loginWithSession.actorRef, session)(5.seconds).mapTo[Future[Boolean]]
        res.flatMap(identity)
      } else Future(false)
      loginResult = if (finalResult) {
        LoginResult(result.head, Some(token))
      } else LoginResult(result.headOption.getOrElse(CVUser("", "", "", "", "", "", None, "", "", "", "")), None)
    } yield loginResult
  }

  def saveUser(user: CVUser): Future[List[CVCCandidate]] = {
    for {
      saveToDb <- save(user)
      saveMapping <- UserKeyMappingRepositoryImpl.save(UserKeyMapping(BSONObjectID.generate().stringify, user.email))
      document = BSONDocument(EMAIL -> user.email, PASSWORD -> user.password)
      filtered <- filterQuery(document)
      candidate = CVCCandidate("", filtered.head._id, None, Nil, Nil, Audit(filtered.head._id), Audit(filtered.head._id), None, None)
      insertCandidate <- CVCCandidateRepositoryImpl.save(candidate)
      newDocument = BSONDocument(CVCCandidateColumnConstants.CVC_USER_ID -> BSONObjectID.parse(filtered.head._id).get)
      finalResult <- CVCCandidateRepositoryImpl.filterQuery(newDocument)
    } yield finalResult
  }
}

//Implementation Object for Repository
object CVUserRepositoryImpl extends CVUserRepository

//Service for Data Model. If any server side logic will be there that will be done here.
class CVUserActor(repository: CVUserRepository) extends BaseActor {

  override def normalExecution: Receive = {
    case FindAllCommand => sender ! repository.findAll()
    case login: LoginWithSession => sender ! repository.checkAuthentication(login)
    case user: CVUser => sender ! repository.saveUser(user)
    case cmd: FindByIdCommand => sender ! repository.findById(cmd.id)
    case cmd: DeleteByIdCommand => sender ! repository.deleteById(cmd.id)
    case cmd: UserUpdateCommand[CVUser]@unchecked => sender ! repository.updateUser(cmd)
    case cmd: FindByIdsCommand => sender ! repository.getUsersByIds(cmd.ids)
    case cmd: ForgetPassword => sender ! repository.changePassword(cmd)
  }
}