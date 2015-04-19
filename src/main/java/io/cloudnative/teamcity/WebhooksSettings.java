package io.cloudnative.teamcity;

import static io.cloudnative.teamcity.WebhooksConstants.*;
import static io.cloudnative.teamcity.WebhooksUtils.*;
import com.google.common.collect.Sets;
import com.google.common.io.Files;
import com.google.gson.Gson;
import jetbrains.buildServer.serverSide.ServerPaths;
import lombok.AccessLevel;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.experimental.ExtensionMethod;
import lombok.experimental.FieldDefaults;
import lombok.val;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.*;


@ExtensionMethod(LombokExtensions.class)
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public class WebhooksSettings {

  File                    settingsFile;
  Map<String,Set<String>> urls;

  public WebhooksSettings(ServerPaths serverPaths) {
    settingsFile = new File(serverPaths.getConfigDir(), SETTINGS_FILE);
    urls         = restoreSettings();
  }


  @SuppressWarnings("ConstantConditions")
  @NonNull
  Set<String> getUrls(@NonNull String projectId){
    return urls.get(projectId).or(Collections.<String>emptySet());
  }


  void addUrl(@NonNull String projectId, @NonNull String url){

    if (! urls.containsKey(projectId)) {
      urls.put(projectId, Sets.<String>newHashSet());
    }

    urls.get(projectId).add(url);
    saveSettings();
  }


  @SuppressWarnings("unchecked")
  private Map<String,Set<String>> restoreSettings(){

    @SuppressWarnings({"TypeMayBeWeakened", "CollectionDeclaredAsConcreteClass"})
    val result = new HashMap<String, Set<String>>();

    if (settingsFile.isFile()) {
      try {
        Map<String, List<String>> map = (Map<String, List<String>>) readJsonFile(settingsFile);
        for (String url : map.keySet()){
          result.put(url, Sets.newHashSet(map.get(url)));
        }
      }
      catch (Exception e) {
        LOG.error("Failed to restore settings from '%s'".f(path(settingsFile), e));
      }
    }

    return result;
  }


  @SneakyThrows(IOException.class)
  private void saveSettings(){
    String content = new Gson().toJson(urls);
    Files.write(content, settingsFile, Charset.forName("UTF-8"));
  }
}
