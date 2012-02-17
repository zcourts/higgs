# Scala, Maven template

Nothing overly interesting. Simple a template to copy and paste when creating new maven based Scala projects.

I'll add a few additional things, RPM plugin and a base init.d script for e.g.

# Usage

Plain and simple...

* Open pom.xml
* change <groupId>info.crlog</groupId> to <groupId>com.website.something</groupId> where com.website.something is whatever you want your group ID to be
* change <artifactId>scala-maven-template</artifactId> to <artifactId>artifact</artifactId>
* change <name>Project name</name> to <name>NAME</name> where NAME is the name of your project

* Tweak and tune as you see fit...
* Add Scala source files/packages to src/main/scala
* Add Java source files/packages to src/main/java
* Add Scala test source files/packages to src/test/scala
* Add Java test source files/packages to src/test/java

* Add resources such as font,images etc to src/main/java/resources and/or src/main/scala/resources for Java and Scala respectively

Copy and paste (folders/pom) as you see fit, now setting up is less of a pain in the ass!
