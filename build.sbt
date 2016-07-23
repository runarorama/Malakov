name := "Malakov"

version := "2.0"

scalaVersion := "2.10.2"

javaOptions += "-Xms128m" 

scalacOptions += "-deprecation"

scalacOptions += "-feature"

resolvers += "Sonatype snapshots" at "http://oss.sonatype.org/content/repositories/snapshots/" 

libraryDependencies ++= Seq("org.scalaz" %% "scalaz-core" % "7.0.4",
                            "org.scalaz" %% "scalaz-concurrent" % "7.0.4",
                            "org.scalaz.stream" %% "scalaz-stream" % "0.1")

