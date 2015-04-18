package io.cloudnative.teamcity;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.experimental.FieldDefaults;
import java.util.Map;


/**
 * https://cloudnative.io/docs/bakery/json-webhook/
 */
@AllArgsConstructor(staticName = "of")
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public class WebhookPayload {

  String       name;
  String       url;
  PayloadBuild build;

  @Builder
  static class PayloadBuild {
    String full_url;
    String build_id;
    String status;
    Scm    scm;
    Map<String,Map<String,String>> artifacts;
  }

  @Builder
  static class Scm {
    String url;
    String branch;
    String commit;
  }
}
