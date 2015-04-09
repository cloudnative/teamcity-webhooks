package io.cloudnative.teamcity;


import com.google.common.collect.Maps;
import com.google.common.io.Files;
import jetbrains.buildServer.serverSide.ServerPaths;
import jodd.json.JsonParser;
import jodd.json.JsonSerializer;
import lombok.*;
import lombok.experimental.ExtensionMethod;
import lombok.experimental.FieldDefaults;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Map;


@ExtensionMethod(LombokExtensions.class)
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public class WebhooksSettings {

  private final File settingsFile;
  private final Map<String,String> urls;

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
    saveSettings(urls);
  }


  @SuppressWarnings("unchecked")
  @SneakyThrows(IOException.class)
  private Map<String,String> restoreSettings(){
    if (settingsFile.isFile()) {
      val content  = Files.toString(settingsFile, Charset.forName("UTF-8"));
      val settings = new JsonParser().parse(content, Map.class);
      return (Map<String,String>) settings;
    }
    else {
      return Maps.newHashMap();
    }
  }


  @SneakyThrows(IOException.class)
  private void saveSettings(Map<String,String> settings){
    val content = new JsonSerializer().serialize(settings);
    Files.write(content, settingsFile, Charset.forName("UTF-8"));
  }
}
