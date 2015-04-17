package io.cloudnative.teamcity;

import static io.cloudnative.teamcity.WebhooksConstants.*;
import static io.cloudnative.teamcity.WebhooksUtils.*;
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
    settingsFile = new File(serverPaths.getConfigDir(), SETTINGS_FILE);
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


  @SuppressWarnings("unchecked")
  private Map<String,String> restoreSettings(){

    if (settingsFile.isFile()) {
      try {
        return (Map<String,String>) readJsonFile(settingsFile);
      }
      catch (Exception e) {
        LOG.error("Failed to restore settings from '%s'".f(path(settingsFile), e));
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
