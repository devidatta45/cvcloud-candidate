package com.cvcloud.utils

import java.io.File
import java.nio.file.{Files, Paths}

import scala.concurrent.duration._

/**
  * Created by DDM on 20-04-2017.
  */
object Constants {
  val USER_TABLE = "CVCUSER"
  val CANDIDATE = "CVCCANDIDATE"
  val CHAT_MESSAGE = "CVCMESSAGES"
  val CONTACT = "CVCCONTACT"
  val USER_KEY_MAPPING = "USERKEYMAPPING"
  val SESSION = "SESSION"
  val DEGREE = "DEGREE"
  val NEWS_FEED = "CVCNEWSFEED"
  val TIMEOUT = 5.seconds
  val MAIL_TIMEOUT = 10.seconds
  val FILE_TYPE = "multipart/form-data"
  val FILE_PATH = "public/files/"
  val FILE_SIZE = 2048
  val SESSION_EXPIRED = "User Session expired"
  val SENDER_EMAIL = "careercv774@gmail.com"
  val SENDER_PASSWORD = "career@123"
  val SENDER_NAME = "Career Cv-Cloud"
  val SMTP_HOST = "smtp.gmail.com"
  val SMTP_PORT = 587
}

trait BaseColumnConstants {
  val ID = "_id"
  val ISREMOVED = "isRemoved"
}

object BaseColumnConstants extends BaseColumnConstants

object CVUserColumnConstants extends BaseColumnConstants {
  val TITLE = "TITLE"
  val FIRST_NAME = "FIRSTNAME"
  val LAST_NAME = "LASTNAME"
  val EMAIL = "EMAIL"
  val PASSWORD = "PASSWORD"
  val CLIENT_REF_NUMBER = "CLIENTREFNUMBER"
  val MOBILE = "MOBILE"
  val ADDRESS = "ADDRESS"
  val COUNTRY = "COUNTRY"
  val POST_CODE = "POSTCODE"
}

object CVCCandidateColumnConstants extends BaseColumnConstants {
  val CVC_USER_ID = "CVCUSERID"
  val PHOTO = "PHOTO"
  val EDUCATION = "EDUCATION"
  val SKILLS = "SKILLS"
  val CREATED_BY = "CREATEDBY"
  val MODIFIED_BY = "MODIFIEDBY"
  val CURRENT_STATUS = "CURRENTSTATUS"
  val EMPLOYMENT_DETAILS = "EMPLOYMENTDETAILS"
  val NAME = "NAME"
  val START_DATE = "STARTDATE"
  val END_DATE = "ENDDATE"
  val JOB_TITLE = "JOBTITLE"
  val SALARY = "SALARY"
  val LOCATION = "LOCATION"
  val SCORE = "SCORE"
  val IS_PRIMARY = "ISPRIMARY"
  val DEGREE = "DEGREE"
  val COMPLETION_YEAR = "COMPLETIONYEAR"
  val EMAIL = "EMAIL"
  val MOBILE = "MOBILE"
  val VERSION = "VERSION"
  val DOCUMENT_TYPE = "DOCUMENTTYPE"
  val AUTHOR = "AUTHOR"
  val MODIFIED_DATE = "MODIFIEDDATE"
  val OBJECT_ID = "OBJECTID"
  val DESCRIPTION = "DESCRIPTION"
  val USER_ID = "USERID"
  val CURRENT_EMPLOYER = "CURRENTEMPLOYER"
  val PREVIOUS_EMPLOYERS = "PREVIOUSEMPLOYERS"
  val JOB_LOCATIONS = "JOBLOCATIONS"
  val JOB_TRAVEL_DISTANCE_PERM = "JOBTRAVELDISTANCEPERM"
  val JOB_TRAVEL_DISTANCE_CONTRACT = "JOBTRAVELDISTANCECONTRACT"
  val NOTICE_PERIOD = "NOTICEPERIOD"
  val TELEPHONIC_NOTICE = "TELEPHONICNOTICE"
  val FACE_TO_FACE_NOTICE = "FACETOFACENOTICE"
  val DATE = "DATE"
  val CURRENT_SALARY = "CURRENTSALARY"
  val CONTRACT_RATE = "CONTRACTRATE"
  val REQUIRED_SALARY = "REQUIREDSALARY"
  val REQUIRED_CONTRACT_RATE = "REQUIREDCONTRACTRATE"
  val IS_NEGOTIABLE = "ISNEGOTIABLE"
  val PREVIOUS_CLIENT_REF = "PREVIOUSCLIENTREF"
  val NEW_JOB_REASON = "NEWJOBREASON"
  val LINKEDIN_URL = "LINKEDINURL"
  val RESUME = "RESUME"
  val CANDIDATE_DOCUMENTS = "CANDIDATEDOCUMENTS"
  val CVCLOUD_DOCUMENTS = "CVCLOUDDOCUMENTS"
  val PERMANENT = "PERMANENT"
  val CONTRACT = "CONTRACT"
}

object DegreeColumnConstants extends BaseColumnConstants {
  val NAME = "NAME"
}

object ContactColumnConstants extends BaseColumnConstants {
  val USER_ID = "USERID"
  val NAME = "NAME"
  val EMAIL = "EMAIL"
  val SUBJECT = "SUBJECT"
  val PHONE = "PHONE"
  val MESSAGE = "MESSAGE"
}

object UserKeyColumnConstants extends BaseColumnConstants {
  val EMAIL = "EMAIL"
}

object NewsFeedColumnConstants extends BaseColumnConstants {
  val JOB_ID = "JOBID"
  val CANDIDATE_ID = "CANDIDATEID"
  val MESSAGE = "MESSAGE"
  val DATE = "DATE"
}

object MessageColumnConstants extends BaseColumnConstants {
  val CONSOLE_ID = "CONSOLEID"
  val FIRST_USER_ID = "FIRSTUSERID"
  val SECOND_USER_ID = "SECONDUSERID"
  val CHAT_CONVERSATION = "CHATCONVERSATION"
  val USER_ID = "USERID"
  val CHAT = "CHAT"
  val LAST_TIME = "LASTTIME"
  val LAST_TIME_TALK = "LASTTIMETALK"
}

object SessionColumnConstants extends BaseColumnConstants {
  val USER_ID = "USERID"
  val TOKEN = "TOKEN"
  val IS_RUNNING = "ISRUNNING"
}

//object ChatMessageConstants {
//  val ADMIN_USER_ID = "59105a593000004500df4049"
//}

object Helper {
  def convertToObjectId(list: List[Int]): String = {
    val decimals = list.map { dec =>
      if (dec < 0) {
        dec + 256
      } else dec
    }
    val hexList = decimals.map(decimal => {
      val str = Integer.toHexString(decimal)
      if (str.length == 1) {
        "0" + str
      } else str
    })
    val objectId = hexList.mkString("")
    objectId
  }

  def checkFilePresentInFolder(folder: String, fileName: String): Boolean = {
    val folderPath = new File(folder)
    val files = folderPath.listFiles().toList
    val result = files.filter(x => x.isFile && x.getName == fileName)
    if (result.nonEmpty) true else false
  }
}