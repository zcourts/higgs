# Higgs Boson

The name shamelessly stolen from the almost mystical Higgs Boson http://en.wikipedia.org/wiki/Higgs_boson .

* __Higgs__ - the name of the library
* __Boson__ - the name of the protocol

Together with __Netty__ forms a pure JVM (NIO based) high performance, message oriented networking library.
The project was formed out of the frustration of the issues I ran into while using ZMQ in Java & Scala. 
Mainly issues with the native bindings and versions; Not with ZMQ's ideas... As such, the project will
(at some point) have support for subscribing to ZMQ publishers and less likely, publishing with the ZMQ format.


# Features

* Simplicity and Abstraction from the underlying NIO operations & socket handling.
* Extensible - Allowing user supplied protocols, encoders,decoders,client 7 server handlers
* Performant. As fast as you can get out of the JVM