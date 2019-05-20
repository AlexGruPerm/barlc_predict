name := "barclpred"
version := "0.1"
scalaVersion := "2.11.8"
version := "1.0"

/**
  * todo: try use cassandra 4.0.0 driver with replace
  *
  * Driver 3:
  * import com.datastax.driver.core.ResultSet
  * import com.datastax.driver.core.Row
  * import com.datastax.driver.core.SimpleStatement
  *
  * Driver 4:
  * import com.datastax.oss.driver.api.core.cql.ResultSet;
  * import com.datastax.oss.driver.api.core.cql.Row;
  * import com.datastax.oss.driver.api.core.cql.SimpleStatement;
  *
  *
*/

val sparkVersion = "2.3.2"

libraryDependencies ++= Seq(
  "com.datastax.cassandra" % "cassandra-driver-core" % "3.7.1",
  "ch.qos.logback" % "logback-classic" % "1.2.3",
  "org.scala-lang" % "scala-library" % "2.11.8",
  "com.madhukaraphatak" %% "java-sizeof" % "0.1",
  "org.scalatest" %% "scalatest" % "3.0.5" % Test,
  "com.typesafe" % "config" % "1.3.4",
  "com.typesafe.akka" %% "akka-actor" % "2.5.22",
  "com.typesafe.akka" %% "akka-http" % "10.1.8",
  "com.typesafe.akka" %% "akka-stream" % "2.5.22",
  "org.json4s" %% "json4s-jackson" % "3.6.5",
  "org.apache.spark" %% "spark-hive" % sparkVersion % "provided",
  "org.apache.spark" %% "spark-core" % sparkVersion % "provided",
  "org.apache.spark" %% "spark-sql" % sparkVersion % "provided",
  "org.apache.spark" %% "spark-mllib" % sparkVersion % "provided",
  "com.datastax.spark" %% "spark-cassandra-connector" % "2.3.2"
)

assemblyMergeStrategy in assembly := {
  case PathList("reference.conf") => MergeStrategy.concat
  case PathList("META-INF", xs @ _*) => MergeStrategy.discard
  case "META-INF/ECLIPSEF.RSA" => MergeStrategy.last
  case "META-INF/mailcap" => MergeStrategy.last
  case "META-INF/mimetypes.default" => MergeStrategy.last
  case "plugin.properties" => MergeStrategy.last
  case "log4j.properties" => MergeStrategy.last
  case "logback.xml" => MergeStrategy.last
  case "resources/logback.xml" => MergeStrategy.last
  case "resources/application.conf" => MergeStrategy.last
  case "application.conf" => MergeStrategy.last
  case x => MergeStrategy.first
}

//sbt -mem 2048 run
//export _JAVA_OPTIONS="-Xms1024m -Xmx2G -Xss256m -XX:MaxPermSize=4G"
//to ~/.bash_profile and restart terminal

assemblyJarName in assembly :="barclpred.jar"
mainClass in (Compile, packageBin) := Some("pred.Main")
mainClass in (Compile, run) := Some("pred.Main")
