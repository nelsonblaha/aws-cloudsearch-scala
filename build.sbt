name := "cloudsearch4s"

organization := "jp.co.bizreach"

version := "0.0.1"

scalaVersion := "2.11.1"

crossScalaVersions := Seq("2.10.3", "2.11.1")

libraryDependencies ++= Seq(
  "com.fasterxml.jackson.module" %% "jackson-module-scala"  % "2.4.0-rc2",
  "org.apache.httpcomponents"    %  "httpclient"            % "4.3.4",
  "org.apache.lucene"            % "lucene-core"            % "4.8.1",
  "com.eaio.uuid"                % "uuid"                   % "3.2",
  "org.slf4j"                    % "slf4j-api"              % "1.7.7",
  "ch.qos.logback"               % "logback-classic"        % "1.1.2" % "test"
)

publishTo := Some(Resolver.ssh("amateras-repo-scp", "shell.sourceforge.jp", "/home/groups/a/am/amateras/htdocs/mvn/") withPermissions("0664")
  as(System.getProperty("user.name"), new java.io.File(Path.userHome.absolutePath + "/.ssh/id_rsa")))