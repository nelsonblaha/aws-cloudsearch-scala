name := "cloudsearch4s"

organization := "jp.co.bizreach"

version := "0.0.1"

scalaVersion := "2.10.3"

libraryDependencies ++= Seq(
  "com.fasterxml.jackson.module" %% "jackson-module-scala"  % "2.3.3",
  "org.apache.httpcomponents"    %  "httpclient"            % "4.3.4",
  "lucene"                       %  "lucene"                % "1.4.3",
  "joda-time"                     % "joda-time"             % "2.2",
  "org.joda"                      % "joda-convert"          % "1.2"
)
