package com.cvcloud.services

import java.util.Date

import com.cvcloud.utils.NewsFeedColumnConstants._
import com.cvcloud.utils.{BaseActor, BaseEntity, BaseRepository, Constants}
import reactivemongo.bson.{BSONDocument, BSONDocumentReader, BSONDocumentWriter, BSONObjectID}

/**
  * Created by Donald Pollock on 09/06/2017.
  */

case class NewsFeed(override val _id: String, jobId: String, candidateId: String, message: String,date:Option[Date]) extends BaseEntity

object NewsFeed {

  implicit object NewFeedReader extends BSONDocumentReader[NewsFeed] {
    def read(doc: BSONDocument): NewsFeed = {
      val id = doc.getAs[BSONObjectID](ID).get
      val jobId = doc.getAs[BSONObjectID](JOB_ID).get
      val candidateId = doc.getAs[BSONObjectID](CANDIDATE_ID).get
      val message = doc.getAs[String](MESSAGE).get
      val date = doc.getAs[Date](DATE)
      NewsFeed(id.stringify, jobId.stringify, candidateId.stringify, message,date)
    }
  }

  implicit object NewFeedWriter extends BSONDocumentWriter[NewsFeed] {
    def write(feed: NewsFeed): BSONDocument = {
      val id = BSONObjectID.generate()
      val jobId = BSONObjectID.parse(feed.jobId).get
      val candidateId = BSONObjectID.parse(feed.candidateId).get
      BSONDocument(ID -> id,
        JOB_ID -> jobId,
        CANDIDATE_ID -> candidateId,
        MESSAGE -> feed.message,
        DATE -> feed.date,
        ISREMOVED -> feed.isRemoved)
    }
  }

}

class NewsFeedRepository extends BaseRepository[NewsFeed] {
  override def table: String = Constants.NEWS_FEED
}

object NewsFeedRepositoryImpl extends NewsFeedRepository

class NewsFeedActor(repository: NewsFeedRepository) extends BaseActor {
  override def normalExecution: Receive = {
    case cmd: NewsFeed => sender ! repository.save(cmd)
    case candidateId: String => sender ! repository.filterQuery(BSONDocument(CANDIDATE_ID -> BSONObjectID.parse(candidateId).get))
  }
}