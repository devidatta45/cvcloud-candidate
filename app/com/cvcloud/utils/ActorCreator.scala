package com.cvcloud.utils

import javax.inject.{Inject, Singleton}

import akka.actor.Actor.Receive
import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import akka.pattern.ask
import akka.routing.RoundRobinPool
import akka.util.Timeout
import akka.actor.OneForOneStrategy
import akka.actor.SupervisorStrategy._
import play.api.Logger

import scala.concurrent.duration._
import scala.concurrent.Await

/**
  * Created by DDM on 24-04-2017.
  */
@Singleton
class ActorCreator @Inject()(system: ActorSystem) {
  implicit val timeout: Timeout = Constants.TIMEOUT
  val ref = system.actorOf(Props[ActorSupervisor])

  def createRouter(props: Props, name: String): ActorRef = {
    val future = ask(ref, ActorName(props, name)).mapTo[ActorRef]
    Await.result(future, Constants.TIMEOUT)
  }

  def createActorRef(props: Props, name: String): ActorRef = {
    createRouter(RoundRobinPool(1).props(props), name)
  }
}

class ActorSupervisor extends Actor {
  override val supervisorStrategy =
    OneForOneStrategy(maxNrOfRetries = 10, withinTimeRange = 1.minute) {
      case cmd: Throwable => {
        println(s"Exception occured: ${cmd.printStackTrace()}")
        Resume
      }
    }

  override def receive: Receive = {
    case cmd: ActorName => sender ! context.actorOf(cmd.props, cmd.name)
  }
}

case class ActorName(props: Props, name: String)

trait BaseActor extends Actor {

  def normalExecution: Receive

  override def receive: Receive = normalExecution orElse handleError

  def handleError: Receive = {
    case cmd: Exception => {
      throw cmd
    }
  }
}

//class TestActor extends Actor {
//  override def receive: Receive = {
//    case cmd: String => println(cmd)
//    case cmd: Exception => throw cmd
//  }
//}
//
//object TestResult extends App {
//  val system = ActorSystem.create("test-sys")
//  val creator = new ActorCreator(system)
//  val ref = creator.createActorRef(Props(classOf[TestActor]), "TestActor")
//  ref ! "Hello"
//  ref ! new Exception("Its broken")
//  ref ! "Hello"
//}