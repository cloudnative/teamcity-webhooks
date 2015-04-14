package io.cloudnative.teamcity;

import static io.cloudnative.teamcity.WebhookPayload.Build;
import static io.cloudnative.teamcity.WebhookPayload.Scm;
import static io.cloudnative.teamcity.WebhooksConstants.*;
import static io.cloudnative.teamcity.WebhooksUtils.*;
import com.amazonaws.AmazonClientException;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;
import jetbrains.buildServer.serverSide.*;
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


  public void register(){
    buildServer.addListener(this);
  }


  @Override
  public void buildFinished(SRunningBuild finishedBuild) {
    String payload = new Gson().toJson(buildPayload(finishedBuild));
    LOG.info("Project '%s' finished, payload is '%s'".f(finishedBuild.getBuildTypeExternalId(), payload));
  }


  @SuppressWarnings("FeatureEnvy")
  @SneakyThrows(VcsException.class)
  private WebhookPayload buildPayload(jetbrains.buildServer.Build finishedBuild){
    @NonNull SBuildType buildType =
      buildServer.getProjectManager().findBuildTypeByExternalId(finishedBuild.getBuildTypeExternalId());
    @SuppressWarnings("ConstantConditions")
    val status = buildType.getStatusDescriptor().getStatusDescriptor().getText();
    List<VcsRootInstanceEntry> vcsRoots = buildType.getVcsRootInstanceEntries();
    Map<String,Map<String, String>> artifacts = s3Artifacts(finishedBuild);
    Scm scm = null;

    if (! vcsRoots.isEmpty()) {
      @NonNull val vcsRoot = vcsRoots.get(0).getVcsRoot();
      scm = Scm.builder().url(vcsRoot.getProperty("url")).
                          branch(vcsRoot.getProperty("branch")).
                          commit(vcsRoot.getCurrentRevision().getVersion()).build();
    }

    val build = Build.builder().status(status).scm(scm).artifacts(artifacts).build();
    return WebhookPayload.of(finishedBuild.getFullName(), build);
  }


  /**
   * Retrieves map of build's S3 artifacts:
   * {'artifact.jar' => {'s3' => 'https://artifact/url'}}
   */
  private Map<String,Map<String, String>> s3Artifacts(jetbrains.buildServer.Build finishedBuild){

    final File s3SettingsFile = new File(serverPaths.getConfigDir(), S3_SETTINGS_FILE);

    if (!s3SettingsFile.isFile()) {
      return Collections.emptyMap();
    }

    val s3Settings            = readJsonFile(s3SettingsFile);
    final String bucketName   = ((String) s3Settings.get("bucketName")).or("");
    final String awsAccessKey = ((String) s3Settings.get("awsAccessKey")).or("");
    final String awsSecretKey = ((String) s3Settings.get("awsSecretKey")).or("");

    if (isEmpty(bucketName, awsAccessKey, awsSecretKey)) {
      return Collections.emptyMap();
    }

    try {
      AmazonS3 s3Client = new AmazonS3Client(new BasicAWSCredentials(awsAccessKey, awsSecretKey));
      if (! s3Client.doesBucketExist(bucketName)) {
        return Collections.emptyMap();
      }

      String   prefix   = "%s/%s".f(finishedBuild.getFullName().replace( " :: ", "::" ),
                                    finishedBuild.getBuildNumber());
      List<S3ObjectSummary> objects = s3Client.listObjects(bucketName, prefix).getObjectSummaries();

      if (objects.size() < 1) {
        return Collections.emptyMap();
      }

      final Map<String, Map<String, String>> artifacts = new HashMap<String, Map<String, String>>();

      for (S3ObjectSummary summary : objects){
        String key = summary.getKey();
        if (key.contains("/artifacts/")) {
          final String artifact = key.split("/").last();
          final String url      = "https://s3-%s.amazonaws.com/%s/%s".f(s3Client.getBucketLocation(bucketName), bucketName, key);
          artifacts.put(artifact, ImmutableMap.of("s3", url));
        }
      }

      return artifacts;
    }
    catch (AmazonClientException e) {
      LOG.error("Failed to list objects of S3 bucket '%s'".f(bucketName), e);
      return Collections.emptyMap();
    }
  }
}
