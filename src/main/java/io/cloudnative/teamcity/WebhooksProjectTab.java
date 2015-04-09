package io.cloudnative.teamcity;

import static io.cloudnative.teamcity.WebhooksConstants.*;
import com.google.common.collect.ImmutableMap;
import jetbrains.buildServer.serverSide.ProjectManager;
import jetbrains.buildServer.serverSide.SProject;
import jetbrains.buildServer.users.SUser;
import jetbrains.buildServer.web.openapi.PagePlaces;
import jetbrains.buildServer.web.openapi.PluginDescriptor;
import jetbrains.buildServer.web.openapi.project.ProjectTab;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.val;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;


@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public class WebhooksProjectTab extends ProjectTab {

  PluginDescriptor     pluginDescriptor;
  WebhooksSettings settings;

  private static final Logger LOG = Logger.getLogger(WebhooksProjectTab.class);

  public WebhooksProjectTab(@NotNull PagePlaces pagePlaces,
                            @NotNull ProjectManager projectManager,
                            @NotNull PluginDescriptor pluginDescriptor,
                            @NotNull WebhooksSettings settings) {
    super(PLUGIN_NAME, PLUGIN_TITLE, pagePlaces, projectManager);
    this.pluginDescriptor = pluginDescriptor;
    this.settings         = settings;
  }


  @Override
  protected void fillModel (@NotNull  Map<String, Object> model,
                            @NotNull  HttpServletRequest request,
                            @NotNull  SProject project,
                            @Nullable SUser user){
    val projectId = project.getExternalId();
    model.putAll(ImmutableMap.of("projectId", projectId,
                                 "url",       settings.getUrl(projectId),
                                 "action",    PLUGIN_NAME + "/" + CONTROLLER_PATH));
    LOG.info("Project tab model: " + model);
  }


  @NotNull
  @Override
  public String getIncludeUrl() {
    return pluginDescriptor.getPluginResourcesPath("projectTab.jsp");
  }
}
