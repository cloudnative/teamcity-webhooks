package io.cloudnative.teamcity;

import static io.cloudnative.teamcity.WebhooksConstants.*;
import jetbrains.buildServer.serverSide.BuildServerAdapter;
import jetbrains.buildServer.serverSide.SBuildServer;
import jetbrains.buildServer.serverSide.SRunningBuild;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.experimental.ExtensionMethod;
import lombok.experimental.FieldDefaults;
import lombok.val;


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
    val projectId = build.getProjectExternalId();
    val url       = settings.getUrl(projectId);
    LOG.info("Project '%s' finished, url is '%s'".f(projectId, url));
  }
}
