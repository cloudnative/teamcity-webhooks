package io.cloudnative.teamcity;

import lombok.AllArgsConstructor;
import lombok.Builder;


@AllArgsConstructor(staticName = "of")
public class WebhookPayload {

  String name;
  Build  build;

  @Builder
  static class Build {
    String status;
    Scm    scm;
  }

  @Builder
  static class Scm {
    String url;
    String branch;
    String commit;
  }
}
