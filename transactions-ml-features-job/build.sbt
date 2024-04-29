ThisBuild / version := "0.1.0"

ThisBuild / scalaVersion := "2.12.18"

lazy val root = (project in file("."))
  .settings(
    name := "transactions-ml-features-job"
  )

val flinkVersion = "1.14.6"
val logbackVersion = "1.2.11"

val flinkDependencies = Seq(
  "org.apache.flink" %% "flink-clients" % flinkVersion,
  "org.apache.flink" %% "flink-scala" % flinkVersion,
  "org.apache.flink" %% "flink-streaming-scala" % flinkVersion
)

val flinkConnectors = Seq(
  "org.apache.flink" %% "flink-connector-kafka" % flinkVersion
)

val avroDependencies = Seq(
  "org.apache.flink" % "flink-avro" % flinkVersion,
  "io.confluent" % "kafka-avro-serializer" % "7.2.2"
)

val cassandraDependencies = Seq(
  "org.apache.flink" %% "flink-connector-cassandra" % flinkVersion
)

val logging = Seq(
  "ch.qos.logback" % "logback-core" % logbackVersion,
  "ch.qos.logback" % "logback-classic" % logbackVersion
)

libraryDependencies ++= flinkDependencies ++ flinkConnectors ++ avroDependencies ++ cassandraDependencies ++ logging

resolvers += "Confluent" at "https://packages.confluent.io/maven/"

updateOptions := updateOptions.value.withGigahorse(false)

assemblyMergeStrategy in assembly := {
  case PathList("org", "apache", xs @ _*) => MergeStrategy.first
  case PathList("META-INF", xs @ _*) => MergeStrategy.discard
  case _ => MergeStrategy.first
}

assemblyOption in assembly := (assemblyOption in assembly).value.copy(includeScala = false)
assemblyJarName in assembly := s"${name.value}-${version.value}.jar"

crossTarget := baseDirectory.value / "target/scala-2.12/"