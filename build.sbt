name := "aws-cloudsearch-scala"

organization := "jp.co.bizreach"

version := "0.0.2"

scalaVersion := "2.11.7"

libraryDependencies ++= Seq(
  "com.fasterxml.jackson.module" %% "jackson-module-scala"  % "2.4.0-rc2",
  "org.apache.httpcomponents"    %  "httpclient"            % "4.3.4",
  "org.apache.lucene"            % "lucene-core"            % "4.8.1",
  "com.eaio.uuid"                % "uuid"                   % "3.2",
  "org.slf4j"                    % "slf4j-api"              % "1.7.7",
  "ch.qos.logback"               % "logback-classic"        % "1.1.2" % "test"
)

publishMavenStyle := true

publishTo <<= version { (v: String) =>
  val nexus = "https://oss.sonatype.org/"
  if (v.trim.endsWith("SNAPSHOT")) Some("snapshots" at nexus + "content/repositories/snapshots")
  else                             Some("releases"  at nexus + "service/local/staging/deploy/maven2")
}

scalacOptions := Seq("-deprecation")

publishArtifact in Test := false

pomIncludeRepository := { _ => false }

pomExtra := (
  <url>https://github.com/bizreach/aws-cloudsearch-scala</url>
  <licenses>
    <license>
      <name>The Apache Software License, Version 2.0</name>
      <url>http://www.apache.org/licenses/LICENSE-2.0.txt</url>
    </license>
  </licenses>
  <scm>
    <url>https://github.com/bizreach/aws-cloudsearch-scala</url>
    <connection>scm:git:https://github.com/bizreach/aws-cloudsearch-scala.git</connection>
  </scm>
  <developers>
    <developer>
      <id>takezoe</id>
      <name>Naoki Takezoe</name>
      <email>naoki.takezoe_at_bizreach.co.jp</email>
      <timezone>+9</timezone>
    </developer>
  </developers>)
