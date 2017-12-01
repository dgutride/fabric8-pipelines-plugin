package dsl

import io.fabric8.utils.Strings
import org.apache.maven.model.Model
import org.jenkinsci.plugins.fabric8.model.ServiceConstants
import org.jenkinsci.plugins.fabric8.steps.PromoteArtifacts
import org.jenkinsci.plugins.fabric8.steps.PromoteImages
import org.jenkinsci.plugins.fabric8.steps.ReleaseProject
import org.jenkinsci.plugins.fabric8.steps.TagImages
import org.jenkinsci.plugins.fabric8.steps.WaitUntilArtifactSyncedWithCentral
import org.jenkinsci.plugins.fabric8.steps.WaitUntilPullRequestMerged

def call(ReleaseProject.Arguments arguments) {
  echo "releaseProject ${arguments}"

  def flow = new Fabric8Commands()

  PromoteArtifacts.Arguments promoteArtifactsArguments = arguments.createPromoteArtifactsArguments()
  String pullRequestId = promoteArtifacts(promoteArtifactsArguments)

  PromoteImages.Arguments promoteImagesArguments = arguments.createPromoteImagesArguments()
  def promoteDockerImages = promoteImagesArguments.images
  if (promoteDockerImages.size() > 0) {
    def validation = promoteImagesArguments.validate()
    if (validation != null) {
      error validation
    } else {
      promoteImages(promoteImagesArguments)
    }
  }


  TagImages.Arguments tagImagesArguments = arguments.createTagImagesArguments()
  def tagDockerImages = tagImagesArguments.images
  if (tagDockerImages && tagDockerImages.size() > 0) {
    tagImages(tagImagesArguments)
  }

  if (pullRequestId != null) {
    WaitUntilPullRequestMerged.Arguments waitUntilPullRequestMergedArguments = arguments.createWaitUntilPullRequestMergedArguments(pullRequestId)
    waitUntilPullRequestMerged(waitUntilPullRequestMergedArguments)
  }

  WaitUntilArtifactSyncedWithCentral.Arguments waitUntilArtifactSyncedWithCentralArguments = arguments.createWaitUntilArtifactSyncedWithCentralArguments()
  Model mavenProject = flow.loadMavenPom()
  defaultWaitInfoFromPom(waitUntilArtifactSyncedWithCentralArguments, mavenProject)

  if (waitUntilArtifactSyncedWithCentralArguments.isValid()) {
    waitUntilArtifactSyncedWithCentral(waitUntilArtifactSyncedWithCentralArguments)
  } 
}

/**
 * If no properties are configured explicitly lets try default them from the pom.xml
 */
def defaultWaitInfoFromPom(WaitUntilArtifactSyncedWithCentral.Arguments arguments, Model mavenProject) {
  if (mavenProject != null) {
    if (Strings.isNullOrBlank(arguments.groupId)) {
      arguments.groupId = mavenProject.groupId
    }
    if (Strings.isNullOrBlank(arguments.artifactId)) {
      arguments.artifactId = mavenProject.artifactId
    }
    if (Strings.isNullOrBlank(arguments.extension)) {
      arguments.extension = "pom";
    }
    if (Strings.isNullOrBlank(arguments.repositoryUrl)) {
      arguments.repositoryUrl = ServiceConstants.MAVEN_CENTRAL
    }
  }
}


