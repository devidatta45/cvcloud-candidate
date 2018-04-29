package com.cvcloud.utils

import javax.inject.Inject

import play.api.cache.CacheApi

import scala.concurrent.duration.Duration

/**
  * Created by Donald Pollock on 27/07/2017.
  */
abstract class CacheService[T] @Inject()(cache: CacheApi) {
  val key: String

  def setCache(items: List[T]) = {
    cache.set(key, items)
  }

  def setCache(items: List[T], duration: Duration) = {
    cache.set(key, items, duration)
  }

  def addToCache(item: T) = {
    if (getCache.isDefined) {
      val list = getCache.get ::: List(item)
      setCache(list)
    }
    else {
      setCache(List(item))
    }
  }

  def removeFromCache(item: T) = {
    if (getCache.isDefined) {
      val list = getCache.get.filterNot(_ == item)
      setCache(list)
    }
    else {
      setCache(Nil)
    }
  }

  def getCache: Option[List[T]] = cache.get[List[T]](key)
}
