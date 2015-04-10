package io.cloudnative.teamcity;

import static io.cloudnative.teamcity.WebhookPayload.*;
import static io.cloudnative.teamcity.WebhooksConstants.LOG;
import com.google.gson.Gson;
import jetbrains.buildServer.serverSide.BuildServerAdapter;
import jetbrains.buildServer.serverSide.SBuildServer;
import jetbrains.buildServer.serverSide.SBuildType;
import jetbrains.buildServer.serverSide.SRunningBuild;
import jetbrains.buildServer.vcs.VcsRootInstanceEntry;
import lombok.*;
import lombok.experimental.ExtensionMethod;
import lombok.experimental.FieldDefaults;
import java.util.List;


@ExtensionMethod(LombokExtensions.class)
@AllArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public class WebhooksListener extends BuildServerAdapter {

  @NonNull WebhooksSettings settings;
  @NonNull SBuildServer     buildServer;


  public void register(){
    buildServer.addListener(this);
  }


  @Override
  public void buildFinished(SRunningBuild build) {
    String payload = new Gson().toJson(buildPayload(build));
    LOG.info("Project '%s' finished, payload is '%s'".f(build.getBuildTypeExternalId(), payload));
  }


  @SuppressWarnings("FeatureEnvy")
  @SneakyThrows(jetbrains.buildServer.vcs.VcsException.class)
  private WebhookPayload buildPayload(jetbrains.buildServer.Build finishedBuild){
    @NonNull SBuildType buildType = buildServer.getProjectManager().findBuildTypeByExternalId(finishedBuild.getBuildTypeExternalId());
    @SuppressWarnings("ConstantConditions")
    val status = buildType.getStatusDescriptor().getStatusDescriptor().getText();
    List<VcsRootInstanceEntry> vcsRoots = buildType.getVcsRootInstanceEntries();

    if (! vcsRoots.isEmpty()) {
      val vcsRoot   = vcsRoots.get(0).getVcsRoot();
      val vcsUrl    = vcsRoot.getProperty("url");
      val vcsBranch = vcsRoot.getProperty("branch");
      val vcsCommit = vcsRoot.getCurrentRevision().getVersion();
      val scm       = Scm.builder().url(vcsUrl).branch(vcsBranch).commit(vcsCommit).build();
      val build     = Build.builder().status(status).scm(scm).build();
      return WebhookPayload.of(finishedBuild.getFullName(), build);
    }
    else {
      return WebhookPayload.of(finishedBuild.getFullName(), Build.builder().status(status).build());
    }
  }
}
