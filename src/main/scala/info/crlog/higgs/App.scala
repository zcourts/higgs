package com.scriptandscroll.movies

import info.crlog.higgs.{Higgs, HiggsConstants}


object App {
  def main(args: Array[String]) = {
    val client = new Higgs(HiggsConstants.SOCKET_CLIENT)
    client.port=9090
    client.receive{
      case message=>println(message)
    }
    client.subscribe("a"){
      case message =>println(message)
    }

    client.connect()
    //val server = new Higgs(HiggsConstants.SOCKET_SERVER)

  }
}
