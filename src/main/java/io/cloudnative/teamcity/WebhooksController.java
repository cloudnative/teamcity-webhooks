package io.cloudnative.teamcity;


import static io.cloudnative.teamcity.WebhooksConstants.*;
import static io.cloudnative.teamcity.WebhooksUtils.*;
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


/**
 * Called when project's webhooks are updated.
 */
@ExtensionMethod(LombokExtensions.class)
@AllArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public class WebhooksController extends BaseController {

  @NonNull WebControllerManager webManager;
  @NonNull WebhooksSettings     settings;

  public void register(){
    webManager.registerController("/" + CONTROLLER_PATH, this);
  }


  @Nullable
  @Override
  @SuppressWarnings("FeatureEnvy")
  protected ModelAndView doHandle(@NotNull HttpServletRequest  request,
                                  @NotNull HttpServletResponse response) throws Exception {

    val projectId = notEmpty(request.getParameter("projectId"), "Missing 'projectId' parameter in request");
    val delete    = request.getParameter("delete");
    val add       = request.getParameter("add");

    if (notEmpty(delete)) {
      final String urlToDelete = notEmpty(request.getParameter(delete),
                                          "Missing '%s' parameter in request (url to delete)".f(delete));
      settings.removeUrl(projectId, urlToDelete);
    }
    else if (notEmpty(add)){
      final String urlToAdd = notEmpty(request.getParameter(add),
                                       "Missing '%s' parameter in request (url to add)".f(add));
      settings.addUrl(projectId, urlToAdd);
    }

    return new ModelAndView("redirect:/project.html?projectId=%s&tab=%s".f(projectId, PLUGIN_NAME));
  }
}
