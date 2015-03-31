package io.cloudnative.teamcity;

import lombok.NonNull;
import lombok.experimental.UtilityClass;


/**
 * http://projectlombok.org/features/experimental/ExtensionMethod.html
 */
@UtilityClass
class LombokExtensions {

  @NonNull
  <T> T or(T o, @NonNull T defaultOption) { return (o != null ? o : defaultOption); }

  @NonNull
  String f(String s, Object ... args){ return String.format(s, args); }
}
