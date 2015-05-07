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
import jodd.http.HttpRequest;
import jodd.http.net.SocketHttpConnection;
import lombok.*;
import lombok.experimental.ExtensionMethod;
import lombok.experimental.FieldDefaults;
import java.io.File;
import java.util.*;


@ExtensionMethod(LombokExtensions.class)
@AllArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public class WebhooksListener extends BuildServerAdapter {

  @NonNull WebhooksSettings settings;
  @NonNull SBuildServer     buildServer;
  @NonNull ServerPaths      serverPaths;
  @NonNull ArtifactsGuard   artifactsGuard;


  public void register(){
    buildServer.addListener(this);
  }


  @Override
  public void buildFinished(@NonNull SRunningBuild build) {
    val time = System.currentTimeMillis();
    try {
      val gson    = new Gson();
      val payload = gson.toJson(buildPayload(build));
      gson.fromJson(payload, Map.class); // Sanity check of JSON generated
      log("Build '%s/#%s' finished, payload is '%s'".f(build.getFullName(), build.getBuildNumber(), payload));

      for (val url : settings.getUrls(build.getProjectExternalId())){
        postPayload(url, payload);
      }

      log("Operation finished in %s ms".f(System.currentTimeMillis() - time));
    }
    catch (Throwable t) {
      error("Failed to listen on buildFinished() of '%s' #%s".f(build.getFullName(), build.getBuildNumber()), t);
    }
  }


  @SuppressWarnings({"FeatureEnvy" , "ConstantConditions"})
  @SneakyThrows(VcsException.class)
  private WebhookPayload buildPayload(@NonNull SBuild build){
    Scm scm      = null;
    val vcsRoots = build.getBuildType().getVcsRootInstanceEntries();

    if (! vcsRoots.isEmpty()) {
      val vcsRoot = vcsRoots.get(0).getVcsRoot();
      scm = Scm.builder().url(vcsRoot.getProperty("url")).
                          branch(vcsRoot.getProperty("branch").replace("refs/heads/", "origin/")).
                          commit(vcsRoot.getCurrentRevision().getVersion()).build();
    }

    final PayloadBuild payloadBuild = PayloadBuild.builder().
      // http://127.0.0.1:8080/viewLog.html?buildTypeId=Echo_Build&buildId=90
      full_url("%s/viewLog.html?buildTypeId=%s&buildId=%s".f(buildServer.getRootUrl(),
                                                             build.getBuildType().getExternalId(),
                                                             build.getBuildId())).
      build_id(build.getBuildNumber()).
      status(build.getBuildType().getStatusDescriptor().getStatusDescriptor().getText().toLowerCase()).
      scm(scm).
      artifacts(artifacts(build)).
      build();

    return WebhookPayload.of(build.getFullName(),
                             // http://127.0.0.1:8080/viewType.html?buildTypeId=Echo_Build
                             "%s/viewType.html?buildTypeId=%s".f(buildServer.getRootUrl(),
                                                                 build.getBuildType().getExternalId()),
                             payloadBuild);
  }


  /**
   * POSTs payload to the URL specified
   */
  private void postPayload(@NonNull String url, @NonNull String payload){
    try {
      val request  = HttpRequest.post(url).body(payload).open();
      // http://jodd.org/doc/http.html#sockethttpconnection
      ((SocketHttpConnection) request.httpConnection()).getSocket().setSoTimeout(POST_TIMEOUT);
      val response = request.send();

      if (response.statusCode() == 200) {
        log("Payload POST-ed to '%s'".f(url));
      }
      else {
        error("POST-ing payload to '%s' - got %s response: %s".f(url, response.statusCode(), response));
      }
    }
    catch (Throwable t) {
      error("Failed to POST payload to '%s'".f(url), t);
    }
  }


  /**
   * Retrieves map of build's artifacts (archived in TeamCity and uploaded to S3):
   * {'artifact.jar' => {'archive' => 'http://teamcity/artifact/url', 's3' => 'https://s3-artifact/url'}}
   *
   * https://devnet.jetbrains.com/message/5257486
   * https://confluence.jetbrains.com/display/TCD8/Patterns+For+Accessing+Build+Artifacts
   */
  @SuppressWarnings({"ConstantConditions", "CollectionDeclaredAsConcreteClass", "FeatureEnvy"})
  private Map<String,Map<String, String>> artifacts(@NonNull SBuild build){

    val buildArtifacts = buildArtifacts(build);
    if (buildArtifacts.isEmpty()) {
      return Collections.emptyMap();
    }

    val rootUrl   = buildServer.getRootUrl();
    @SuppressWarnings("TypeMayBeWeakened")
    val artifacts = new HashMap<String, Map<String, String>>();

    if (notEmpty(rootUrl)) {
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
  private Collection<File> buildArtifacts(@NonNull SBuild build){
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
  private Map<String,Map<String, String>> addS3Artifacts(@NonNull Map<String, Map<String, String>> artifacts,
                                                         @NonNull @SuppressWarnings("TypeMayBeWeakened") SBuild build){

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

      // "Echo::Build/15/"
      final String prefix = "%s/%s/".f(build.getFullName().replace(" :: ", "::"), build.getBuildNumber());
      val objects = s3Client.listObjects(bucketName, prefix).getObjectSummaries();

      if (objects.isEmpty()) {
        return artifacts;
      }

      val region = s3Client.getBucketLocation(bucketName);

      for (val summary : objects){
        val artifactKey = summary.getKey();
        if (isEmpty(artifactKey) || artifactKey.endsWith("/build.json")) { continue; }

        final String artifactName = artifactKey.split("/").last();
        if (isEmpty(artifactName)) { continue; }

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
      error("Failed to list objects in S3 bucket '%s'".f(bucketName), t);
    }

    return artifacts;
  }
}
