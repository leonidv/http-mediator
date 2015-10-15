package controllers

import play.api.libs.iteratee.{Enumerator, Iteratee, Enumeratee}
import play.api.libs.concurrent.Execution.Implicits._
import play.api.mvc.Request

/**
 * Very simple enum
 *
 * Created by leonidv on 15.10.15.
 */
object RequestIdEnumeratee {

  /**
   * Create a Comet Enumeratee.
   *
   * @tparam E Type of messages handled by this comet stream.
   */
  def apply[E](request: Request[_]) = new Enumeratee[E, String] {

    def applyOn[A](inner: Iteratee[String, A]): Iteratee[E, Iteratee[String, A]] = {

      //val fedWithInitialChunk = Iteratee.flatten(Enumerator("") |>> inner)
      val eToScript = Enumeratee.map[E](data => s"${request.id} $data")
      eToScript.applyOn(inner)
    }
  }
}
