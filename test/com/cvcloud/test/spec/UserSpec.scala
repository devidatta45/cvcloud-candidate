package com.cvcloud.test.spec

import akka.actor.{ActorSystem, Props}
import akka.pattern.ask
import akka.testkit.{ImplicitSender, TestKit}
import akka.util.Timeout
import com.cvcloud.services._
import com.cvcloud.test.util.MockBaseRepository
import com.cvcloud.utils._
import org.scalatest._
import reactivemongo.bson.BSONObjectID

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Await, Future}

/**
  * Created by Donald Pollock on 31/08/2017.
  */

class UserSpec extends TestKit(ActorSystem("MySpec")) with ImplicitSender
  with WordSpecLike with Matchers with BeforeAndAfterAll {

  implicit val timeout: Timeout = Constants.TIMEOUT

  val bson1 = BSONObjectID.generate()
  val bson2 = BSONObjectID.generate()
  val bson3 = BSONObjectID.generate()
  val bson4 = BSONObjectID.generate()

  val userList = List(CVUser(bson1.stringify, "mr.", "raman", "raghav", "raman@gmail.com", "raman123", None, "6767767666", "bihar", "India", "765445"),
    CVUser(bson2.stringify, "mr.", "raman1", "raghav1", "raman11@gmail.com", "raman1234", None, "6767767456", "bihar", "India", "765445"),
    CVUser(bson3.stringify, "mr.", "raman2", "raghav2", "raman21@gmail.com", "raman1235", None, "67677676346", "tihar", "India", "765445"),
    CVUser(bson3.stringify, "mr.", "raman3", "raghav3", "raman22@gmail.com", "raman1236", None, "6767767678", "jhar", "India", "765445"))

  object MockCVUserRepositoryImpl extends CVUserRepository with MockBaseRepository[CVUser] {
    override val mockList: List[CVUser] = userList
  }

  object MockSessionRepositoryImpl extends SessionRepository with MockBaseRepository[Session] {
    override val mockList: List[Session] = Nil
  }

  val userActor = system.actorOf(Props(classOf[CVUserActor], MockCVUserRepositoryImpl))
  val sessionActor = system.actorOf(Props(classOf[SessionActor], MockSessionRepositoryImpl))

  override def afterAll {
    TestKit.shutdownActorSystem(system)
  }

  "An User Actor" must {
    "return all users" in {
      val future = ask(userActor, FindAllCommand).mapTo[Future[List[CVUser]]].flatMap(identity)
      val result = Await.result(future, Constants.TIMEOUT)
      result should be(userList)
    }
    "return Particular user" in {
      val future = ask(userActor, FindByIdCommand(bson1)).mapTo[Future[List[CVUser]]].flatMap(identity)
      val result = Await.result(future, Constants.TIMEOUT)
      val mockResult = CVUser(bson1.stringify, "mr.", "raman", "raghav", "raman@gmail.com", "raman123",
        None, "6767767666", "bihar", "India", "765445")
      result should be(List(mockResult))
    }
    "check authentication before logging in" in {
      val loginWithSession = LoginWithSession(Login("raman@gmail.com", "raman123"), sessionActor)
      val future = ask(userActor, loginWithSession).mapTo[Future[LoginResult]].flatMap(identity)
      val result = Await.result(future, Constants.TIMEOUT)
      val mockResult = CVUser(bson1.stringify, "mr.", "raman", "raghav", "raman@gmail.com", "raman123",
        None, "6767767666", "bihar", "India", "765445")
      result.user should be(mockResult)
    }
    "not able to log in if credentials are wrong" in {
      val loginWithSession = LoginWithSession(Login("xyxyxyxy", "xyxy123"), sessionActor)
      val future = ask(userActor, loginWithSession).mapTo[Future[LoginResult]].flatMap(identity)
      val result = Await.result(future, Constants.TIMEOUT)
      result.token should be(None)
    }
  }
}