# teamcity-webhooks
A TeamCity plugin sending JSON payload to HTTP webhooks with build details when it finishes, 
similarly to [Jenkins Notification Plugin](https://wiki.jenkins-ci.org/display/JENKINS/Notification+Plugin).

Sample notification payload:

    {
      "name": "Echo :: Build",
      "url": "http://127.0.0.1:8080/viewType.html?buildTypeId=Echo_Build",
      "build": {
        "full_url": "http://127.0.0.1:8080/viewLog.html?buildTypeId=Echo_Build&buildId=14",
        "build_id": "7",
        "status": "success",
        "scm": {
          "url": "https://github.com/evgeny-goldin/echo-service.git",
          "branch": "origin/master",
          "commit": "6bef6af1f43fb3e5e6d73f1e3332e82dae1f55d4"
        },
        "artifacts": {
          "echo-service-0.0.1-SNAPSHOT.jar": {
            "s3": "https://s3-eu-west-1.amazonaws.com/evgenyg-bakery/Echo::Build/7/echo-service-0.0.1-SNAPSHOT.jar",
            "archive": "http://127.0.0.1:8080/repository/download/Echo_Build/7/echo-service-0.0.1-SNAPSHOT.jar"
          }
        }
      }
    }

The payload submitted includes the following information:

* Job's name and URL.
* Build's number, full URL, and status.
* SCM URL, branch and commit (tested only with Git repositories).
* Artifacts generated. 

## Installation:

Download the latest plugin version from [dl.bintray.com/cloudnative/teamcity/teamcity-webhooks/](https://dl.bintray.com/cloudnative/teamcity/teamcity-webhooks/), copy `"webhooks.zip"` to `"~/.BuildServer/plugins/"` and restart TeamCity.  

## Usage:

WebHooks are specified at the project level (not to be confused with build configuration). Click the project name and go to the "WebHooks" tab where URLs can be added or deleted, if you user have "Edit" permissions for the project (otherwise, you'll see WebHooks URLs but will not be able to edit them).

![Project WebHooks](https://raw.githubusercontent.com/cloudnative/teamcity-webhooks/master/images/webhooks-tab.png)

In order for the plugin to report on artifacts produced in a certain build configuration (not to be confused with project), they need to be defined in build configuration Settings => "General Settings" => "Artifact paths". 

![Project WebHooks](https://raw.githubusercontent.com/cloudnative/teamcity-webhooks/master/images/artifact-paths.png)

Also, TeamCity needs to have its URL configured in "Administration" => "Server Administration" => "Global Settings" => "Server URL".

![Project WebHooks](https://raw.githubusercontent.com/cloudnative/teamcity-webhooks/master/images/server-url.png)

Once WebHooks are set for a project, all its build configurations will POST a JSON payload when builds finish.

## Java and TeamCity versions:

The plugin requires Java 6 to work.
The plugin was tested with TeamCity 8.0 and 9.0.3. Therefore, it shouldn't break with other TeamCity 8.x/9.x versions.  

## S3 support:

If you're using [TeamCity S3 plugin](https://github.com/guardian/teamcity-s3-plugin) the plugin will also include artifacts S3 URLs. Note that your S3 bucket needs to allow anonymous downloads for artifacts to be downloaded.


## Building the plugin locally:

    mvn clean package

## Releasing a new plugin's version:

Here I assume `"origin"` refers to the [github.com/cloudnative/teamcity-webhooks](https://github.com/cloudnative/teamcity-webhooks) repo.

    git checkout release
    git merge master
    git push origin release


This will trigger a release build at [circleci.com/gh/cloudnative/teamcity-webhooks](https://circleci.com/gh/cloudnative/teamcity-webhooks) and upload the new version to [dl.bintray.com/cloudnative/teamcity/teamcity-webhooks/](https://dl.bintray.com/cloudnative/teamcity/teamcity-webhooks/). The release is made from the "master" branch, the actual content pushed to "release" doesn't really matter. DO NOT push to "master" while the release build is running, **let it finish first**. Then execute:

    git checkout master
    git pull origin master

.. and continue your work on a `"master"` branch, as before.