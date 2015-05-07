package io.cloudnative.teamcity;

import static io.cloudnative.teamcity.WebhooksConstants.*;
import com.google.common.io.Files;
import com.google.gson.Gson;
import lombok.NonNull;
import lombok.experimental.ExtensionMethod;
import lombok.val;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;


@ExtensionMethod(LombokExtensions.class)
final class WebhooksUtils {

  private WebhooksUtils() {}


  @SuppressWarnings({"TypeMayBeWeakened" , "CollectionDeclaredAsConcreteClass"})
  static <K, V> Map<K, V> map(@NonNull K key, @NonNull V value){
    val map = new HashMap<K, V>();
    map.put(key, value);
    return map;
  }


  /**
   * Reads JSON file specified and de-serializes it to Map.
   */
  @SuppressWarnings("unchecked")
  static Map<String, ?> readJsonFile(@NonNull File file){
    try {
      val content = Files.toString(file, Charset.forName("UTF-8"));
      return (Map<String, ?>) new Gson().fromJson(content, Map.class);
    }
    catch (IOException e) {
      throw new RuntimeException("Failed to read JSON file '%s'".f(path(file), e));
    }
  }


  /**
   * Retrieves file's path, canonical or absolute.
   */
  static String path(@NonNull File f){
    try { return f.getCanonicalPath(); }
    catch (IOException e) { return f.getAbsolutePath(); }
  }


  /**
   * Determines if one of Strings specified is null or empty (after trim()).
   */
  static boolean isEmpty(@NonNull String ... strings){
    for (String s : strings){
      if ((s == null) || (s.trim().length() < 1)) {
        return true;
      }
    }
    return false;
  }


  /**
   * Determines if String specified is not null and non-empty.
   */
  static boolean notEmpty(String s){
    return (! isEmpty(s));
  }


  /**
   * Verifies String specified is not null and non-empty.
   */
  @SuppressWarnings("StaticMethodOnlyUsedInOneClass")
  static String notEmpty(String s, @NonNull String message){
    if (isEmpty(s)) {
      throw new RuntimeException(message);
    }
    return s;
  }


  static void log(@NonNull String message){
    LOG.info("WebHooks plugin - " + message);
  }


  static void error(@NonNull String message){
    error(message, null);
  }


  static void error(@NonNull String message, Throwable error){
    if (error == null) {
      LOG.error("WebHooks plugin - " + message);
    }
    else {
      LOG.error("WebHooks plugin - " + message, error);
    }
  }
}
