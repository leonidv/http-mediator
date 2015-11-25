package controllers

import org.slf4j.LoggerFactory
import play.api.libs.concurrent.Akka
import play.api.libs.iteratee._
import play.api.mvc._
import scala.concurrent.duration._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.Play.current

/**
  * Created by leonidv on 16.10.15.
  */
class HttpMediator extends Controller {

  import HttpMediator._

  val log = LoggerFactory.getLogger(classOf[HttpMediator])


  def subscribe(token: String) = Action { request =>

      Akka.system.scheduler.scheduleOnce(15 seconds) {
        in.push(new TimeoutMessage(token, request.id))
        // in.eofAndEnd()   next connection will be closed immediately
        // in.end()         next connection will be closed immediately
        // in.push(Input.EOF)   next connection will be never closed
      }

      Ok.feed(out &> filter(token) &> shouldStop(request.id) &> extractBody())
  }

  def publish(token: String) = Action(parse.tolerantText) { request =>
    val body = request.body
    log.debug(s"publish ${request.id}, token = $token, body = $body")
    in.push(new DataMessage(token, body))
    in.push(new EndMessage(token))
    Ok("\n")
  }
}

// It's possible to improve performance if use code : Int instead of hierarchy
sealed abstract class Message(val token: String, val body : String)

case class DataMessage(override val token: String, override val body: String) extends Message(token, body)

case class TimeoutMessage(override val token: String, val requestId: Long) extends Message(token, "")

case class EndMessage(override val token : String) extends Message(token, "")

object HttpMediator {
  val log = LoggerFactory.getLogger(classOf[HttpMediator])

  val (out, in) = Concurrent.broadcast[Message]

  def filter(token: String) : Enumeratee[Message, Message] = Enumeratee.filter[Message](_.token == token)

  def shouldStop(requestId: Long): Enumeratee[Message, Message] = Enumeratee.breakE[Message] { msg =>
    msg match {
      case TimeoutMessage(_, `requestId`) => true

      // micro optimization, token already checked in filter(token) function
      case EndMessage(_) => true
      case _ => false
    }
  }

  def extractBody(): Enumeratee[Message, String] = Enumeratee.map[Message](_.body)
}
