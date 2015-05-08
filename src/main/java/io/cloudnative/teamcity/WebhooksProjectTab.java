package io.cloudnative.teamcity;

import static io.cloudnative.teamcity.WebhooksConstants.*;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Ordering;
import jetbrains.buildServer.serverSide.ProjectManager;
import jetbrains.buildServer.serverSide.SProject;
import jetbrains.buildServer.serverSide.auth.Permission;
import jetbrains.buildServer.users.SUser;
import jetbrains.buildServer.web.openapi.PagePlaces;
import jetbrains.buildServer.web.openapi.PluginDescriptor;
import jetbrains.buildServer.web.openapi.project.ProjectTab;
import lombok.AccessLevel;
import lombok.experimental.ExtensionMethod;
import lombok.experimental.FieldDefaults;
import lombok.val;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import javax.servlet.http.HttpServletRequest;
import java.util.Map;


@ExtensionMethod(LombokExtensions.class)
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public class WebhooksProjectTab extends ProjectTab {

  PluginDescriptor pluginDescriptor;
  WebhooksSettings settings;

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
    model.putAll(ImmutableMap.of(
      "projectId", projectId,
      "canEdit",   (user != null) && user.getPermissionsGrantedForProject(projectId).contains(Permission.EDIT_PROJECT),
      "urls",      Ordering.natural().immutableSortedCopy(settings.getUrls(projectId)),
      "action",    CONTROLLER_PATH));
  }


  @NotNull
  @Override
  public String getIncludeUrl() {
    return pluginDescriptor.getPluginResourcesPath("projectTab.jsp");
  }
}
