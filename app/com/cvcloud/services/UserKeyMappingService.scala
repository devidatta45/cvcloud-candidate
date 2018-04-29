package com.cvcloud.services

import com.cvcloud.utils.UserKeyColumnConstants._
import com.cvcloud.utils.{BaseActor, BaseEntity, BaseRepository, Constants}
import reactivemongo.bson.{BSONDocument, BSONDocumentReader, BSONDocumentWriter, BSONObjectID}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/**
  * Created by Donald Pollock on 29/06/2017.
  */

case class UserKeyMapping(override val _id: String, email: String) extends BaseEntity

object UserKeyMapping {

  implicit object UserKeyMappingReader extends BSONDocumentReader[UserKeyMapping] {
    def read(doc: BSONDocument): UserKeyMapping = {
      val id = doc.getAs[BSONObjectID](ID).get
      val email = doc.getAs[String](EMAIL).get
      UserKeyMapping(id.stringify, email)
    }
  }

  implicit object UserKeyMappingWriter extends BSONDocumentWriter[UserKeyMapping] {
    def write(userKeyMapping: UserKeyMapping): BSONDocument = {
      val id = BSONObjectID.generate()
      BSONDocument(ID -> id,
        EMAIL -> userKeyMapping.email,
        ISREMOVED -> userKeyMapping.isRemoved)
    }
  }

}

class UserKeyMappingRepository extends BaseRepository[UserKeyMapping] {
  override def table: String = Constants.USER_KEY_MAPPING

  def getEmailByKey(key: String): Future[String] = {
    for {
      result <- findById(BSONObjectID.parse(key).get)
      email = result.head.email
    } yield email
  }
}

object UserKeyMappingRepositoryImpl extends UserKeyMappingRepository

class UserKeyMappingActor(repository: UserKeyMappingRepository) extends BaseActor {
  override def normalExecution: Receive = {
    case key: String => sender ! repository.getEmailByKey(key)
  }
}

//object TestApp extends App {
//  type Set = Int => Boolean
//  val bound = 1000
//
//  def contains(set: Set, element: Int): Boolean = set(element)
//
//  def filter(set: Set, predicate: Int => Boolean): Set = { elem: Int => contains(set, elem) && contains(predicate, elem) }
//
//  def forall(set: Set, predicate: Int => Boolean): Boolean = {
//    def iter(a: Int): Boolean = {
//      if (a == -bound) true
//      else if (contains(set, a) && (!contains(filter(set, predicate), a))) false
//      else iter(a - 1)
//    }
//    iter(bound)
//  }
//
//  def ss(dd:Int)={
//    if(dd %2 == 0) true else false
//  }
//
//  val set:Set = ss
//
//  val result = forall(set,ss)
//  println(result)
//}