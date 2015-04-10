package io.cloudnative.teamcity;

import com.intellij.openapi.diagnostic.Logger;


@SuppressWarnings("ConstantDeclaredInInterface")
interface WebhooksConstants {
  String PLUGIN_TITLE    = "WebHooks";
  String PLUGIN_NAME     = PLUGIN_TITLE.toLowerCase();
  String CONTROLLER_PATH = "index.html";
  Logger LOG             = Logger.getInstance(WebhooksConstants.class.getPackage().getName());
}
