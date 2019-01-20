/**
  * Copyright 2016-2018 Universitat Politècnica de València
  * Copyright 2016-2018 Università della Calabria
  * Copyright 2016-2018 Prodevelop, SL
  * Copyright 2016-2018 Technische Universiteit Eindhoven
  * Copyright 2016-2018 Fundación de la Comunidad Valenciana para la Investigación,
  *                     Promoción y Estudios Comerciales de Valenciaport
  * Copyright 2016-2018 Rinicom Ltd
  * Copyright 2016-2018 Association pour le développement de la formation
  *                     professionnelle dans le transport
  * Copyright 2016-2018 Noatum Ports Valenciana, S.A.U.
  * Copyright 2016-2018 XLAB razvoj programske opreme in svetovanje d.o.o.
  * Copyright 2016-2018 Systems Research Institute Polish Academy of Sciences
  * Copyright 2016-2018 Azienda Sanitaria Locale TO5
  * Copyright 2016-2018 Alessandro Bassi Consulting SARL
  * Copyright 2016-2018 Neways Technologies B.V.
  *
  * See the NOTICE file distributed with this work for additional information
  * regarding copyright ownership.
  *
  * Licensed under the Apache License, Version 2.0 (the "License");
  * you may not use this file except in compliance with the License.
  * You may obtain a copy of the License at
  *
  * http://www.apache.org/licenses/LICENSE-2.0
  *
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  * See the License for the specific language governing permissions and
  * limitations under the License.
  *
  */

lazy val akkaV = "2.5.18"
lazy val kafkaV = "0.22"
lazy val akkaHttpV = "10.1.5"
lazy val scalaTestV = "3.0.5"
lazy val scalaXmlV = "1.1.1"
lazy val slickV = "3.2.3"
lazy val jenaV = "3.9.0"

lazy val commonSettings = Seq(
  name := "ipsm-core",
  version := "0.8.10",
  scalaVersion := "2.12.8",
  scalacOptions := Seq(
    "-unchecked",
    "-deprecation",
    "-feature",
    "-encoding",
    "utf8"
  ),
  fork := true,
  javaOptions in reStart += "-Dfile.encoding=utf8",
  javaOptions += "-Dconfig.resource=ssl.conf",
  libraryDependencies ++= Seq(
    "org.rogach" %% "scallop" % "3.1.5",
    "commons-io" % "commons-io" % "2.6",
    "com.github.jsonld-java" % "jsonld-java" % "0.12.3",
    "ch.qos.logback" % "logback-classic" % "1.2.3",
    "com.typesafe" % "config" % "1.3.3",
    "com.typesafe.scala-logging" %% "scala-logging" % "3.9.0",
    "io.spray" %% "spray-json" % "1.3.5",
    "org.slf4j" % "slf4j-api" % "1.7.25",
    "com.typesafe.akka" %% "akka-slf4j" % akkaV,
    "com.typesafe.akka" %% "akka-actor" % akkaV,
    "com.typesafe.akka" %% "akka-stream" % akkaV,
    "com.typesafe.akka" %% "akka-stream-kafka" % kafkaV,
    "com.typesafe.akka" %% "akka-http" % akkaHttpV,
    "com.typesafe.akka" %% "akka-http-core" % akkaHttpV,
    "com.typesafe.akka" %% "akka-http-xml" % akkaHttpV,
    "com.typesafe.akka" %% "akka-http-spray-json" % akkaHttpV,
    "ch.megard" %% "akka-http-cors" % "0.3.1",
    "org.scalatest" %% "scalatest" % scalaTestV % "test",
    "com.typesafe.slick" %% "slick" % slickV,
    "com.typesafe.slick" %% "slick-hikaricp" % slickV,
    "org.xerial" % "sqlite-jdbc" % "3.25.2",
    "org.scala-lang.modules" %% "scala-xml" % scalaXmlV,
    "org.apache.kafka" % "kafka-clients" % "1.1.1",
    "org.apache.jena" % "jena-base" % jenaV,
    "org.apache.jena" % "jena-core" % jenaV,
    "org.apache.jena" % "jena-arq" % jenaV,
    "org.topbraid" % "shacl" % "1.1.0",
    "org.dom4j" % "dom4j" % "2.1.1",
    "jaxen" % "jaxen" % "1.1.6"

  ),
  dependencyOverrides ++= Seq(
    "com.typesafe.akka" %% "akka-actor" % akkaV,
    "com.typesafe.akka" %% "akka-stream" % akkaV
  ),
  assemblyDefaultJarName in assembly := s"${name.value}-assembly-${version.value}.jar"
)

lazy val assemblySettings = sbtassembly.AssemblyPlugin.assemblySettings ++ Seq(
  assemblyMergeStrategy in assembly := {
    case PathList("logback-test.xml") => MergeStrategy.discard
    case PathList("org", "apache", "commons", "logging", _*) => MergeStrategy.first
    case PathList("org", "apache", "commons", "codec", _*) => MergeStrategy.first
    case PathList("javax", "xml", _*) => MergeStrategy.first
    case x =>
      val oldStrategy = (assemblyMergeStrategy in assembly).value
      oldStrategy(x)
  },
  mainClass in assembly := Some("eu.interiot.ipsm.core.Main"),
  test in assembly := {}
)

lazy val dockerSettings = Seq(
  dockerfile in docker := {
    val artifact: File = assembly.value
    val artifactTargetPath = s"/ipsm/${artifact.name}"
    new Dockerfile {
      from("openjdk:8-alpine")
      maintainer("Wiesiek Pawłowski <wieslaw.pawlowski@ibspan.waw.pl>")
      add(artifact, artifactTargetPath)
      entryPoint("java", "-Dconfig.resource=ssl.conf", "-jar", artifactTargetPath)
    }
  },
  imageNames in docker := Seq(
    ImageName(s"interiot/ipsm-core:${version.value}")
  ),
  buildOptions in docker := BuildOptions(cache = false)
)

lazy val buildInfoSettings = Seq(
  buildInfoKeys in compile := Seq[BuildInfoKey](name, version, scalaVersion, sbtVersion),
  buildInfoPackage in compile := "eu.interiot.ipsm.core",
  buildInfoObject in compile := "BuildInfo"
)

lazy val aggregatedSettings = commonSettings ++ assemblySettings ++ dockerSettings ++ buildInfoSettings

lazy val ipsm_core = project
  .in(file("."))
  .enablePlugins(DockerPlugin, BuildInfoPlugin)
  .settings(aggregatedSettings: _*)

import com.typesafe.sbt.license.{LicenseInfo, DepModuleInfo}

licenseOverrides := {
  case DepModuleInfo("org.apache.commons", "commons-lang3", _) =>
    LicenseInfo(LicenseCategory.Apache, "The Apache Software License, Version 2.0", "http://opensource.org/licenses/Apache-2.0")
  case DepModuleInfo("ch.qos.logback", _, _) =>
    LicenseInfo(LicenseCategory.EPL, "Eclipse Public License 1.0", "http://opensource.org/licenses/EPL-1.0")
  case DepModuleInfo("com.typesafe.slick", _, _) =>
    LicenseInfo(LicenseCategory.BSD, "BSD 2-clause", "http://opensource.org/licenses/BSD-2-Clause")

}

licenseSelection := Seq(LicenseCategory.Apache, LicenseCategory.BSD, LicenseCategory.MIT, LicenseCategory.EPL)