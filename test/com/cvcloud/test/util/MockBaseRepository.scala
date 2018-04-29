package com.cvcloud.test.util

import com.cvcloud.utils.{BaseEntity, BaseRepository}
import reactivemongo.bson.{BSONDocument, BSONDocumentReader, BSONDocumentWriter, BSONObjectID}

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

/**
  * Created by Donald Pollock on 05/09/2017.
  */
trait MockBaseRepository[T <: BaseEntity] extends BaseRepository[T] {

  val mockList: List[T]

  override def findAll()(implicit reader: BSONDocumentReader[T]): Future[List[T]] = {
    Future(mockList)
  }

  override def findById(id: BSONObjectID)(implicit reader: BSONDocumentReader[T]): Future[List[T]] = {
    val filteredMockList = mockList.filter(mock => mock._id == id.stringify)
    Future(filteredMockList)
  }

  override def save(t: T)(implicit writer: BSONDocumentWriter[T], reader: BSONDocumentReader[T]): Future[Boolean] = {
    Future(true)
  }

  override def filterQuery(document: BSONDocument)(implicit reader: BSONDocumentReader[T]): Future[List[T]] = {
    val keys = document.toMap.keys
    val result: Iterable[List[T]] = keys.map { key =>
      mockList.filter(m => {
        val field = m.getClass.getDeclaredField(key.toLowerCase)
        field.setAccessible(true)
        document.toMap.get(key).get.toString.contains(field.get(m).toString)
      })
    }
    Future(result.head)
  }
}
