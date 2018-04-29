package com.cvcloud.services

import java.io.File
import java.nio.file.Files
import java.util.Date

import akka.actor.Actor
import com.cvcloud.utils.BaseColumnConstants.{ID => _, ISREMOVED => _}
import com.cvcloud.utils.CVCCandidateColumnConstants._
import com.cvcloud.utils.JsonImplicits._
import com.cvcloud.utils.{DeleteByIdCommand, FindByIdCommand, _}
import reactivemongo.bson.{BSONDocument, BSONDocumentReader, BSONDocumentWriter, BSONObjectID}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/**
  * Created by DDM on 21-04-2017.
  */
case class CVCCandidate(override val _id: String, cvcUserId: String, photo: Option[String],
                        education: List[Education], skills: List[Skill], createdBy: Audit, modifiedBy: Audit, currentStatus: Option[String],
                        employmentDetails: Option[CVCCandidateEmploymentDetails]) extends BaseEntity

case class CVCCandidateEmploymentDetails(currentEmployer: Option[Employer], previousEmployers: List[Employer],
                                         jobLocations: List[String], jobTravelDistancePerm: Option[Double],
                                         jobTravelDistanceContract: Option[Double], noticePeriod: Option[Int], telephonicNotice: Option[Double],
                                         faceToFaceNotice: Option[Double], currentSalary: Option[Double], contractRate: Option[Double],
                                         requiredSalary: Option[Double], requiredContractRate: Option[Double], isNegotiable: Option[Boolean],
                                         previousClientRef: List[Reference], newJobReason: Option[String],
                                         linkedinUrl: Option[String], resume: Option[Resume], candidateDocuments: List[Documents],
                                         cvCloudDocuments: List[Documents], permanent: Option[Boolean], contract: Option[Boolean])

case class Employer(name: String, startDate: Option[Date], endDate: Option[Date],
                    jobTitle: String, salary: Double, location: String, skills: List[Skill])

case class Skill(name: String, score: Int, isPrimary: Boolean)

case class Education(degree: String, completionYear: String)

case class Reference(name: String, mobile: String, email: String)

case class Resume(version: String, objectId: String, modifiedDate: Option[Date], documentType: String, author: String)

case class Documents(name: String, objectId: String, description: String, modifiedDate: Option[Date], documentType: String, author: String)

case class Audit(userId: String, date: Option[Date] = Some(new Date()))

object CVCCandidate {

  implicit object SkillReader extends BSONDocumentReader[Skill] {
    def read(doc: BSONDocument): Skill = {
      val name = doc.getAs[String](NAME).get
      val score = doc.getAs[Int](SCORE).get
      val isPrimary = doc.getAs[Boolean](IS_PRIMARY).get
      Skill(name, score, isPrimary)
    }
  }

  implicit object EmployerReader extends BSONDocumentReader[Employer] {
    def read(doc: BSONDocument): Employer = {
      val name = doc.getAs[String](NAME).get
      val startDate = doc.getAs[Date](START_DATE)
      val endDate = doc.getAs[Date](END_DATE)
      val jobTitle = doc.getAs[String](JOB_TITLE).get
      val salary = doc.getAs[Double](SALARY).get
      val location = doc.getAs[String](LOCATION).get
      val skills = doc.getAs[List[Skill]](SKILLS).get
      Employer(name, startDate, endDate, jobTitle, salary, location, skills)
    }
  }

  implicit object EducationReader extends BSONDocumentReader[Education] {
    def read(doc: BSONDocument): Education = {
      val degree = doc.getAs[String](DEGREE).get
      val completionYear = doc.getAs[String](COMPLETION_YEAR).get
      Education(degree, completionYear)
    }
  }

  implicit object ReferenceReader extends BSONDocumentReader[Reference] {
    def read(doc: BSONDocument): Reference = {
      val name = doc.getAs[String](NAME).get
      val mobile = doc.getAs[String](MOBILE).get
      val email = doc.getAs[String](EMAIL).get
      Reference(name, mobile, email)
    }
  }

  implicit object ResumeReader extends BSONDocumentReader[Resume] {
    def read(doc: BSONDocument): Resume = {
      val version = doc.getAs[String](VERSION).get
      val objectId = doc.getAs[BSONObjectID](OBJECT_ID).get
      val modifiedDate = doc.getAs[Date](MODIFIED_DATE)
      val documentType = doc.getAs[String](DOCUMENT_TYPE).get
      val author = doc.getAs[BSONObjectID](AUTHOR).get
      Resume(version, objectId.stringify, modifiedDate, documentType, author.stringify)
    }
  }

  implicit object DocumentsReader extends BSONDocumentReader[Documents] {
    def read(doc: BSONDocument): Documents = {
      val name = doc.getAs[String](NAME).get
      val objectId = doc.getAs[BSONObjectID](OBJECT_ID).get
      val description = doc.getAs[String](DESCRIPTION).get
      val modifiedDate = doc.getAs[Date](MODIFIED_DATE)
      val documentType = doc.getAs[String](DOCUMENT_TYPE).get
      val author = doc.getAs[BSONObjectID](AUTHOR).get
      Documents(name, objectId.stringify, description, modifiedDate, documentType, author.stringify)
    }
  }

  implicit object AuditReader extends BSONDocumentReader[Audit] {
    def read(doc: BSONDocument): Audit = {
      val userId = doc.getAs[BSONObjectID](USER_ID).get
      val date = doc.getAs[Date](DATE)
      Audit(userId.stringify, date)
    }
  }

  implicit object CVCCandidateEmploymentReader extends BSONDocumentReader[CVCCandidateEmploymentDetails] {
    def read(doc: BSONDocument): CVCCandidateEmploymentDetails = {
      val currentEmployer = doc.getAs[Employer](CURRENT_EMPLOYER)
      val previousEmployers = doc.getAs[List[Employer]](PREVIOUS_EMPLOYERS).get
      val jobLocations = doc.getAs[List[String]](JOB_LOCATIONS).get
      val jobTravelDistancePerm = doc.getAs[Double](JOB_TRAVEL_DISTANCE_PERM)
      val jobTravelDistanceContract = doc.getAs[Double](JOB_TRAVEL_DISTANCE_CONTRACT)
      val noticePeriod = doc.getAs[Int](NOTICE_PERIOD)
      val telephonicNotice = doc.getAs[Double](TELEPHONIC_NOTICE)
      val faceToFaceNotice = doc.getAs[Double](FACE_TO_FACE_NOTICE)
      val currentSalary = doc.getAs[Double](CURRENT_SALARY)
      val contractRate = doc.getAs[Double](CONTRACT_RATE)
      val requiredSalary = doc.getAs[Double](REQUIRED_SALARY)
      val requiredContractRate = doc.getAs[Double](REQUIRED_CONTRACT_RATE)
      val isNegotiable = doc.getAs[Boolean](IS_NEGOTIABLE)
      val previousClientRef = doc.getAs[List[Reference]](PREVIOUS_CLIENT_REF).get
      val newJobReason = doc.getAs[String](NEW_JOB_REASON)
      val linkedinUrl = doc.getAs[String](LINKEDIN_URL)
      val resume = doc.getAs[Resume](RESUME)
      val candidateDocuments = doc.getAs[List[Documents]](CANDIDATE_DOCUMENTS).get
      val cvCloudDocuments = doc.getAs[List[Documents]](CVCLOUD_DOCUMENTS).get
      val permanent = doc.getAs[Boolean](PERMANENT)
      val contract = doc.getAs[Boolean](CONTRACT)
      CVCCandidateEmploymentDetails(currentEmployer, previousEmployers, jobLocations, jobTravelDistancePerm, jobTravelDistanceContract,
        noticePeriod, telephonicNotice, faceToFaceNotice, currentSalary, contractRate, requiredSalary, requiredContractRate, isNegotiable,
        previousClientRef, newJobReason, linkedinUrl, resume, candidateDocuments, cvCloudDocuments, permanent, contract)
    }
  }

  implicit object CVCCandidateReader extends BSONDocumentReader[CVCCandidate] {
    def read(doc: BSONDocument): CVCCandidate = {
      val id = doc.getAs[BSONObjectID](ID).get
      val cvcUserId = doc.getAs[BSONObjectID](CVC_USER_ID).get
      val photo = doc.getAs[BSONObjectID](PHOTO)
      val education = doc.getAs[List[Education]](EDUCATION).get
      val skills = doc.getAs[List[Skill]](SKILLS).get
      val createdBy = doc.getAs[Audit](CREATED_BY).get
      val modifiedBy = doc.getAs[Audit](MODIFIED_BY).get
      val currentStatus = doc.getAs[String](CURRENT_STATUS)
      val employmentDetails = doc.getAs[CVCCandidateEmploymentDetails](EMPLOYMENT_DETAILS)
      val validPhoto = if (photo.isDefined) Some(photo.get.stringify) else None
      CVCCandidate(id.stringify, cvcUserId.stringify, validPhoto, education, skills, createdBy, modifiedBy, currentStatus, employmentDetails)
    }
  }

  implicit object ResumeWriter extends BSONDocumentWriter[Resume] {
    def write(resume: Resume): BSONDocument = {
      val objectId = BSONObjectID.parse(resume.objectId).get
      val author = BSONObjectID.parse(resume.author).get
      BSONDocument(
        VERSION -> resume.version,
        OBJECT_ID -> objectId,
        MODIFIED_DATE -> resume.modifiedDate,
        DOCUMENT_TYPE -> resume.documentType,
        AUTHOR -> author
      )
    }
  }

  implicit object EmployerWriter extends BSONDocumentWriter[Employer] {
    def write(employer: Employer): BSONDocument = {
      BSONDocument(
        NAME -> employer.name,
        START_DATE -> employer.startDate,
        END_DATE -> employer.endDate,
        JOB_TITLE -> employer.jobTitle,
        SALARY -> employer.salary,
        LOCATION -> employer.location,
        SKILLS -> employer.skills.map(x => BSONDocument(NAME -> x.name, SCORE -> x.score, IS_PRIMARY -> x.isPrimary))
      )
    }
  }

  implicit object CVCCandidateEmploymentWriter extends BSONDocumentWriter[CVCCandidateEmploymentDetails] {
    def write(candidateEmployment: CVCCandidateEmploymentDetails): BSONDocument = {
      BSONDocument(
        CURRENT_EMPLOYER -> candidateEmployment.currentEmployer,
        PREVIOUS_EMPLOYERS -> candidateEmployment.previousEmployers,
        JOB_LOCATIONS -> candidateEmployment.jobLocations,
        JOB_TRAVEL_DISTANCE_PERM -> candidateEmployment.jobTravelDistancePerm,
        JOB_TRAVEL_DISTANCE_CONTRACT -> candidateEmployment.jobTravelDistanceContract,
        NOTICE_PERIOD -> candidateEmployment.noticePeriod,
        TELEPHONIC_NOTICE -> candidateEmployment.telephonicNotice,
        FACE_TO_FACE_NOTICE -> candidateEmployment.faceToFaceNotice,
        CURRENT_SALARY -> candidateEmployment.currentSalary,
        CONTRACT_RATE -> candidateEmployment.contractRate,
        REQUIRED_SALARY -> candidateEmployment.requiredSalary,
        REQUIRED_CONTRACT_RATE -> candidateEmployment.requiredContractRate,
        IS_NEGOTIABLE -> candidateEmployment.isNegotiable,
        PREVIOUS_CLIENT_REF -> candidateEmployment.previousClientRef.map(x => BSONDocument(NAME -> x.name, MOBILE -> x.mobile, EMAIL -> x.email)),
        NEW_JOB_REASON -> candidateEmployment.newJobReason,
        LINKEDIN_URL -> candidateEmployment.linkedinUrl,
        RESUME -> candidateEmployment.resume,
        CANDIDATE_DOCUMENTS -> candidateEmployment.candidateDocuments.map(x => BSONDocument(NAME -> x.name,
          OBJECT_ID -> BSONObjectID.parse(x.objectId).get, DESCRIPTION -> x.description, MODIFIED_DATE -> x.modifiedDate,
          DOCUMENT_TYPE -> x.documentType, AUTHOR -> BSONObjectID.parse(x.author).get)),
        CVCLOUD_DOCUMENTS -> candidateEmployment.cvCloudDocuments.map(x => BSONDocument(NAME -> x.name,
          OBJECT_ID -> BSONObjectID.parse(x.objectId).get, DESCRIPTION -> x.description, MODIFIED_DATE -> x.modifiedDate,
          DOCUMENT_TYPE -> x.documentType, AUTHOR -> BSONObjectID.parse(x.author).get)),
        PERMANENT -> candidateEmployment.permanent,
        CONTRACT -> candidateEmployment.contract)
    }
  }

  implicit object CVCCandidateWriter extends BSONDocumentWriter[CVCCandidate] {
    def write(candidate: CVCCandidate): BSONDocument = {
      val id = BSONObjectID.generate()
      val userId = BSONObjectID.parse(candidate.cvcUserId).get
      val photo = if (candidate.photo.isDefined && candidate.photo.get != "") Some(BSONObjectID.parse(candidate.photo.get).get) else None
      BSONDocument(ID -> id,
        CVC_USER_ID -> userId,
        PHOTO -> photo,
        EDUCATION -> candidate.education.map(edu => BSONDocument(DEGREE -> edu.degree,
          COMPLETION_YEAR -> edu.completionYear)),
        SKILLS -> candidate.skills.map(x => BSONDocument(NAME -> x.name, SCORE -> x.score, IS_PRIMARY -> x.isPrimary)),
        CREATED_BY -> BSONDocument(USER_ID -> BSONObjectID.parse(candidate.createdBy.userId).get, DATE -> candidate.createdBy.date),
        MODIFIED_BY -> BSONDocument(USER_ID -> BSONObjectID.parse(candidate.modifiedBy.userId).get, DATE -> candidate.modifiedBy.date),
        CURRENT_STATUS -> candidate.currentStatus,
        EMPLOYMENT_DETAILS -> candidate.employmentDetails,
        ISREMOVED -> candidate.isRemoved)
    }
  }

}

class CVCCandidateRepository extends BaseRepository[CVCCandidate] {
  override def table: String = Constants.CANDIDATE

  def updateCandidateEducation(id: String, cmd: List[Education]): Future[List[CVCCandidate]] = {

    val document = BSONDocument(
      "$set" -> BSONDocument(
        EDUCATION -> cmd.map(edu => BSONDocument(
          DEGREE -> edu.degree,
          COMPLETION_YEAR -> edu.completionYear
        ))
      ))
    updateById(BSONObjectID.parse(id).get, document)
  }

  def getCandidateByUserId(cmd: UserIdCommand): Future[List[CVCCandidate]] = {
    val document = BSONDocument(CVC_USER_ID -> cmd.id)
    filterQuery(document)
  }

  def getUserByCandidateId(cmd: CandidateIdCommand): Future[List[CVUser]] = {
    for {
      candidate <- findById(cmd.candidateId)
      user <- CVUserRepositoryImpl.findById(BSONObjectID.parse(candidate.head.cvcUserId).get)
    } yield user
  }

  def getUsersByCandidateIds(candidateIds: List[BSONObjectID]): Future[List[ShortCandidate]] = {
    val query = BSONDocument(ID -> BSONDocument("$in" -> candidateIds))
    for {
      candidates <- filterQuery(query)
      users <- CVUserRepositoryImpl.getUsersByIds(candidates.map(x => BSONObjectID.parse(x.cvcUserId).get))
      shortCandidates=shortCandidate(candidates,users)
    } yield shortCandidates
  }

  def getCandidatesByIds(cmd: List[BSONObjectID]): Future[List[CVCCandidate]] = {
    val query = BSONDocument(ID -> BSONDocument("$in" -> cmd))
    filterQuery(query)
  }

  def shortCandidate(candidates: List[CVCCandidate], users: List[CVUser]): List[ShortCandidate] = {
    candidates.map(candidate => {
      val user = users.find(user => user._id == candidate.cvcUserId)
      ShortCandidate(candidate._id,user.get.firstName,user.get.lastName)
    })
  }

  def updateCandidatePreviousEmployment(command: UpdateEmploymentCommand): Future[List[CVCCandidate]] = {
    import CVCCandidate._
    val document = BSONDocument(
      "$set" -> BSONDocument(
        EMPLOYMENT_DETAILS + "." + PREVIOUS_EMPLOYERS -> command.employer))
    updateById(BSONObjectID.parse(command.id).get, document)
  }

  def updateCandidateEmployment(command: UpdateCandidateCommand): Future[List[CVCCandidate]] = {
    import CVCCandidate._
    val document = BSONDocument(
      "$set" -> BSONDocument(
        EMPLOYMENT_DETAILS + "." + CURRENT_EMPLOYER -> Some(command.employer),
        CURRENT_STATUS -> Some(command.currentStatus)))
    updateById(BSONObjectID.parse(command.id).get, document)
  }

  def updateSkills(command: UpdateSkillCommand): Future[List[CVCCandidate]] = {
    val document = BSONDocument(
      "$set" -> BSONDocument(
        SKILLS -> command.skills.map(x => BSONDocument(NAME -> x.name, SCORE -> x.score, IS_PRIMARY -> x.isPrimary))))
    updateById(BSONObjectID.parse(command.id).get, document)
  }

  def updateAttribute(cmd: UpdateFileCommand, func: (CVCCandidate, ActualPart) => BSONDocument): Future[List[CVCCandidate]] = {
    import CVCCandidate._
    val filename = cmd.file.get.filename
    val staticFile = new File(s"$filename")
    val file = cmd.file.get.ref.moveTo(staticFile, true)
    val fileType = Files.probeContentType(file.toPath)
    val result = for {
      saveToDb <- saveFileToDb(file)
      someData = extractEntity[FormPart](toJson(saveToDb))
      actualPart = ActualPart(Helper.convertToObjectId(someData.id.raw), someData.contentType, someData.filename, fileType, Some(new Date()))
      candidate <- findById(BSONObjectID.parse(cmd.id).get)
      document = func(candidate.head, actualPart)
      deleteFile = staticFile.delete()
      updateInDb <- updateById(BSONObjectID.parse(cmd.id).get, document)
    } yield updateInDb
    result
  }

  def updatePersonalDetails(command: PersonalCommand) = {
    import CVCCandidate._
    import CVUserColumnConstants._
    val document = BSONDocument(
      "$set" -> BSONDocument(
        EMPLOYMENT_DETAILS -> command.employmentDetails
      ))
    val userDocument = BSONDocument(
      "$set" -> BSONDocument(
        TITLE -> command.title,
        FIRST_NAME -> command.firstName,
        LAST_NAME -> command.lastName,
        EMAIL -> command.email,
        PASSWORD -> command.password,
        MOBILE -> command.mobile,
        ADDRESS -> command.address,
        POST_CODE -> command.postCode
      ))
    for {
      updateUser <- CVUserRepositoryImpl.updateById(BSONObjectID.parse(command.userId).get, userDocument)
      filtered <- filterQuery(BSONDocument(CVC_USER_ID -> BSONObjectID.parse(command.userId).get))
      updateCandidate <- updateById(BSONObjectID.parse(filtered.head._id).get, document)
    } yield updateCandidate
  }

  def updateJobSearchDetails(command: UpdateJobDetailCommand) = {
    val document = BSONDocument(
      "$set" -> BSONDocument(
        EMPLOYMENT_DETAILS + "." + JOB_LOCATIONS -> command.jobLocations,
        EMPLOYMENT_DETAILS + "." + JOB_TRAVEL_DISTANCE_PERM -> Some(command.jobTravelDistancePerm),
        EMPLOYMENT_DETAILS + "." + JOB_TRAVEL_DISTANCE_CONTRACT -> Some(command.jobTravelDistanceContract),
        EMPLOYMENT_DETAILS + "." + CURRENT_SALARY -> Some(command.currentSalary),
        EMPLOYMENT_DETAILS + "." + CONTRACT_RATE -> Some(command.contractRate),
        EMPLOYMENT_DETAILS + "." + REQUIRED_SALARY -> Some(command.requiredSalary),
        EMPLOYMENT_DETAILS + "." + REQUIRED_CONTRACT_RATE -> Some(command.requiredContractRate),
        EMPLOYMENT_DETAILS + "." + NEW_JOB_REASON -> Some(command.newJobReason),
        EMPLOYMENT_DETAILS + "." + TELEPHONIC_NOTICE -> Some(command.telephonicNotice),
        EMPLOYMENT_DETAILS + "." + FACE_TO_FACE_NOTICE -> Some(command.faceToFaceNotice),
        EMPLOYMENT_DETAILS + "." + PERMANENT -> Some(command.permanent),
        EMPLOYMENT_DETAILS + "." + CONTRACT -> Some(command.contract),
        EMPLOYMENT_DETAILS + "." + IS_NEGOTIABLE -> command.isNegotiable))

    updateById(BSONObjectID.parse(command.id).get, document)
  }
}

//Implementation Object for Repository
object CVCCandidateRepositoryImpl extends CVCCandidateRepository

class CVCCandidateActor(repository:CVCCandidateRepository) extends BaseActor {

  import CVCCandidate._

  def resumeFunc(candidate: CVCCandidate, actualPart: ActualPart): BSONDocument = {
    val resume = if (candidate.employmentDetails.get.resume.isDefined) {
      val version = candidate.employmentDetails.get.resume.get.version.toDouble
      val newVersion = version + 0.1
      Resume(newVersion.toString, actualPart.id, actualPart.date, actualPart.documentType, candidate.cvcUserId)
    } else {
      val version = "1.0"
      Resume(version, actualPart.id, Some(new Date()), actualPart.documentType, candidate.cvcUserId)
    }
    val document = BSONDocument("$set" -> BSONDocument(EMPLOYMENT_DETAILS + "." + RESUME -> Some(resume)))
    document
  }

  def photoFunc(candidate: CVCCandidate, actualPart: ActualPart): BSONDocument = {
    val photo = Some(BSONObjectID.parse(actualPart.id).get)
    val document = BSONDocument("$set" -> BSONDocument(PHOTO -> photo))
    document
  }

  def documentFunc(candidate: CVCCandidate, actualPart: ActualPart): BSONDocument = {
    val documents = candidate.employmentDetails.get.candidateDocuments ::: List(Documents(actualPart.filename, actualPart.id,
      actualPart.filename + " Card", actualPart.date, actualPart.documentType, candidate.cvcUserId))
    val updatedDocument = BSONDocument(
      "$set" -> BSONDocument(
        EMPLOYMENT_DETAILS + "." + CANDIDATE_DOCUMENTS -> documents.map(document =>
          BSONDocument(NAME -> document.name, OBJECT_ID ->
            BSONObjectID.parse(document.objectId).get, DESCRIPTION -> document.description, MODIFIED_DATE -> document.modifiedDate,
            DOCUMENT_TYPE -> document.documentType, AUTHOR -> BSONObjectID.parse(document.author).get))))
    updatedDocument
  }

  override def normalExecution: Receive = {
    case FindAllCommand => sender ! repository.findAll()
    case candidate: CVCCandidate => sender ! repository.save(candidate)
    case cmd: FindByIdCommand => sender ! repository.findById(cmd.id)
    case cmd: FindByIdsCommand => sender ! repository.getCandidatesByIds(cmd.ids)
    case cmd: FindByUserIdsCommand => sender ! repository.getUsersByCandidateIds(cmd.ids)
    case cmd: DeleteByIdCommand => sender ! repository.deleteById(cmd.id)
    case cmd: UserIdCommand => sender ! repository.getCandidateByUserId(cmd)
    case cmd: CandidateIdCommand => sender ! repository.getUserByCandidateId(cmd)
    case cmd: UpdateCandidateCommand => sender ! repository.updateCandidateEmployment(cmd)
    case cmd: UpdateEmploymentCommand => sender ! repository.updateCandidatePreviousEmployment(cmd)
    case cmd: UpdateSkillCommand => sender ! repository.updateSkills(cmd)
    case cmd: UpdateFileCommand => {
      cmd.constant match {
        case RESUMECONSTANT => sender ! repository.updateAttribute(cmd, resumeFunc)
        case PHOTOCONSTANT => sender ! repository.updateAttribute(cmd, photoFunc)
        case DOCUMENTCONSTANT => sender ! repository.updateAttribute(cmd, documentFunc)
      }
    }
    case cmd: FileCommand => sender ! repository.getFileFromDb(cmd.id)
    case cmd: DownloadCommand => {
      sender ! repository.downloadFile(BSONObjectID.parse(cmd.actualPart.id).get, cmd.actualPart.filename, cmd.userId)
    }
    case cmd: PersonalCommand => sender ! repository.updatePersonalDetails(cmd)
    case cmd: UpdateJobDetailCommand => sender ! repository.updateJobSearchDetails(cmd)
    case cmd: UpdateEducationCommand => sender ! repository.updateCandidateEducation(cmd.id, cmd.educations)
  }
}

case class UpdateCandidateCommand(id: String, employer: Employer, currentStatus: String)

case class UpdateEmploymentCommand(id: String, employer: List[Employer])

case class UpdateEducationCommand(id: String, educations: List[Education])

case class UpdateJobDetailCommand(id: String, jobLocations: List[String], jobTravelDistancePerm: Double, jobTravelDistanceContract: Double,
                                  currentSalary: Double, contractRate: Double, requiredSalary: Double, requiredContractRate: Double,
                                  newJobReason: String, telephonicNotice: Double, faceToFaceNotice: Double,
                                  isNegotiable: Option[Boolean], permanent: Boolean, contract: Boolean)

case class UpdateSkillCommand(id: String, skills: List[Skill])

case class CandidateIdCommand(candidateId: BSONObjectID)

case class FindByUserIdsCommand(ids: List[BSONObjectID])

case class ShortCandidate(candidateId: String, firstName: String, lastName: String)

case class PersonalCommand(userId: String, title: String, firstName: String, lastName: String, password: String,
                           address: String, mobile: String, email: String, postCode: String, employmentDetails: CVCCandidateEmploymentDetails)