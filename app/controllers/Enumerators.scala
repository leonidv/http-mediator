package controllers

import java.text.SimpleDateFormat
import java.util.Date

import org.slf4j.LoggerFactory
import play.api.libs.Comet
import play.api.libs.concurrent.{Akka, Promise}
import play.api.libs.iteratee._
import play.api.mvc._

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._
import play.api.libs.concurrent.Execution.Implicits._
//import play.api.libs.concurrent.Execution.Implicits.defaultContext

import play.api.Play.current

/**
 * Created by leonidv on 14.10.15.
 */
class Enumerators extends Controller {
  val log = LoggerFactory.getLogger("controllers.Enumarators")

  private def now() : String = {
    val dateFormat = new SimpleDateFormat("HH mm ss\n")
    dateFormat.format(new Date)
  }


  /**
   * 1. Create simple enumerator (producer), that publish data every 100 milliseconds.
   */
  lazy val clock : Enumerator[String] =
    Enumerator.generateM {
      Promise.timeout(Some(now()), 100 milliseconds)
    }


  /**
   * 2. In action just return a created Enumerator as publisher
   * @return
   */
  def liveClock = Action {
    Ok.chunked(clock)
  }


  /**
   * Broadcast example
   */
  /**
   * 1. Create new enumerator (producer) and channel (end point to enumerator (producer).
   */
  val (chatOut, chatChannel) = Concurrent.broadcast[String]

  /**
   * 2. Simmple timer, that push into channel time every 100 millisecond
   */
  Akka.system.scheduler.schedule(0 seconds, 100 millisecond) {
    chatChannel.push(now())
  }


  /**
   * 3. For testing purpose create a Enumeratee (transformation) for handling client disconnect
   * @param request
   * @return
   */
  private def disconnected(request : Request[_]): Enumeratee[String, String] =
    Enumeratee.onIterateeDone{()=>
      log.info("disconnected, request.id = "+request.id)
    }

  /**
   * 4. In action we compose Enumerator (chatOut) and it's transformations (Enumeratee).
   * @return
   */
  def numbers = Action {implicit request =>
    log.info("connected, request.id = " + request.id)
    Ok.feed(chatOut
      through disconnected(request)
      through RequestIdEnumeratee(request)
      )
  }

}
