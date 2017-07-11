name := "w-test"

version := "1.0"

scalaVersion := "2.11.11"

val akkaV = "2.4.19"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % akkaV,
  "com.typesafe.akka" %% "akka-testkit" % akkaV,
  "org.scalatest" %% "scalatest" % "3.0.1" % "test"
)
