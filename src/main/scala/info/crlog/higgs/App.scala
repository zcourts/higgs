package com.scriptandscroll.movies

import info.crlog.higgs.{Higgs, HiggsConstants}
import info.crlog.higgs.protocol.boson.BosonMessage


object App {
  def main(args: Array[String]) = {
    val client = new Higgs(HiggsConstants.SOCKET_CLIENT)
    client.port = 9090
    //get all messages regardless of topic
    client.receive {
       message: BosonMessage => println(message)
    }
    //sub scribe to the topic 'a'
    client.subscribe("a") {
       message: BosonMessage => println(message)
    }
    client connect
    //val server = new Higgs(HiggsConstants.SOCKET_SERVER)

  }
}
