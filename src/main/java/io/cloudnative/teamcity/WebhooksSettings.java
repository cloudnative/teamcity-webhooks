package io.cloudnative.teamcity;

import static io.cloudnative.teamcity.WebhooksConstants.LOG;
import com.google.common.collect.Maps;
import com.google.common.io.Files;
import com.google.gson.Gson;
import jetbrains.buildServer.serverSide.ServerPaths;
import lombok.AccessLevel;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.experimental.ExtensionMethod;
import lombok.experimental.FieldDefaults;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Map;


@ExtensionMethod(LombokExtensions.class)
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public class WebhooksSettings {

  File               settingsFile;
  Map<String,String> urls;

  public WebhooksSettings(ServerPaths serverPaths) {
    settingsFile = new File(serverPaths.getConfigDir(), "webhooks-settings.json");
    urls         = restoreSettings();
  }


  @NonNull
  String getUrl(@NonNull String projectId){
    return urls.get(projectId).or("");
  }


  void setUrl(@NonNull String projectId, @NonNull String url){
    urls.put(projectId, url);
    saveSettings();
  }


  @SneakyThrows(IOException.class)
  private Map<String,String> restoreSettings(){

    if (settingsFile.isFile()) {
      try {
        String content = Files.toString(settingsFile, Charset.forName("UTF-8"));
        // noinspection unchecked
        return (Map<String,String>) new Gson().fromJson(content, Map.class);
      }
      catch (Exception e) {
        LOG.error("Failed to restore settings from '%s'".f(settingsFile.getCanonicalPath()), e);
      }
    }

    return Maps.newHashMap();
  }


  @SneakyThrows(IOException.class)
  private void saveSettings(){
    String content = new Gson().toJson(urls);
    Files.write(content, settingsFile, Charset.forName("UTF-8"));
  }
}
