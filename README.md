# Patcher Maven Plugin

An easy way to patch Maven projects.

This repository contains
- `patcher-maven-plugin` - a Maven plugin that allows to apply patches to a Maven project.
- `patch-projects` an easy way to patch Maven projects with the `patcher-maven-plugin`, for example, to fix the [CVE-2022-36033](https://github.com/jhy/jsoup/security/advisories/GHSA-gp7f-rwcx-9369) in the jsoup project 1.15.2


## Install the plugin locally

```bash
cd patcher-maven-plugin
mvn clean install
...
[INFO] --- invoker:3.8.1:integration-test (integration-test) @ patcher-maven-plugin ---
[INFO] Building: jsoup-it/pom.xml
[INFO] run post-build script verify.groovy
[INFO]           jsoup-it/pom.xml ................................. SUCCESS (44.06 s)
[INFO]
[INFO] --- invoker:3.8.1:verify (integration-test) @ patcher-maven-plugin ---
[INFO] -------------------------------------------------
[INFO] Build Summary:
[INFO]   Passed: 1, Failed: 0, Errors: 0, Skipped: 0
[INFO] -------------------------------------------------
[INFO]
[INFO] --- install:3.1.2:install (default-install) @ patcher-maven-plugin ---
[INFO] Installing /Users/mgreau/git/mgreau/maven-zero-cves/patcher/pom.xml to /Users/mgreau/.m2/repository/com/mgreau/patcher-maven-plugin/0.1.0-SNAPSHOT/patcher-maven-plugin-0.1.0-SNAPSHOT.pom
[INFO] Installing /Users/mgreau/git/mgreau/maven-zero-cves/patcher/target/patcher-maven-plugin-0.1.0-SNAPSHOT.jar to /Users/mgreau/.m2/repository/com/mgreau/patcher-maven-plugin/0.1.0-SNAPSHOT/patcher-maven-plugin-0.1.0-SNAPSHOT.jar
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time:  51.542 s
[INFO] Finished at: 2024-11-24T22:09:05-05:00
[INFO] ------------------------------------------------------------------------
```

## Patching a project - jsoup example

> [!NOTE] 
> On this case, let's imagine the 1.15.3 version has not been released yet, but the patch is available on GitHub.

Patch the jsoup project to eliminate [CVE-2022-36033](https://github.com/jhy/jsoup/security/advisories/GHSA-gp7f-rwcx-9369) from the 1.15.2 version:

The plugin does the following:
- download the `jsoup-1.15.2` tag from GitHub
- apply the patch
- build the project and run the tests
- install the patched version in your local Maven repository

### Patch the library

```bash
cd patch-projects
mvn clean patcher:apply-patch -f patch-jsoup.xml

[INFO] Installing /Users/mgreau/git/mgreau/maven-zero-cves/patch-projects/target/source-project/target/jsoup-1.15.3-r0-javadoc.jar to /Users/mgreau/.m2/repository/org/jsoup/jsoup/1.15.3-r0/jsoup-1.15.3-r0-javadoc.jar
[INFO] Installing /Users/mgreau/git/mgreau/maven-zero-cves/patch-projects/target/source-project/target/jsoup-1.15.3-r0-sources.jar to /Users/mgreau/.m2/repository/org/jsoup/jsoup/1.15.3-r0/jsoup-1.15.3-r0-sources.jar
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time:  02:01 min
[INFO] Finished at: 2024-11-24T22:25:25-05:00
[INFO] ------------------------------------------------------------------------
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time:  02:13 min
[INFO] Finished at: 2024-11-24T22:25:25-05:00
[INFO] ------------------------------------------------------------------------
```

### Check the patched version against CVE-2022-36033

```bash
grype /Users/mgreau/.m2/repository/org/jsoup/jsoup/1.15.2/jsoup-1.15.2.jar
 ✔ Vulnerability DB                [no update available]
 ✔ Indexed file system               /Users/mgreau/.m2/repository/org/jsoup/jsoup/1.15.2
 ✔ Cataloged contents              2014a59b8985a33479eed29265b308194125773fa33dd84f2f74e
   ├── ✔ Packages                        [1 packages]
   └── ✔ Executables                     [0 executables]
 ✔ Scanned for vulnerabilities     [0 vulnerability matches]
   ├── by severity: 0 critical, 0 high, 1 medium, 0 low, 0 negligible
   └── by status:   1 fixed, 0 not-fixed, 0 ignored
NAME   INSTALLED  FIXED-IN  TYPE          VULNERABILITY        SEVERITY
jsoup  1.15.2     1.15.3    java-archive  GHSA-gp7f-rwcx-9369  Medium
```

```bash
grype /Users/mgreau/.m2/repository/org/jsoup/jsoup/1.15.3-r0/jsoup-1.15.3-r0.jar
 ✔ Vulnerability DB                [no update available]
 ✔ Indexed file system              /Users/mgreau/.m2/repository/org/jsoup/jsoup/1.15.3-
 ✔ Cataloged contents              f4a6a1fff2807dd7beb8c399f9a5a0b769dfd95560956c76234b6
   ├── ✔ Packages                        [1 packages]
   └── ✔ Executables                     [0 executables]
 ✔ Scanned for vulnerabilities     [0 vulnerability matches]
   ├── by severity: 0 critical, 0 high, 0 medium, 0 low, 0 negligible
   └── by status:   0 fixed, 0 not-fixed, 0 ignored
No vulnerabilities found
```

## Next steps

- sign the final artifacts with Sigstore using https://github.com/sigstore/sigstore-java/tree/main/sigstore-maven-plugin

