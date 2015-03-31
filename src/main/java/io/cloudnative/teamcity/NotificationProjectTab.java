package io.cloudnative.teamcity;


import jetbrains.buildServer.serverSide.ProjectManager;
import jetbrains.buildServer.serverSide.SProject;
import jetbrains.buildServer.users.SUser;
import jetbrains.buildServer.web.openapi.PagePlaces;
import jetbrains.buildServer.web.openapi.PluginDescriptor;
import jetbrains.buildServer.web.openapi.project.ProjectTab;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;


public class NotificationProjectTab extends ProjectTab {

  PluginDescriptor pluginDescriptor;

  public NotificationProjectTab(@NotNull PagePlaces pagePlaces,
                                @NotNull ProjectManager projectManager,
                                @NotNull PluginDescriptor pluginDescriptor) {
    super("notification", "Notification", pagePlaces, projectManager);
    this.pluginDescriptor = pluginDescriptor;
  }


  @Override
  protected void fillModel (@NotNull  Map<String, Object> model,
                            @NotNull  HttpServletRequest request,
                            @NotNull  SProject project,
                            @Nullable SUser user){
    model.put("projectName", project.getName());
  }


  @NotNull
  @Override
 	public String getIncludeUrl() {
    return resourcePath("projectTab.jsp");
  }


  @NotNull
  private String resourcePath(String relativePath){
    return pluginDescriptor.getPluginResourcesPath(relativePath);
  }
}
