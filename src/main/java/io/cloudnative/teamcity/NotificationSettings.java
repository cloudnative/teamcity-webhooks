package io.cloudnative.teamcity;


import com.google.common.collect.Maps;
import jetbrains.buildServer.serverSide.MainConfigProcessor;
import lombok.AccessLevel;
import lombok.NonNull;
import lombok.experimental.ExtensionMethod;
import lombok.experimental.FieldDefaults;
import org.jdom.Element;

import java.util.Map;


@ExtensionMethod({LombokExtensions.class})
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public class NotificationSettings implements MainConfigProcessor {

  private final Map<String,String> urls = Maps.newHashMap();


  public void readFrom(Element element) {
  }

  public void writeTo(Element element) {
  }


  @NonNull
  String getUrl(@NonNull String projectId){
    return urls.get(projectId).or("");
  }


  void setUrl(@NonNull String projectId, @NonNull String url){
    urls.put(projectId, url);
  }
}
