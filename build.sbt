import ReleaseTransformations._
import com.typesafe.sbt.license.{DepModuleInfo, LicenseInfo}

// default release process, but without publishArtifacts
releaseProcess := Seq[ReleaseStep](
  checkSnapshotDependencies,
  inquireVersions,
  runTest,
  setReleaseVersion,
  commitReleaseVersion,
  tagRelease,
  // publishArtifacts,
  setNextVersion,
  commitNextVersion,
  pushChanges
)

def hasPrefix(org: String, prefixes: Seq[String]) = prefixes.exists(x => org.startsWith(x))

lazy val commonSettings = Seq(
  organization := "au.csiro.data61",
  // version := "0.1-SNAPSHOT", // see version.sbt maintained by sbt-release plugin
  licenses := Seq("BSD" -> url("https://github.com/data61/tika-extract/blob/master/LICENSE.txt")),
  homepage := Some(url("https://github.com/data61/tika-extract")),

  scalaVersion := "2.11.8",
  scalacOptions ++= Seq("-unchecked", "-deprecation", "-feature", "-optimise"),
  exportJars := true, // required by sbt-onejar
  autoAPIMappings := true, // scaladoc
  
  unmanagedSourceDirectories in Compile := (scalaSource in Compile).value :: Nil, // only Scala sources, no Java
  unmanagedSourceDirectories in Test := (scalaSource in Test).value :: Nil,
  
  // filterScalaLibrary := false, // sbt-dependency-graph: include scala library in output
  scalacOptions in (Compile,doc) := Seq("-diagrams"), // sbt-dependency-graph needs: sudo apt-get install graphviz
  
  EclipseKeys.withSource := true,
  // If Eclipse and sbt are both building to same dirs at same time it takes forever and produces corrupted builds.
  // So here we tell Eclipse to build somewhere else (bin is it's default build output folder)
  EclipseKeys.eclipseOutput in Compile := Some("bin"),   // default is sbt's target/scala-2.11/classes
  EclipseKeys.eclipseOutput in Test := Some("test-bin"), // default is sbt's target/scala-2.11/test-classes

  licenseOverrides := {
    case DepModuleInfo(org, _, _) if hasPrefix(org, Seq("org.apache", "com.fasterxml", "com.google.guava", "org.javassist", "io.swagger", "org.json4s")) => LicenseInfo(LicenseCategory.Apache, "The Apache Software License, Version 2.0", "http://www.apache.org/licenses/LICENSE-2.0.txt")
    case DepModuleInfo(org, _, _) if hasPrefix(org, Seq("com.thoughtworks.paranamer")) => LicenseInfo(LicenseCategory.BSD, "BSD-Style", "http://www.opensource.org/licenses/bsd-license.php")
    case DepModuleInfo(org, _, _) if hasPrefix(org, Seq("javax.ws.rs", "org.jvnet.mimepull", "org.glassfish")) => LicenseInfo(LicenseCategory.GPLClasspath, "CDDL + GPLv2 with classpath exception", "https://glassfish.dev.java.net/nonav/public/CDDL+GPL.html")
    case DepModuleInfo(org, _, _) if hasPrefix(org, Seq("ch.qos.logback")) => LicenseInfo(LicenseCategory.LGPL, "EPL + GNU Lesser General Public License", "http://logback.qos.ch/license.html")
    case DepModuleInfo(org, _, _) if hasPrefix(org, Seq("com.google.code.findbugs")) => LicenseInfo(LicenseCategory.LGPL, "GNU Lesser General Public License", "http://www.gnu.org/licenses/lgpl.html")
    case DepModuleInfo(org, _, _) if hasPrefix(org, Seq("org.slf4j")) => LicenseInfo(LicenseCategory.MIT, "MIT License", "http://www.slf4j.org/license.html")
    }
  )

// the sbt build honours transitive dependsOn, however this is not honoured by "com.typesafe.sbteclipse" % "sbteclipse-plugin" % "4.0.0"
// so we explicitly add the transitive dependencies

lazy val root = project.in(file(".")).
  settings(commonSettings: _*)
  .settings(com.github.retronym.SbtOneJar.oneJarSettings: _*)
  .settings(
	name := "tika-extract",
	libraryDependencies ++= Seq(
	  "http4s-dsl",
	  "http4s-blaze-server"
        ).map("org.http4s" %% _ % "0.15.0a"),
    libraryDependencies ++= Seq(
      "org.http4s" %% "http4s-argonaut" % "0.15.0a",
      "com.optimaize.languagedetector" % "language-detector" % "0.6",
      "edu.stanford.nlp" % "stanford-corenlp" % "3.6.0",
      "edu.stanford.nlp" % "stanford-corenlp" % "3.6.0" classifier "models" classifier "models-spanish",
      "com.google.protobuf" % "protobuf-java" % "3.1.0", // undeclared dependency of corenlp?
	  "com.typesafe.scala-logging" %% "scala-logging" % "3.5.0",
	  // "org.slf4j" % "slf4j-api" % "1.7.12",
	  "ch.qos.logback" % "logback-classic" % "1.1.3",
	  "org.scalatest" %% "scalatest" % "3.0.0" % "test"
	  // "org.scala-lang" % "scala-reflect" % "2.11.8", // Multiple dependencies with the same organization/name but different versions. To avoid conflict, pick one version
	  // "org.scala-lang.modules" %% "scala-xml" % "1.0.4" // as above
    ),
    mainClass in Compile := Some("au.csiro.data61.tikaExtract.ner.Main")
  )
