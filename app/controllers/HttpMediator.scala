package controllers

import org.slf4j.LoggerFactory
import play.api.libs.iteratee.{Enumeratee, Concurrent}
import play.api.mvc._

import play.api.libs.concurrent.Execution.Implicits.defaultContext

/**
 * Created by leonidv on 16.10.15.
 */
class HttpMediator extends Controller {
  import HttpMediator._

  val log = LoggerFactory.getLogger(classOf[HttpMediator])

  def subscribe(token : String) = Action { request =>
    log.debug("subscribe " + request.id)
    Ok.feed(out &> filter(token) &> extractBody())
  }

  def publish(token : String) = Action(parse.tolerantText){ request =>
    val body = request.body
    log.debug(s"publish ${request.id}, body = $body")
    in.push(Message(token, body))
    Ok(":)\n")
  }
}

case class Message(val token : String, val body : String)

object HttpMediator {
  val (out, in) = Concurrent.broadcast[Message]

  def extractBody() : Enumeratee[Message,String] = Enumeratee.map[Message] (msg => msg.body)

  def filter(token : String) : Enumeratee[Message,Message] = Enumeratee.filter(msg => msg.token.equals(token))
}
