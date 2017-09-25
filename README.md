Gradle Circle Style
===================

A plugin for Gradle that integrates [Checkstyle] and [FindBugs] output into Circle.

[Checkstyle]: https://docs.gradle.org/current/userguide/checkstyle_plugin.html
[FindBugs]: https://docs.gradle.org/current/userguide/findbugs_plugin.html

Quickstart
----------

Add the following to your project's top-level build.gradle file:

```gradle

plugins {
  id 'com.palantir.circle.style' version '0.1'
}
```

And now your Circle builds will fail with nice summaries:

![CHECKSTYLE — 1 FAILURE](images/checkstyle-circle-failure.png?raw=true "Circle failure image")
