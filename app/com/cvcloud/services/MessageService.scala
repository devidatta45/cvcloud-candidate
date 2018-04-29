package com.cvcloud.services

import java.util.Date

import com.cvcloud.utils.MessageColumnConstants._
import com.cvcloud.utils._
import reactivemongo.bson.{BSONDocument, BSONDocumentReader, BSONDocumentWriter, BSONObjectID}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/**
  * Created by DDM on 25-04-2017.
  */

case class ChatMessage(override val _id: String, consoleId: String, firstUserId: String, secondUserId: String,
                       chatConversation: List[Conversation], lastTimeTalk: Option[Date]) extends BaseEntity

case class ChatMessageView(override val _id: String, consoleId: String, firstUserId: Option[CVUser], secondUserId: String,
                           chatConversation: List[Conversation], lastTimeTalk: Option[Date]) extends BaseEntity

case class ChatMessageClientView(override val _id: String, consoleId: String, firstUserId: String, secondUserId: Option[CVUser],
                                 chatConversation: List[Conversation], lastTimeTalk: Option[Date]) extends BaseEntity

case class Conversation(userId: String, chat: String, lastTime: Option[Date])

object ChatMessage {

  import MessageColumnConstants._

  implicit object ConversationReader extends BSONDocumentReader[Conversation] {
    def read(doc: BSONDocument): Conversation = {
      val userId = doc.getAs[BSONObjectID](USER_ID).get
      val chat = doc.getAs[String](CHAT).get
      val lastTime = doc.getAs[Date](LAST_TIME)
      Conversation(userId.stringify, chat, lastTime)
    }
  }

  implicit object ChatMessageReader extends BSONDocumentReader[ChatMessage] {
    def read(doc: BSONDocument): ChatMessage = {
      val id = doc.getAs[BSONObjectID](ID).get
      val consoleId = doc.getAs[BSONObjectID](CONSOLE_ID).get
      val firstUserId = doc.getAs[BSONObjectID](FIRST_USER_ID).get
      val secondUserId = doc.getAs[BSONObjectID](SECOND_USER_ID).get
      val chatConversation = doc.getAs[List[Conversation]](CHAT_CONVERSATION).get
      val lastTimeTalk = doc.getAs[Date](LAST_TIME_TALK)
      ChatMessage(id.stringify, consoleId.stringify, firstUserId.stringify, secondUserId.stringify, chatConversation, lastTimeTalk)
    }
  }

  implicit object ChatMessageWriter extends BSONDocumentWriter[ChatMessage] {
    def write(chatMessage: ChatMessage): BSONDocument = {
      val id = BSONObjectID.generate()
      val firstUserId = BSONObjectID.parse(chatMessage.firstUserId).get
      val secondUserId = BSONObjectID.parse(chatMessage.secondUserId).get
      val consoleId = BSONObjectID.parse(chatMessage.consoleId).get
      BSONDocument(ID -> id,
        CONSOLE_ID -> consoleId,
        FIRST_USER_ID -> firstUserId,
        SECOND_USER_ID -> secondUserId,
        CHAT_CONVERSATION -> chatMessage.chatConversation.map(conversation => BSONDocument(USER_ID ->
          BSONObjectID.parse(conversation.userId).get, CHAT -> conversation.chat, LAST_TIME -> conversation.lastTime)),
        LAST_TIME_TALK -> chatMessage.lastTimeTalk,
        ISREMOVED -> chatMessage.isRemoved)
    }
  }

}

class ChatMessageRepository extends BaseRepository[ChatMessage] {
  override def table: String = Constants.CHAT_MESSAGE

  def updateChatHistory(candidateId: String, chatterId: String, conversation: Conversation): Future[List[ChatMessage]] = {
    val userId = BSONObjectID.parse(candidateId).get
    val secondUserId = BSONObjectID.parse(chatterId).get
    val query = BSONDocument(FIRST_USER_ID -> userId, SECOND_USER_ID -> secondUserId)
    val finalResult = for {
      result <- filterQuery(query)
      convo = result.head.chatConversation ::: List(conversation)
      document = BSONDocument(
        "$set" -> BSONDocument(
          CHAT_CONVERSATION -> convo.map(con => BSONDocument(USER_ID -> BSONObjectID.parse(con.userId).get, CHAT -> con.chat, LAST_TIME -> con.lastTime)),
          LAST_TIME_TALK -> Some(new Date())))
      updatedResult <- updateById(BSONObjectID.parse(result.head._id).get, document)
    } yield updatedResult
    finalResult
  }

  def saveOrUpdateMessage(message: ChatMessage): Future[Either[Future[Boolean], Future[List[ChatMessage]]]] = {
    val firstUserId = BSONObjectID.parse(message.firstUserId).get
    val secondUserId = BSONObjectID.parse(message.secondUserId).get
    val query = BSONDocument(FIRST_USER_ID -> firstUserId, SECOND_USER_ID -> secondUserId)
    for {
      result <- filterQuery(query)
      finalResult = if (result.isEmpty) {
        Left(save(message))
      } else {
        Right(updateChatHistory(message.firstUserId, message.secondUserId, message.chatConversation.head))
      }
    } yield finalResult
  }

  def showAllMessage(userId: String): Future[List[ChatMessageView]] = {
    val query = BSONDocument(SECOND_USER_ID -> BSONObjectID.parse(userId).get)
    for {
      filteredUsers <- filterQuery(query)
      userIds = filteredUsers.map(x => BSONObjectID.parse(x.firstUserId).get)
      allUsers <- CVUserRepositoryImpl.getUsersByIds(userIds)
      finalResult = filteredUsers.map(x => ChatMessageView(x._id,x.consoleId, getCandidate[CVUser](allUsers, x.firstUserId), x.secondUserId, x.chatConversation,x.lastTimeTalk))
    } yield finalResult
  }

  def showAllClientMessage(userId: String): Future[List[ChatMessageClientView]] = {
    val query = BSONDocument(FIRST_USER_ID -> BSONObjectID.parse(userId).get)
    for {
      filteredUsers <- filterQuery(query)
      userIds = filteredUsers.map(x => BSONObjectID.parse(x.secondUserId).get)
      allUsers <- CVUserRepositoryImpl.getUsersByIds(userIds)
      finalResult = filteredUsers.map(x => ChatMessageClientView(x._id,x.consoleId, x.firstUserId, getCandidate[CVUser](allUsers, x.secondUserId), x.chatConversation,x.lastTimeTalk))
    } yield finalResult
  }

  def getCandidate[T <: BaseEntity](list: List[T], id: String): Option[T] = {
    list.find(l => l._id == id)
  }
}

//Implementation Object for Repository
object ChatMessageRepositoryImpl extends ChatMessageRepository

class ChatMessageActor(repository:ChatMessageRepository) extends BaseActor {
  override def normalExecution: Receive = {
    case message: ChatMessage => sender ! repository.saveOrUpdateMessage(message)
    case id: String => sender ! repository.showAllMessage(id)
    case cmd: ClientCommand => sender ! repository.showAllClientMessage(cmd.userId)
  }
}

case class ClientCommand(userId: String)