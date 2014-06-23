name := "cloudsearch4s"

organization := "jp.co.bizreach"

version := "0.0.1"

scalaVersion := "2.11.1"

libraryDependencies ++= Seq(
  "com.fasterxml.jackson.module" %% "jackson-module-scala"  % "2.4.0-rc2",
  "org.apache.httpcomponents"    %  "httpclient"            % "4.3.4",
  "lucene"                       %  "lucene"                % "1.4.3",
  "com.eaio.uuid"                % "uuid"                   % "3.2"
)