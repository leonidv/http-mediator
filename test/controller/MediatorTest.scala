package controller

import controllers.Mediator
import org.scalamock.scalatest.MockFactory
import org.scalatest._
import play.api.inject.ApplicationLifecycle

/**
 * Created by leonidv on 11.10.15.
 */
class MediatorTest extends FlatSpec with Matchers with MockFactory {
  val lifecycle = stub[ApplicationLifecycle]

  "A Mediator" should "collect one item with on key and remove it on process" in {
    val mediator = new Mediator[Integer](lifecycle)
    mediator.put("1",1)
    var queue = mediator.remove("1")
    queue should have size 1
    queue should be equals(Set(1))

    queue = mediator.remove("1")
    queue shouldBe empty
  }

  it should "collect several items with one key" in {
    val mediator = new Mediator[Integer](lifecycle)

    mediator.put("1",1)
    mediator.put("1",2)

    var queue = mediator.remove("1")
    queue should have size 2
    queue should be equals(Set(1,2))

    queue = mediator.remove("1")
    queue shouldBe empty
  }

  it should "collect items with several keys" in {
    val mediator = new Mediator[Int](lifecycle)

    mediator.put("1",1)
    mediator.put("2",1)
    mediator.put("3",2)


    mediator.remove("1") should be equals (Set(1))
    mediator.remove("1") shouldBe empty

    mediator.remove("2") should be equals (Set(1))
    mediator.remove("2") shouldBe empty

    mediator.remove("3") should be equals (Set(2))
    mediator.remove("3") shouldBe empty
  }
}
