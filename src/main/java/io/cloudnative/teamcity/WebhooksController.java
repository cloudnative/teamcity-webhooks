package io.cloudnative.teamcity;


import static io.cloudnative.teamcity.WebhooksConstants.*;
import jetbrains.buildServer.controllers.BaseController;
import jetbrains.buildServer.web.openapi.WebControllerManager;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.experimental.ExtensionMethod;
import lombok.experimental.FieldDefaults;
import lombok.val;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.web.servlet.ModelAndView;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


@ExtensionMethod(LombokExtensions.class)
@AllArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public class WebhooksController extends BaseController {

  @NonNull WebControllerManager webManager;
  @NonNull
  WebhooksSettings settings;

  public void register(){
    webManager.registerController("/%s/%s".f(PLUGIN_NAME, CONTROLLER_PATH), this);
  }


  @Nullable
  @Override
  protected ModelAndView doHandle(@NotNull HttpServletRequest  request,
                                  @NotNull HttpServletResponse response) throws Exception {
    @NonNull val projectId = request.getParameter("projectId");
    @NonNull val url       = request.getParameter("url");

    settings.setUrl(projectId, url);

    return new ModelAndView("redirect:/project.html?projectId=%s&tab=%s".f(projectId, PLUGIN_NAME));
  }
}
