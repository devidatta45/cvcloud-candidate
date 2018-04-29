package com.cvcloud.services

import com.cvcloud.utils._
import reactivemongo.bson.{BSONDocument, BSONDocumentReader, BSONDocumentWriter, BSONObjectID}

/**
  * Created by Donald Pollock on 23/05/2017.
  */

case class Degree(override val _id: String, name: String) extends BaseEntity

object Degree {

  import DegreeColumnConstants._

  implicit object SessionReader extends BSONDocumentReader[Degree] {
    def read(doc: BSONDocument): Degree = {
      val id = doc.getAs[BSONObjectID](ID).get
      val name = doc.getAs[String](NAME).get
      Degree(id.stringify, name)
    }
  }

  implicit object SessionWriter extends BSONDocumentWriter[Degree] {
    def write(degree: Degree): BSONDocument = {
      val id = BSONObjectID.generate()
      BSONDocument(ID -> id,
        NAME -> degree.name,
        ISREMOVED -> degree.isRemoved)
    }
  }

}

class DegreeRepository extends BaseRepository[Degree] {
  override def table: String = Constants.DEGREE
}

object DegreeRepositoryImpl extends DegreeRepository

class DegreeActor(repository: DegreeRepository) extends BaseActor {
  override def normalExecution: Receive = {
    case FindAllCommand => sender ! repository.findAll()
    case cmd:Degree => sender ! repository.save(cmd)
    case cmd:FindByIdCommand => sender ! repository.findById(cmd.id)
  }
}