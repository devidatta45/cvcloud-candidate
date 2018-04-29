package com.cvcloud.controllers

import java.nio.file.Path
import javax.inject.{Inject, Singleton}

import akka.actor.Props
import akka.pattern.ask
import akka.util.Timeout
import com.cvcloud.services._
import com.cvcloud.utils.BaseAuthentication._
import com.cvcloud.utils.JsonImplicits._
import com.cvcloud.utils._
import play.api.libs.Files.TemporaryFile
import play.api.mvc.MultipartFormData.FilePart
import play.api.mvc.{Session => _, _}
import reactivemongo.api.BSONSerializationPack
import reactivemongo.api.gridfs.ReadFile
import reactivemongo.bson.{BSONObjectID, BSONValue}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import Constants._

/**
  * Created by Donald Pollock on 04/05/2017.
  */
@Singleton
class ProfileController @Inject()(creator: ActorCreator) extends Controller {
  implicit val timeout: Timeout = Constants.TIMEOUT
  val candidateActor = creator.createActorRef(Props(classOf[CVCCandidateActor], CVCCandidateRepositoryImpl), "CVCCandidateActor1")
  val sessionActor = creator.createActorRef(Props(classOf[SessionActor], SessionRepositoryImpl), "SessionActor1")

  val createCandidate = Action.async { request =>
    val apiKey = request.headers.get("apiKey")
    val authResult = auth(sessionActor, apiKey)
    authResult.flatMap(auth => {
      if (auth.nonEmpty) {
        val json = request.body.asJson.get
        val message = json.toString()
        val candidate = extractEntity[CVCCandidate](message)
        val result = ask(candidateActor, candidate).mapTo[Future[Boolean]]
        val actualResult = result.flatMap(identity)
        actualResult.map { res =>
          if (res) {
            Ok(toJson(StatusMessage("Candidate Created")))
          }
          else {
            InternalServerError(toJson(StatusMessage("Candidate Not Created")))
          }
        }.recover({
          case ex =>
            candidateActor ! ex
            InternalServerError(toJson(StatusMessage(ex.getMessage)))
        })
      }
      else {
        Future(InternalServerError(toJson(StatusMessage(SESSION_EXPIRED))))
      }
    })
  }


  def createCandidateEmployment(id: String) = Action.async { request =>
    val apiKey = request.headers.get("apiKey")
    val authResult = auth(sessionActor, apiKey)
    authResult.flatMap(auth => {
      if (auth.nonEmpty) {
        val json = request.body.asJson.get
        val message = json.toString()
        val details = extractEntity[UpdateCandidateCommand](message)
        val result = ask(candidateActor, details).mapTo[Future[List[CVCCandidate]]]
        val actualResult = result.flatMap(identity)
        actualResult.map { res =>
          if (res.nonEmpty) {
            Ok(toJson(StatusMessage("Candidate Employment details Added")))
          }
          else {
            InternalServerError(toJson(StatusMessage("Candidate Employment details not Added")))
          }
        }.recover({
          case ex =>
            candidateActor ! ex
            InternalServerError(toJson(StatusMessage(ex.getMessage)))
        })
      }
      else {
        Future(InternalServerError(toJson(StatusMessage(SESSION_EXPIRED))))
      }
    })
  }

  val editCandidatePersonal = Action.async { request =>
    val apiKey = request.headers.get("apiKey")
    val authResult = auth(sessionActor, apiKey)
    authResult.flatMap(auth => {
      if (auth.nonEmpty) {
        val json = request.body.asJson.get
        val message = json.toString()
        val personalCommand = extractEntity[PersonalCommand](message)
        val result = ask(candidateActor, personalCommand).mapTo[Future[List[CVCCandidate]]]
        val actualResult = result.flatMap(identity)
        actualResult.map { res =>
          if (res.nonEmpty) {
            Ok(toJson(StatusMessage("Candidate Updated")))
          }
          else {
            InternalServerError(toJson(StatusMessage("Candidate Not Updated")))
          }
        }.recover({
          case ex =>
            candidateActor ! ex
            InternalServerError(toJson(StatusMessage(ex.getMessage)))
        })
      }
      else {
        Future(InternalServerError(toJson(StatusMessage(SESSION_EXPIRED))))
      }
    })
  }

  def editCandidateEmployment(id: String) = Action.async { request =>
    val apiKey = request.headers.get("apiKey")
    val authResult = auth(sessionActor, apiKey)
    authResult.flatMap(auth => {
      if (auth.nonEmpty) {
        val json = request.body.asJson.get
        val message = json.toString()
        val details = extractEntity[UpdateCandidateCommand](message)
        val result = ask(candidateActor, details).mapTo[Future[List[CVCCandidate]]]
        val actualResult = result.flatMap(identity)
        actualResult.map { res =>
          if (res.nonEmpty) {
            Ok(toJson(StatusMessage("Candidate Employment Updated")))
          }
          else {
            InternalServerError(toJson(StatusMessage("Candidate Employment Not Updated")))
          }
        }.recover({
          case ex =>
            candidateActor ! ex
            InternalServerError(toJson(StatusMessage(ex.getMessage)))
        })
      }
      else {
        Future(InternalServerError(toJson(StatusMessage(SESSION_EXPIRED))))
      }
    })
  }

  def editCandidatePreviousEmployment(id: String) = Action.async { request =>
    val apiKey = request.headers.get("apiKey")
    val authResult = auth(sessionActor, apiKey)
    authResult.flatMap(auth => {
      if (auth.nonEmpty) {
        val json = request.body.asJson.get
        val message = json.toString()
        val candidateEmployment = extractEntity[List[Employer]](message)
        val details = UpdateEmploymentCommand(id, candidateEmployment)
        val result = ask(candidateActor, details).mapTo[Future[List[CVCCandidate]]]
        val actualResult = result.flatMap(identity)
        actualResult.map { res =>
          if (res.nonEmpty) {
            Ok(toJson(StatusMessage("Candidate Employment Updated")))
          }
          else {
            InternalServerError(toJson(StatusMessage("Candidate Employment Not Updated")))
          }
        }.recover({
          case ex =>
            candidateActor ! ex
            InternalServerError(toJson(StatusMessage(ex.getMessage)))
        })
      }
      else {
        Future(InternalServerError(toJson(StatusMessage(SESSION_EXPIRED))))
      }
    })
  }

  def editCandidateEducation(id: String) = Action.async { request =>
    val apiKey = request.headers.get("apiKey")
    val authResult = auth(sessionActor, apiKey)
    authResult.flatMap(auth => {
      if (auth.nonEmpty) {
        val json = request.body.asJson.get
        val message = json.toString()
        val education = extractEntity[List[Education]](message)
        val details = UpdateEducationCommand(id, education)
        val result = ask(candidateActor, details).mapTo[Future[List[CVCCandidate]]]
        val actualResult = result.flatMap(identity)
        actualResult.map { res =>
          if (res.nonEmpty) {
            Ok(toJson(StatusMessage("Candidate Education Updated")))
          }
          else {
            InternalServerError(toJson(StatusMessage("Candidate Education Not Updated")))
          }
        }.recover({
          case ex =>
            candidateActor ! ex
            InternalServerError(toJson(StatusMessage(ex.getMessage)))
        })
      }
      else {
        Future(InternalServerError(toJson(StatusMessage(SESSION_EXPIRED))))
      }
    })
  }

  val editCandidateJobSearchEmployment = Action.async { request =>
    val apiKey = request.headers.get("apiKey")
    val authResult = auth(sessionActor, apiKey)
    authResult.flatMap(auth => {
      if (auth.nonEmpty) {
        val json = request.body.asJson.get
        val message = json.toString()
        val details = extractEntity[UpdateJobDetailCommand](message)
        val result = ask(candidateActor, details).mapTo[Future[List[CVCCandidate]]]
        val actualResult = result.flatMap(identity)
        actualResult.map { res =>
          if (res.nonEmpty) {
            Ok(toJson(StatusMessage("Candidate Job Details Updated")))
          }
          else {
            InternalServerError(toJson(StatusMessage("Candidate Job Details Not Updated")))
          }
        }.recover({
          case ex =>
            candidateActor ! ex
            InternalServerError(toJson(StatusMessage(ex.getMessage)))
        })
      }
      else {
        Future(InternalServerError(toJson(StatusMessage(SESSION_EXPIRED))))
      }
    })
  }

  def editCandidateSkills(id: String) = Action.async { request =>
    val apiKey = request.headers.get("apiKey")
    val authResult = auth(sessionActor, apiKey)
    authResult.flatMap(auth => {
      if (auth.nonEmpty) {
        val json = request.body.asJson.get
        val message = json.toString()
        val skills = extractEntity[List[Skill]](message)
        val details = UpdateSkillCommand(id, skills)
        val result = ask(candidateActor, details).mapTo[Future[List[CVCCandidate]]]
        val actualResult = result.flatMap(identity)
        actualResult.map { res =>
          if (res.nonEmpty) {
            Ok(toJson(StatusMessage("Candidate Skills Updated")))
          }
          else {
            InternalServerError(toJson(StatusMessage("Candidate Skills Not Updated")))
          }
        }.recover({
          case ex =>
            candidateActor ! ex
            InternalServerError(toJson(StatusMessage(ex.getMessage)))
        })
      }
      else {
        Future(InternalServerError(toJson(StatusMessage(SESSION_EXPIRED))))
      }
    })
  }

  def showProfile(id: String) = Action.async { request =>
    val apiKey = request.headers.get("apiKey")
    val authResult = auth(sessionActor, apiKey)
    authResult.flatMap(auth => {
      if (auth.nonEmpty) {
        val actualResult = ask(candidateActor, UserIdCommand(BSONObjectID.parse(id).get)).
          mapTo[Future[List[CVCCandidate]]].flatMap(identity)
        actualResult.map { res =>
          if (res.nonEmpty) {
            Ok(toJson(res.head))
          }
          else {
            NoContent
          }
        }.recover({
          case ex =>
            candidateActor ! ex
            InternalServerError(toJson(StatusMessage(ex.getMessage)))
        })
      }
      else {
        Future(InternalServerError(toJson(StatusMessage(SESSION_EXPIRED))))
      }
    })
  }

  def getUserByCandidateId(candidateId: String) = Action.async { request =>
    val apiKey = request.headers.get("apiKey")
    val authResult = auth(sessionActor, apiKey)
    authResult.flatMap(auth => {
      if (auth.nonEmpty) {
        val actualResult = ask(candidateActor, CandidateIdCommand(BSONObjectID.parse(candidateId).get)).
          mapTo[Future[List[CVUser]]].flatMap(identity)
        actualResult.map { res =>
          if (res.nonEmpty) {
            Ok(toJson(res.head))
          }
          else {
            NoContent
          }
        }.recover({
          case ex =>
            candidateActor ! ex
            InternalServerError(toJson(StatusMessage(ex.getMessage)))
        })
      }
      else {
        Future(InternalServerError(toJson(StatusMessage(SESSION_EXPIRED))))
      }
    })
  }

  val getCandidatesByIds = Action.async { request =>
    val apiKey = request.headers.get("apiKey")
    val authResult = auth(sessionActor, apiKey)
    authResult.flatMap(auth => {
      if (auth.nonEmpty) {
        val json = request.body.asText.get
        val details = extractEntity[List[BSONObjectID]](json)
        val req = FindByIdsCommand(details)
        val result = ask(candidateActor, req).mapTo[Future[List[CVCCandidate]]]
        val actualResult = result.flatMap(identity)
        actualResult.map { res =>
          Ok(toJson(res))
        }.recover({
          case ex =>
            candidateActor ! ex
            InternalServerError(toJson(StatusMessage(ex.getMessage)))
        })
      }
      else {
        Future(InternalServerError(toJson(StatusMessage(SESSION_EXPIRED))))
      }
    })
  }

  val getUsersByCandidateIds = Action.async { request =>
    val apiKey = request.headers.get("apiKey")
    val authResult = auth(sessionActor, apiKey)
    authResult.flatMap(auth => {
      if (auth.nonEmpty) {
        val json = request.body.asText.get
        val details = extractEntity[List[BSONObjectID]](json)
        val req = FindByUserIdsCommand(details)
        val result = ask(candidateActor, req).mapTo[Future[List[ShortCandidate]]]
        val actualResult = result.flatMap(identity)
        actualResult.map { res =>
          Ok(toJson(res))
        }.recover({
          case ex =>
            candidateActor ! ex
            InternalServerError(toJson(StatusMessage(ex.getMessage)))
        })
      }
      else {
        Future(InternalServerError(toJson(StatusMessage(SESSION_EXPIRED))))
      }
    })
  }

  def uploadFile(id: String, authResult: Future[List[Session]], fileConstant: FileConstant,
                 request: Request[MultipartFormData[TemporaryFile]], message: String): Future[Result] = {
    authResult.flatMap(auth => {
      if (auth.nonEmpty) {
        val file: Option[FilePart[TemporaryFile]] = request.body.file("file")
        val size = file.get.ref.file.length()
        if (size / 1024 > Constants.FILE_SIZE) {
          Future {
            InternalServerError(toJson(StatusMessage("File size is too big")))
          }
        }
        else {
          val command = UpdateFileCommand(id, file, fileConstant)
          val result = ask(candidateActor, command).mapTo[Future[List[CVCCandidate]]]
          val actualResult = result.flatMap(identity)
          actualResult.map { res =>
            if (res.nonEmpty) {
              Ok(toJson(StatusMessage(message + " added")))
            }
            else {
              InternalServerError(toJson(StatusMessage(message + " not added")))
            }
          }.recover({
            case ex =>
              candidateActor ! ex
              InternalServerError(toJson(StatusMessage(ex.getMessage)))
          })
        }
      }
      else {
        Future(InternalServerError(toJson(StatusMessage(SESSION_EXPIRED))))
      }
    })
  }

  def upload(id: String) = Action.async(parse.multipartFormData) { request =>
    val apiKey = request.headers.get("apiKey")
    val authResult = auth(sessionActor, apiKey)
    uploadFile(id, authResult, RESUMECONSTANT, request, "Candidate Resume")
  }

  def uploadPhoto(id: String) = Action.async(parse.multipartFormData) { request =>
    val apiKey = request.headers.get("apiKey")
    val authResult = auth(sessionActor, apiKey)
    uploadFile(id, authResult, PHOTOCONSTANT, request, "Candidate Photo")
  }

  def uploadMultiple(id: String) = Action.async(parse.multipartFormData) { request =>
    val apiKey = request.headers.get("apiKey")
    val authResult = auth(sessionActor, apiKey)
    uploadFile(id, authResult, DOCUMENTCONSTANT, request, "Candidate Document")
  }

  def getFileFromDb(id: String) = Action.async { request =>
    val apiKey = request.headers.get("apiKey")
    val authResult = auth(sessionActor, apiKey)
    authResult.flatMap(auth => {
      if (auth.nonEmpty) {
        val actualResult = ask(candidateActor, FileCommand(BSONObjectID.parse(id).get)).
          mapTo[Future[ReadFile[BSONSerializationPack.type, BSONValue]]].flatMap(identity)
        actualResult.map { res =>
          val someData = extractEntity[FormPart](toJson(res))
          val actualPart = ActualPart(Helper.convertToObjectId(someData.id.raw), someData.contentType, someData.filename, "", None)
          Ok(toJson(actualPart))
        }.recover({
          case ex =>
            candidateActor ! ex
            InternalServerError(toJson(StatusMessage(ex.getMessage)))
        })
      }
      else {
        Future(InternalServerError(toJson(StatusMessage(SESSION_EXPIRED))))
      }
    })
  }

  val download = Action.async { request =>
    val apiKey = request.headers.get("apiKey")
    val authResult = auth(sessionActor, apiKey)
    authResult.flatMap(auth => {
      if (auth.nonEmpty) {
        val json = request.body.asJson.get
        val message = json.toString()
        val wholeData = extractEntity[DownloadCommand](message)
        val result = ask(candidateActor, wholeData).mapTo[Future[Path]]
        val actualResult = result.flatMap(identity)
        actualResult.map { res =>
          Ok(toJson(res))
        }.recover({
          case ex =>
            candidateActor ! ex
            InternalServerError(toJson(StatusMessage(ex.getMessage)))
        })
      }
      else {
        Future(InternalServerError(toJson(StatusMessage(SESSION_EXPIRED))))
      }
    })
  }
}

