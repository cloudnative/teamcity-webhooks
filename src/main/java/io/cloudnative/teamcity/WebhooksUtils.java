package io.cloudnative.teamcity;

import com.google.common.io.Files;
import com.google.gson.Gson;
import lombok.NonNull;
import lombok.experimental.ExtensionMethod;
import lombok.val;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Map;


@ExtensionMethod(LombokExtensions.class)
final class WebhooksUtils {

  private WebhooksUtils() {}


  static Map readJsonFile(File file){
    try {
      val content = Files.toString(file, Charset.forName("UTF-8"));
      return new Gson().fromJson(content, Map.class);
    }
    catch (IOException e) {
      throw new RuntimeException("Failed to read file '%s'".f(file.getAbsolutePath()), e);
    }
  }


  static boolean isEmpty(@NonNull String ... strings){
    for (String s : strings){
      if ((s == null) || (s.trim().length() < 1)) {
        return true;
      }
    }
    return false;
  }
}
