package io.cloudnative.teamcity;

import com.intellij.openapi.diagnostic.Logger;
import jetbrains.buildServer.log.Loggers;


@SuppressWarnings("ConstantDeclaredInInterface")
interface WebhooksConstants {
  String PLUGIN_TITLE       = "WebHooks";
  String PLUGIN_NAME        = PLUGIN_TITLE.toLowerCase();
  String CONTROLLER_PATH    = "index.html";
  String SETTINGS_FILE      = PLUGIN_NAME + ".json";
  String S3_SETTINGS_FILE   = "s3.json";
  int    POST_TIMEOUT       = 10000;
  // https://confluence.jetbrains.com/display/TCD9/Plugin+Development+FAQ#PluginDevelopmentFAQ-HowtoUseLogging
  Logger LOG                = Loggers.SERVER;
}
