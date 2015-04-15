package io.cloudnative.teamcity;

import static io.cloudnative.teamcity.WebhookPayload.*;
import static io.cloudnative.teamcity.WebhooksConstants.*;
import static io.cloudnative.teamcity.WebhooksUtils.*;
import com.amazonaws.AmazonClientException;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.gson.Gson;
import jetbrains.buildServer.serverSide.*;
import jetbrains.buildServer.serverSide.artifacts.ArtifactsGuard;
import jetbrains.buildServer.vcs.VcsException;
import jetbrains.buildServer.vcs.VcsRootInstanceEntry;
import lombok.*;
import lombok.experimental.ExtensionMethod;
import lombok.experimental.FieldDefaults;
import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@ExtensionMethod(LombokExtensions.class)
@AllArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public class WebhooksListener extends BuildServerAdapter {

  @NonNull WebhooksSettings   settings;
  @NonNull SBuildServer       buildServer;
  @NonNull ServerPaths        serverPaths;
  @NonNull ArtifactsGuard     artifactsGuard;


  public void register(){
    buildServer.addListener(this);
  }


  @Override
  public void buildFinished(SRunningBuild build) {
    String payload = new Gson().toJson(buildPayload(build));
    LOG.info("Project '%s' finished, payload is '%s'".f(build.getBuildTypeExternalId(), payload));
  }


  @SuppressWarnings("FeatureEnvy")
  @SneakyThrows(VcsException.class)
  private WebhookPayload buildPayload(SRunningBuild build){
    @NonNull SBuildType buildType =
      buildServer.getProjectManager().findBuildTypeByExternalId(build.getBuildTypeExternalId());
    @SuppressWarnings("ConstantConditions")
    val status = buildType.getStatusDescriptor().getStatusDescriptor().getText();
    List<VcsRootInstanceEntry> vcsRoots = buildType.getVcsRootInstanceEntries();
    Map<String,Map<String, String>> artifacts = artifacts(build);
    Scm scm = null;

    if (! vcsRoots.isEmpty()) {
      @NonNull val vcsRoot = vcsRoots.get(0).getVcsRoot();
      scm = Scm.builder().url(vcsRoot.getProperty("url")).
                          branch(vcsRoot.getProperty("branch")).
                          commit(vcsRoot.getCurrentRevision().getVersion()).build();
    }

    val buildPayload = Build.builder().status(status).scm(scm).artifacts(artifacts).build();
    return WebhookPayload.of(build.getFullName(), buildPayload);
  }


  /**
   * Retrieves map of build's artifacts (archived in TeamCity and uploaded to S3):
   * {'artifact.jar' => {'archive' => 'http://teamcity/artifact/url', 's3' => 'https://s3-artifact/url'}}
   *
   * https://devnet.jetbrains.com/message/5257486
   * https://confluence.jetbrains.com/display/TCD8/Patterns+For+Accessing+Build+Artifacts
   */
  private Map<String,Map<String, String>> artifacts(@SuppressWarnings("TypeMayBeWeakened") SRunningBuild build){
    val artifactsDirectory = build.getArtifactsDirectory();
    if ((artifactsDirectory == null) || (! artifactsDirectory.isDirectory())) {
      return Collections.emptyMap();
    }

    final Map<String, Map<String, String>> artifacts = new HashMap<String, Map<String, String>>();

    artifactsGuard.lockReading(artifactsDirectory);
    for (val artifact : artifactsDirectory.listFiles()){
      // http://127.0.0.1:8080/repository/download/Echo_Build/37/echo-service-0.0.1-SNAPSHOT.jar
      val artifactName = artifact.getName();
      if (".teamcity".equals(artifactName)) { continue; }

      final String url = "%s/repository/download/%s/%s/%s".f(buildServer.getRootUrl(),
                                                             build.getBuildType().getExternalId(),
                                                             build.getBuildNumber(),
                                                             artifactName);
      artifacts.put(artifactName, Maps.newHashMap(ImmutableMap.of("archive", url)));
    }

    artifactsGuard.unlockReading(artifactsDirectory);
    return Collections.unmodifiableMap(addS3Artifacts(artifacts, build));
  }



  /**
   * Retrieves map of build's S3 artifacts:
   * {'artifact.jar' => {'s3' => 'https://s3-artifact/url'}}
   */
  private Map<String,Map<String, String>> addS3Artifacts(Map<String, Map<String, String>> artifacts, SRunningBuild build){

    final File s3SettingsFile = new File(serverPaths.getConfigDir(), S3_SETTINGS_FILE);

    if (!s3SettingsFile.isFile()) {
      return artifacts;
    }

    val s3Settings            = readJsonFile(s3SettingsFile);
    final String bucketName   = ((String) s3Settings.get("artifactBucket")).or("");
    final String awsAccessKey = ((String) s3Settings.get("awsAccessKey")).or("");
    final String awsSecretKey = ((String) s3Settings.get("awsSecretKey")).or("");

    if (isEmpty(bucketName)) {
      return artifacts;
    }

    try {
      AmazonS3 s3Client = isEmpty(awsAccessKey, awsSecretKey) ?
        new AmazonS3Client() :
        new AmazonS3Client(new BasicAWSCredentials(awsAccessKey, awsSecretKey));

      if (! s3Client.doesBucketExist(bucketName)) {
        return artifacts;
      }

      final String prefix = "%s/%s".f(build.getFullName().replace(" :: ", "::"),
                                      build.getBuildNumber());
      val objects = s3Client.listObjects(bucketName, prefix).getObjectSummaries();

      if (objects.isEmpty()) {
        return artifacts;
      }

      val region = s3Client.getBucketLocation(bucketName);

      for (val summary : objects){
        val artifactKey = summary.getKey();
        final String artifactName = artifactKey.split("/").last();

        if ("build.json".equals(artifactName)) { continue; }

        final String url = "https://s3-%s.amazonaws.com/%s/%s".f(region, bucketName, artifactKey);
        if (artifacts.containsKey(artifactName)) {
          artifacts.get(artifactName).put("s3", url);
        }
        else {
          artifacts.put(artifactName, ImmutableMap.of("s3", url));
        }
      }

      return artifacts;
    }
    catch (AmazonClientException e) {
      LOG.error("Failed to list objects in S3 bucket '%s'".f(bucketName), e);
      return artifacts;
    }
  }
}
