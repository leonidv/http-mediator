package controllers

import org.slf4j.LoggerFactory
import play.api.libs.concurrent.Akka
import play.api.libs.iteratee.{Iteratee, Enumerator, Enumeratee, Concurrent}
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

  def subscribe(token : String) = Action { request =>
//    log.debug("subscribe " + request.id + ", token = " + token)
    Akka.system.scheduler.scheduleOnce(7 seconds) {
      in.push(new BreakMessage(token))
    }

    //Ok.feed(out &> break &> disconnected(request)  &> extractBody())

    Ok.feed(out &> disconnected(request) &> filter(token) &> checkTimout &> extractBody())
  }

  def publish(token : String) = Action(parse.tolerantText){ request =>
    val body = request.body
//    log.debug(s"publish ${request.id}, token = $token, body = $body")
    in.push(new TextMessage(token, body))
    Ok(":)\n")
  }
}

sealed abstract class Message(val token : String)
class TextMessage(token : String, val body : String) extends Message(token)
class BreakMessage(token: String) extends Message(token)

object HttpMediator {
  val log = LoggerFactory.getLogger(classOf[HttpMediator])

  val (out, in) = Concurrent.broadcast[Message]

  def extractBody() : Enumeratee[Message,String] = Enumeratee.map[Message]{
    case msg : TextMessage => msg.body
    case break : BreakMessage => ""
  }

  def filter(token : String) : Enumeratee[Message,Message] = Enumeratee.filter(msg => msg.token.equals(token))

  def disconnected(request : Request[_]): Enumeratee[Message, Message] =
    Enumeratee.onIterateeDone{()=>
      //log.info("disconnected, request.id = "+request.id)
    }

  def checkTimout : Enumeratee[Message,Message] = Enumeratee.breakE[Message](x => x.isInstanceOf[BreakMessage])
}
