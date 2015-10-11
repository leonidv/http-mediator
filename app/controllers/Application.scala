package controllers

import javax.inject.Inject
import play.api.mvc._
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import scala.concurrent.Promise

class Application @Inject() (mediator: Mediator[Promise[Result]]) extends Controller {

  def sub(token: String) = Action.async {
    val p = Promise[Result]()
    mediator.put(token, p);
    p.future
  }

  def pub(token: String) = Action(parse.tolerantText) { request =>
    val value = request.body
    val subscribers = mediator.remove(token)
    subscribers.foreach(_.success(Ok(value)))
    Ok(subscribers.size+"\n")
  }

}
