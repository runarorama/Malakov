name := "Malakov"

version := "3.0"

scalaVersion := "2.11.7"

javaOptions += "-Xms128m" 

scalacOptions += "-deprecation"

scalacOptions += "-feature"

resolvers += "Sonatype snapshots" at "http://oss.sonatype.org/content/repositories/snapshots/" 

libraryDependencies ++= Seq("org.scalaz" %% "scalaz-core" % "7.2.4",
                            "org.scalaz" %% "scalaz-concurrent" % "7.2.4",
                            "org.scalaz.stream" %% "scalaz-stream" % "0.8")

