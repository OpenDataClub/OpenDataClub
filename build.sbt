name := "Open Data Club"

version := "0.0.1"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.6"

routesGenerator := InjectedRoutesGenerator

libraryDependencies ++= Seq(  
  "org.postgresql" % "postgresql" % "9.2-1004-jdbc41" withSources(),
  "com.typesafe.play" %% "play-slick" % "1.0.0" withSources(),
  "com.typesafe.play" %% "play-slick-evolutions" % "1.0.0" withSources(),
  "com.typesafe.slick" %% "slick" % "3.0.0" withSources(),
  "joda-time" % "joda-time" % "2.7" withSources(),
  "org.joda" % "joda-convert" % "1.7" withSources(),
  "com.github.tototoshi" %% "slick-joda-mapper" % "2.0.0" withSources(),
  "org.webjars" %% "webjars-play" % "2.4.0-1",
  "org.webjars" % "bootstrap" % "3.3.4",
  "org.webjars" % "angular-ui-bootstrap" % "0.13.0",
  "org.webjars" % "angularjs" % "1.4.0"
)

libraryDependencies += evolutions