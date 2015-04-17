package io.cloudnative.teamcity;

import static io.cloudnative.teamcity.WebhookPayload.*;
import static io.cloudnative.teamcity.WebhooksConstants.*;
import static io.cloudnative.teamcity.WebhooksUtils.*;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.google.gson.Gson;
import jetbrains.buildServer.serverSide.*;
import jetbrains.buildServer.serverSide.artifacts.ArtifactsGuard;
import jetbrains.buildServer.vcs.VcsException;
import lombok.*;
import lombok.experimental.ExtensionMethod;
import lombok.experimental.FieldDefaults;
import java.io.File;
import java.util.*;


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
    try {
      String payload = new Gson().toJson(buildPayload(build));
      LOG.info("Project '%s' finished, payload is '%s'".f(build.getBuildTypeExternalId(), payload));
    }
    catch (Throwable t) {
      LOG.error("Failed to listen on buildFinished() of '%s' #%s".f(build.getFullName(), build.getBuildNumber()), t);
    }
  }


  @SuppressWarnings({"FeatureEnvy" , "ConstantConditions"})
  @SneakyThrows(VcsException.class)
  private WebhookPayload buildPayload(SRunningBuild build){
    val buildType = build.getBuildType();
    val status    = buildType.getStatusDescriptor().getStatusDescriptor().getText();
    val vcsRoots  = buildType.getVcsRootInstanceEntries();
    Scm scm = null;

    if (! vcsRoots.isEmpty()) {
      val vcsRoot = vcsRoots.get(0).getVcsRoot();
      scm = Scm.builder().url(vcsRoot.getProperty("url")).
                          branch(vcsRoot.getProperty("branch")).
                          commit(vcsRoot.getCurrentRevision().getVersion()).build();
    }

    val buildPayload = PayloadBuild.builder().status(status).scm(scm).artifacts(artifacts(build)).build();
    return WebhookPayload.of(build.getFullName(), buildPayload);
  }


  /**
   * Retrieves map of build's artifacts (archived in TeamCity and uploaded to S3):
   * {'artifact.jar' => {'archive' => 'http://teamcity/artifact/url', 's3' => 'https://s3-artifact/url'}}
   *
   * https://devnet.jetbrains.com/message/5257486
   * https://confluence.jetbrains.com/display/TCD8/Patterns+For+Accessing+Build+Artifacts
   */
  @SuppressWarnings({"ConstantConditions" , "TypeMayBeWeakened" , "CollectionDeclaredAsConcreteClass" , "FeatureEnvy"})
  private Map<String,Map<String, String>> artifacts(SBuild build){

    val buildArtifacts = buildArtifacts(build);
    if (buildArtifacts.isEmpty()) {
      return Collections.emptyMap();
    }

    val rootUrl   = buildServer.getRootUrl();
    val artifacts = new HashMap<String, Map<String, String>>();

    if (! isEmpty(rootUrl)) {
      for (val artifact : buildArtifacts){
        val artifactName = artifact.getName();
        if (".teamcity".equals(artifactName) || isEmpty(artifactName)) { continue; }

        // http://127.0.0.1:8080/repository/download/Echo_Build/37/echo-service-0.0.1-SNAPSHOT.jar
        final String url = "%s/repository/download/%s/%s/%s".f(rootUrl,
                                                               build.getBuildType().getExternalId(),
                                                               build.getBuildNumber(),
                                                               artifactName);
        artifacts.put(artifactName, map("archive", url));
      }
    }

    return Collections.unmodifiableMap(addS3Artifacts(artifacts, build));
  }


  /**
   * Retrieves current build's artifacts.
   */
  @SuppressWarnings("ConstantConditions")
  private Collection<File> buildArtifacts(SBuild build){
    val artifactsDirectory = build.getArtifactsDirectory();
    if ((artifactsDirectory == null) || (! artifactsDirectory.isDirectory())) {
      return Collections.emptyList();
    }

    try {
      artifactsGuard.lockReading(artifactsDirectory);
      File[] files = artifactsDirectory.listFiles();
      return (files == null ? Collections.<File>emptyList() : Arrays.asList(files));
    }
    finally {
      artifactsGuard.unlockReading(artifactsDirectory);
    }
  }


  /**
   * Updates map of build's artifacts with S3 URLs:
   * {'artifact.jar' => {'s3' => 'https://s3-artifact/url'}}
   */
  @SuppressWarnings("FeatureEnvy")
  private Map<String,Map<String, String>> addS3Artifacts(Map<String, Map<String, String>> artifacts,
                                                         @SuppressWarnings("TypeMayBeWeakened") SBuild build){

    val s3SettingsFile = new File(serverPaths.getConfigDir(), S3_SETTINGS_FILE);

    if (! s3SettingsFile.isFile()) {
      return artifacts;
    }

    val s3Settings   = readJsonFile(s3SettingsFile);
    val bucketName   = ((String) s3Settings.get("artifactBucket"));
    val awsAccessKey = ((String) s3Settings.get("awsAccessKey"));
    val awsSecretKey = ((String) s3Settings.get("awsSecretKey"));

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

      final String prefix = build.getFullName().replace(" :: ", "::") + '/' + build.getBuildNumber();
      val region          = s3Client.getBucketLocation(bucketName);
      val objects         = s3Client.listObjects(bucketName, prefix).getObjectSummaries();

      for (val summary : objects){
        val artifactKey = summary.getKey();
        if (isEmpty(artifactKey)) { continue; }

        final String artifactName = artifactKey.split("/").last();
        if ("build.json".equals(artifactName) || isEmpty(artifactName)) { continue; }

        // https://s3-eu-west-1.amazonaws.com/evgenyg-bakery/Echo%3A%3ABuild/45/echo-service-0.0.1-SNAPSHOT.jar
        final String url = "https://s3-%s.amazonaws.com/%s/%s".f(region, bucketName, artifactKey);

        if (artifacts.containsKey(artifactName)) {
          artifacts.get(artifactName).put("s3", url);
        }
        else {
          artifacts.put(artifactName, map("s3", url));
        }
      }
    }
    catch (Throwable t) {
      LOG.error("Failed to list objects in S3 bucket '%s'".f(bucketName), t);
    }

    return artifacts;
  }
}
