package io.cloudnative.teamcity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import java.util.Map;


@AllArgsConstructor(staticName = "of")
public class WebhookPayload {

  String       name;
  PayloadBuild build;

  @Builder
  static class PayloadBuild {
    String status;
    Scm    scm;
    Map<String,Map<String, String>> artifacts;
  }

  @Builder
  static class Scm {
    String url;
    String branch;
    String commit;
  }
}
