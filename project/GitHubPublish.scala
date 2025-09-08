/*
 * Copyright (C) 2009-2025 Lightbend Inc. <https://www.lightbend.com>
 */

package akka

import sbt._
import sbt.Keys._

object GitHubPublish extends AutoPlugin {

  override def trigger = allRequirements

  override lazy val projectSettings = Seq(
    publishTo := {
      val hasGitHubToken = sys.env.get("GITHUB_MAVEN_TOKEN").orElse(sys.env.get("GITHUB_MAVEN_PASSWORD")).isDefined
      (sys.env.get("GITHUB_MAVEN_URL"), sys.env.get("GITHUB_MAVEN_USERNAME"), hasGitHubToken) match {
        case (Some(url), Some(_), true) =>
          Some("GitHubPackages" at url)
        case _ =>
          // Disable publishing when GitHub Maven variables are not set
          None
      }
    },
    credentials ++= githubCredentials,
    // Disable the default Publish plugin when GitHub Maven is configured
    publish := {
      val hasGitHubToken = sys.env.get("GITHUB_MAVEN_TOKEN").orElse(sys.env.get("GITHUB_MAVEN_PASSWORD")).isDefined
      (sys.env.get("GITHUB_MAVEN_URL"), sys.env.get("GITHUB_MAVEN_USERNAME"), hasGitHubToken) match {
        case (Some(_), Some(_), true) =>
          publish.value
        case _ =>
          streams.value.log.info(s"Skipping publish for ${name.value} - GitHub Maven variables not set")
      }
    }
  )

  def githubCredentials: Seq[Credentials] = {
    val tokenEnvVar = "GITHUB_MAVEN_TOKEN" // Can be made configurable
    (sys.env.get("GITHUB_MAVEN_URL"), sys.env.get("GITHUB_MAVEN_USERNAME"), sys.env.get(tokenEnvVar).orElse(sys.env.get("GITHUB_MAVEN_PASSWORD"))) match {
      case (Some(url), Some(username), Some(password)) =>
        val host = java.net.URI.create(url).getHost
        Seq(Credentials("GitHub Package Registry", host, username, password))
      case _ =>
        // Fall back to Cloudsmith credentials
        (sys.env.get("PUBLISH_USER"), sys.env.get("PUBLISH_PASSWORD")) match {
          case (Some(user), Some(password)) =>
            Seq(Credentials("Cloudsmith API", "maven.cloudsmith.io", user, password))
          case _ =>
            Nil
        }
    }
  }
}