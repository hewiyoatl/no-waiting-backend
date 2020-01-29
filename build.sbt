
import com.typesafe.config.ConfigFactory
import scoverage.ScoverageKeys

name := "no-waiting-backend"

lazy val root = (project in file(".")).settings(
  bashScriptExtraDefines ++= Seq(
    "export LC_ALL=C.UTF-8",
    "export LANG=C.UTF-8"
  )).enablePlugins(PlayScala, PlayNettyServer).disablePlugins(PlayAkkaHttpServer)

disablePlugins(PlayLayoutPlugin)
PlayKeys.playMonitoredFiles ++= (sourceDirectories in(Compile, TwirlKeys.compileTemplates)).value

scalaVersion in ThisBuild := "2.11.8"
//scalaVersion in ThisBuild := "2.13.1"

resolvers += "Typesafe repository" at "https://repo.typesafe.com/typesafe/releases/"
resolvers += "Play2war plugins release" at "http://repository-play-war.forge.cloudbees.com/release/"
resolvers += "scalaz-bintray" at "https://dl.bintray.com/scalaz/releases/"
resolvers += "sonatype-releases" at "https://oss.sonatype.org/content/repositories/releases/"
resolvers += Resolver.bintrayRepo("cakesolutions", "maven")

//resolvers += "Rocketlawyer Snapshots" at "http://f1tst-linbld100/nexus/content/repositories/snapshots"
//resolvers += "Rocketlawyer Releases" at "http://f1tst-linbld100/nexus/content/repositories/releases"
//resolvers += "netty" at "https://mvnrepository.com/artifact/io.netty/netty-all"

libraryDependencies ++= Seq(
  ws,
  filters,
  guice,
  ehcache,
  "com.google.crypto.tink" % "tink" % "1.3.0-rc3", //https://github.com/google/tink
  "com.typesafe.play" %% "play-mailer" % "7.0.1",
  "com.typesafe.play" %% "play-mailer-guice" % "7.0.1",
  //metrics for database connections
  "nl.grons" %% "metrics-scala" % "3.5.4_a2.3",
  "com.kenshoo" %% "metrics-play" % "2.7.0_0.8.0",
  "io.prometheus" % "simpleclient" % "0.0.16",
  "io.prometheus" % "simpleclient_hotspot" % "0.0.16",
  "io.prometheus" % "simpleclient_servlet" % "0.0.16",
  "io.prometheus" % "simpleclient_pushgateway" % "0.0.16",
  "mysql" % "mysql-connector-java" % "5.1.34",
  "org.apache.commons" % "commons-lang3" % "3.0",
  specs2 % Test,
  "com.typesafe.play" %% "play-slick" % "4.0.2",
  //"org.hsqldb" % "hsqldb" % "2.4.0",
  "com.pauldijou" %% "jwt-play" % "0.19.0")
//  "com.pauldijou" %% "jwt-core" % "0.19.0",
//  "com.auth0" % "jwks-rsa" % "0.6.1")

//unmanagedResourceDirectories in Test <+= baseDirectory(_ / "target/web/public/test")

// disable .jar publishing
publishArtifact in (Compile, packageBin) := false

//sbtPlugin := true

publishMavenStyle := true

//to remove the reverse files generated from play for the coverage report
ScoverageKeys.coverageExcludedPackages := """controllers\..*Reverse.*;router.Routes.*;kafka\..*.*;"""

//to unable the parallel execution for testing purposes
parallelExecution in Test := false

// for the liquid support

// first read the local file from play application.conf
def getConfig: com.typesafe.config.Config = {
  val classLoader = new java.net.URLClassLoader( Array( new File("./src/main/resources/").toURI.toURL ) )
  ConfigFactory.load(classLoader)
}

// to generate the docker images
enablePlugins(JavaAppPackaging)

//microservice plugin for mini kubernetus
//enablePlugins(DirectoryMicroservice)

// for the standalone jar
assemblyMergeStrategy in assembly := {
  // Building fat jar without META-INF
  case PathList("META-INF", xs @ _*) => MergeStrategy.discard
  // Take last config file
  case PathList(ps @ _*) if ps.last endsWith ".conf" => MergeStrategy.last
  case PathList(ps @ _*) if ps.last endsWith "module-info.class" => MergeStrategy.concat
  case PathList("reference-overrides.conf") => MergeStrategy.concat
  case PathList("org", "slf4j", xs @ _*) => MergeStrategy.last
  case PathList("javax", "activation", xs @ _*) => MergeStrategy.first
  case PathList("com", "zaxxer", xs @ _*) => MergeStrategy.last
  case PathList("org", "apache", "log4j", xs @ _*) => MergeStrategy.last
  case PathList("org", "apache", "commons", "logging", xs @ _*) => MergeStrategy.discard
//  case PathList("org", "joda", "joda-convert", xs @ _*) => MergeStrategy.concat
//  case PathList("javax", "xml", "bind", "jaxb-api", xs @ _*) => MergeStrategy.concat
  case x if x.endsWith("io.netty.versions.properties") => MergeStrategy.first
  case x if x.endsWith("application.conf") => MergeStrategy.first
  case x if x.endsWith("spring.tooling") => MergeStrategy.first
  case x if x.endsWith("logback.xml") => MergeStrategy.first
  case x =>
    val oldStrategy = (assemblyMergeStrategy in assembly).value
    oldStrategy(x)
}

assemblyExcludedJars in assembly := {
  val cp = (fullClasspath in assembly).value
  cp filter {_.data.getName.contains("slf4j-log4j12")}
}

mainClass in assembly := Some("play.core.server.ProdServerStart")


fullClasspath in assembly += Attributed.blank(PlayKeys.playPackageAssets.value)

artifact in (Compile, assembly) := {
  val art = (artifact in (Compile, assembly)).value
  art.copy(`classifier` = Some("assembly"))
}

addArtifact(artifact in (Compile, assembly), assembly)
