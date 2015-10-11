package controllers

import javax.inject.{Inject, Singleton}

import net.sf.ehcache.{Element, Ehcache, CacheManager}
import play.api.inject.ApplicationLifecycle
import play.api.mvc.Result

import scala.collection.Set
import scala.concurrent.{Future, Promise}

/**
 * Created by leonidv on 11.10.15.
 */
@Singleton
class Mediator[T] @Inject() (lifecycle : ApplicationLifecycle) {
  private implicit def elementAsSet(elem : Element) : Set[T] = {
    if (null == elem) {
      Set()
    } else {
      elem.getObjectValue().asInstanceOf[Set[T]]
    }
  }

  private val cacheManager = CacheManager.create()

  private val cache = cacheManager.getEhcache("messages")

  lifecycle.addStopHook{ () =>
    Future.successful(cacheManager.shutdown())
  }

  private def  lockedWrite[R](cache : Ehcache, key : String)(f:  => R): R = {
    cache.acquireWriteLockOnKey(key)
    try {
      return f
    } finally {
      cache.releaseWriteLockOnKey(key)
    }

  }



  def put(key : String, p : T): Unit = {
    lockedWrite(cache, key) {
      val currentQueue :Set[T] = cache.get(key);
      val newQueue = currentQueue + p;
      cache.put(new Element(key, newQueue))
    }
  }

  def remove(key : String) : Set[T] = {
    lockedWrite(cache, key) {
      val subscribers = cache.get(key)
      cache.remove(key)
      return subscribers
    }
  }
}
