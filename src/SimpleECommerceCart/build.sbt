name := "SimpleECommerceCart"
version := "1.0"
scalaVersion := "2.11.10"

libraryDependencies ++= Seq(
  "com.typesafe.slick" %% "slick" % "3.2.3",
  "mysql" % "mysql-connector-java" % "5.1.47",
  "org.slf4j" % "slf4j-nop" % "1.7.30"
)