scalaVersion := "2.11.0"


organization := "cc.nlplab"

name := "crawl_ltn"

version := "0.1-SNAPSHOT"

resolvers += Resolver.sonatypeRepo("public")

resolvers += "repo.codahale.com" at "http://repo.codahale.com"

libraryDependencies ++= Seq (
  "org.seleniumhq.webdriver" % "webdriver-selenium" % "0.9.7376",
  "org.seleniumhq.webdriver" % "webdriver-htmlunit" % "0.9.7376",
  "com.github.scopt" %% "scopt" % "3.2.0",
  "com.typesafe.scala-logging" %% "scala-logging-slf4j" % "2.1.0",
  "org.slf4j" % "slf4j-api" % "1.7.7",
  "org.slf4j" % "slf4j-simple" % "1.7.7",
  "org.slf4j" % "slf4j-log4j12" % "1.7.7",
   "log4j" % "log4j" % "1.2.17"
)

// "com.codahale" % "logula_2.9.1" % "2.1.3",

