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
  String last(@NonNull String[] array) {

    if (array.length < 1) {
      throw new IllegalArgumentException("Array specified is empty");
    }

    return array[array.length - 1];
  }

  @NonNull
  String f(@NonNull String s, @NonNull Object ... args){ return String.format(s, args); }
}
