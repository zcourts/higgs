package com.scriptandscroll.movies

import info.crlog.higgs.{Higgs, HiggsConstants}
import info.crlog.higgs.protocol.boson.BosonMessage
import info.crlog.higgs.protocol.Message


object App {
  def main(args: Array[String]) = {
    val client = new Higgs(HiggsConstants.SOCKET_CLIENT)
    client.port = 9090
    //get all messages regardless of topic
    client.receive {
      message => println(message)
    }
    //sub scribe to the topic 'a'
    client.subscribe("a") {
      case message: BosonMessage => println(message)
      case message: Message => println(message)
    }
    client connect
    //val server = new Higgs(HiggsConstants.SOCKET_SERVER)

  }
}
