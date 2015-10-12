package controllers

import javax.inject.Inject
import play.api.Play.current
import play.api.mvc._
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import scala.concurrent.{Future, Promise}
import scala.concurrent.duration._

class Application @Inject() (mediator: Mediator[Promise[Result]]) extends Controller {
  private val timeToIdle = current.configuration.getInt("mediator.connectionTimeout").getOrElse(30)


  def sub(token: String) = Action.async {
    val p = Promise[Result]()
    mediator.put(token, p, timeToIdle);

    val timeoutFuture = play.api.libs.concurrent.Promise.timeout("ttl", timeToIdle.seconds)

    Future.firstCompletedOf(Seq(p.future, timeoutFuture)).map {
      case r : Result => r
      case s : String => {
        mediator.invalidate(token, p)
        RequestTimeout("sub again\n")
      }
    }
  }

  def pub(token: String) = Action(parse.tolerantText) { request =>
    val value = request.body
    val subscribers = mediator.remove(token)
    subscribers.foreach{
      p=>
        println("p.isCompleted = "+p.isCompleted)
        p.success(Ok(value))
    }
    Ok(subscribers.size+"\n")
  }

}
