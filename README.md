# Snow Owl

## Introduction
Snow Owl is a terminology server and a collaborative terminology authoring platform.  The authoring platform maintains terminology artifacts developed by a team and supported by business workflows that are driven by external task management systems like Bugzilla and JIRA.  With its modular design, the server can maintain multiple terminologies where new terminologies can be plugged-in to the platform.  The functionality of Snow Owl is exposed via a REST API.

## Build

Snow Owl uses Maven for its build system.

In order to create a distribution, simply run the `mvn clean package -Pdependencies -Psite -Pdist` command in the cloned directory.

To run the test cases, simply run:

    mvn clean verify -Pdependencies -Psite -Pdist

The distribution package can be found in the `releng/distribution/target` folder, when the build completes.

## Release

It is not possible to use JGitFlow to perform a release from the develop to master branch because although it will try to update the pom files, it is not set up to set the same versions in the various MANIFEST.MF files, and so a manual merge and version update must be performed.

Is it important when an update to version numbers is done in the master branch, that this is merged back to the develop branch (and then the versions updated further to develop versions) so that future merges from develop to master can be performed without conflicts.

To modify versions, use Tycho:
```
mvn -Pdependencies -Dtycho.mode=maven org.eclipse.tycho:tycho-versions-plugin:set-version -DnewVersion=4.2.2-SNAPSHOT
```

In addition, the package org.eclipse.equinox.bundles may need updating since it will try to deploy with the rest of the code, and if the release version is not updated, Nexus will reject it as it rejects all attempted overwrites.

### Additional Build Improvements

Here are few tips to improve the quality of the default build process.

#### Nexus

We highly recommend to install a local artifact repository (`Nexus OSS` is supported), so the build can deploy and reuse (in downstream projects) `Maven` and `p2` artifacts.

1. Download and install Nexus OSS or Professional (http://www.sonatype.org/nexus/go/).
2. Install `Nexus Unzip Plugin` to easily reference p2 repositories deployed as zip: https://wiki.eclipse.org/Tycho/Nexus_Unzip_Plugin
3. Define the `nexus.url` parameter in the `settings.xml` file under `.m2` folder on your build server (use the `settings.xml` in the root of this repository as template).
4. Define a deployment user in Nexus, and reference it in the `.m2/settings.xml` file.
5. Use `mvn clean deploy` instead of `mvn clean verify` when you execute the process.
6. *Optional: deploy only if build succeeds (requires a `Jenkins CI` job with post build step to deploy artifacts to `Nexus`*

#### Prefetched target platform

The `-Pdependencies` profile includes all required third party repositories and modules as part of the build process using Tycho's p2 and Maven dependency resolution capabilities. 
While this should be enough to run the process, in production builds we recommend using a prefetched target platform, as it will ensure consistent third party versions and reduces the execution time significantly.

1. Create the target platform update site, run `mvn clean verify -Pdependencies -Ptarget_site` from the **releng** folder
2. Navigate to `com.b2international.snowowl.server.target.update/target` folder
3. Copy the `target_platform_<version>` folder to a webserver, or use `Nexus` to serve the site as unzipped p2 (requires Nexus OSS with Unzip Plugin installed, see previous section)
4. Define an `http` URL as `target.platform.url` parameter in the global Maven `.m2/settings.xml` file
5. Run Snow Owl maven process with `mvn clean verify -Ptp_dependencies -Psite -Pdist` (*NOTE: the tp_dependencies profile will use the prefetched local p2 repository instead of querying all remote sites*)
