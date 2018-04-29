package com.cvcloud.services

import akka.actor.Actor
import akka.actor.Actor.Receive
import com.cvcloud.utils._
import reactivemongo.bson.{BSONDocument, BSONDocumentReader, BSONDocumentWriter, BSONObjectID}


/**
  * Created by Donald Pollock on 25/05/2017.
  */

case class Contact(override val _id: String, userId: String, name: String, email: String,
                   subject: String, phone: String, message: String) extends BaseEntity

object Contact {

  import ContactColumnConstants._

  implicit object ContactReader extends BSONDocumentReader[Contact] {
    def read(doc: BSONDocument): Contact = {
      val id = doc.getAs[BSONObjectID](ID).get
      val userId = doc.getAs[BSONObjectID](USER_ID).get
      val name = doc.getAs[String](NAME).get
      val email = doc.getAs[String](EMAIL).get
      val subject = doc.getAs[String](SUBJECT).get
      val phone = doc.getAs[String](PHONE).get
      val message = doc.getAs[String](MESSAGE).get
      Contact(id.stringify, userId.stringify, name, email, subject, phone, message)
    }
  }

  implicit object ContactWriter extends BSONDocumentWriter[Contact] {
    def write(contact: Contact): BSONDocument = {
      val id = BSONObjectID.generate()
      val userId = BSONObjectID.parse(contact.userId).get
      BSONDocument(ID -> id,
        USER_ID -> userId,
        NAME -> contact.name,
        EMAIL -> contact.email,
        SUBJECT -> contact.subject,
        PHONE -> contact.phone,
        MESSAGE -> contact.message,
        ISREMOVED -> contact.isRemoved)
    }
  }

}

class ContactRepository extends BaseRepository[Contact] {
  override def table: String = Constants.CONTACT
}

object ContactRepositoryImpl extends ContactRepository

class ContactActor(repository:ContactRepository) extends BaseActor {
  override def normalExecution: Receive = {
    case cmd: Contact => sender ! repository.save(cmd)
    case FindAllCommand => sender ! repository.findAll()
    case cmd: FindByIdCommand => sender ! repository.findById(cmd.id)
    case cmd: DeleteByIdCommand => sender ! repository.deleteById(cmd.id)
  }
}