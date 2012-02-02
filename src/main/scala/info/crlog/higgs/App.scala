package com.scriptandscroll.movies

import info.crlog.higgs.protocol.boson.BosonMessage
import info.crlog.higgs.protocol.Message
import info.crlog.higgs.{Higgs, HiggsConstants}


object App {
  def main(args: Array[String]) = {
    //    println(HiggsConstants.SOCKET_CLIENT)
    //    println(HiggsConstants.SOCKET_SERVER)
    val client = new Higgs(HiggsConstants.SOCKET_CLIENT)
    //val server = new Higgs(HiggsConstants.SOCKET_SERVER)
    subscribe("stop")({
      case message => println(message)
    })
    receive {
      case msg => println(msg)
    }
  }

  def subscribe(topic: String)(fn: Function1[Message, Unit]) = {
    if (topic.isEmpty)
      fn(new BosonMessage("No topic..."))
    else
      fn(new BosonMessage("topic:" + topic))
  }

  def receive(fn: Function1[Message, Unit]) = {
    subscribe("")(fn)
  }
}
