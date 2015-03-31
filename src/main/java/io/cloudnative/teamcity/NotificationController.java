package io.cloudnative.teamcity;


import jetbrains.buildServer.controllers.BaseController;
import jetbrains.buildServer.web.openapi.WebControllerManager;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.val;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


@AllArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public class NotificationController extends BaseController {

  WebControllerManager webManager;

  public void register(){
    webManager.registerController("/notification/index.html", this);
  }


  @Nullable
  @Override
  protected ModelAndView doHandle(@NotNull HttpServletRequest  request,
                                  @NotNull HttpServletResponse response) throws Exception {
    val url = request.getParameter("url");
    return null;
  }
}
