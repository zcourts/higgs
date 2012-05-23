# Higgs Boson

The name shamelessly stolen from the almost mystical Higgs Boson http://en.wikipedia.org/wiki/Higgs_boson .

* __Higgs__ - the name of the library
* __Boson__ - the name of the protocol, the [protocol specification is here](https://github.com/zcourts/higgs/blob/master/src/main/scala/rubbish/crlog/higgs/protocol/boson/spec.md)

Together with __Netty__ forms a pure JVM (NIO based) high performance, message oriented networking library.
The project was formed out of the frustration of the issues I ran into while using ZMQ in Java & Scala. 
Mainly issues with the native bindings and versions; Not with ZMQ's ideas... As such, the project will
(at some point) have support for subscribing to ZMQ publishers and less likely, publishing with the ZMQ format.


# Features

* Simplicity and Abstraction from the underlying NIO operations & socket handling.
* Extensible - Allowing user supplied protocols, encoders,decoders,client 7 server handlers
* Performant. As fast as you can get out of the JVM

# Getting started

* From __Scala__ the class to interact with is __Higgs__ in package __info.crlog.higgs__
* From __Java__ the class to interact with is also called *Higgs* but in package __info.crlog.higgs.api.java__, this class is just a thin wrapper around the Scala version to make working with the library from Java a bit easier (i.e. more Java-like)

* A Higgs instance can be of 3 types, HiggsConstants.SOCKET_(CLIENT|SERVER|OTHER).
* From a message point of view, a SERVER allows you to "publish" messages, that CLIENTS can then subscribe to by topic (or receive everything)
* From a message point of view, a client allows you to subscribe to topics using __Higgs.subscribe__ method, to listen to all messages use __Higgs.receive__.
From a traditional client/server point of view, the subscription process above is simply you acting as a client and connecting to a server which then sends you data.
The point of Higgs however, is to abstract this and the underlying work that needs to be done.

# Advanced

Higgs is a fairly flexible library. One of the things it allows you to do is provide your own protocol to encode and decode messages.
Once you create a Higgs instance, you can supply classes to encode,decode data as well as a client and server handler which handles the logic of your protocol.

There are two requirements to be able to do this:

* Encoders,Decoders,ClientHandlers and ServerHandlers must inherit from the respective Higgs traits
* Each of the aformentioned must provide a default no arg constructor.