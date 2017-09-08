/*
 * Copyright 2017 Palantir Technologies, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.palantir.gradle.circlestyle;

import static com.google.common.base.Charsets.UTF_8;
import static com.palantir.gradle.circlestyle.TestCommon.copyTestFile;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;

import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.EnvironmentVariables;
import org.junit.rules.TemporaryFolder;

import com.google.common.base.Joiner;
import com.google.common.io.Files;

public class CircleStylePluginTests {

    @Rule public final EnvironmentVariables env = new EnvironmentVariables();
    @Rule public final TemporaryFolder projectDir = new TemporaryFolder();

    @Test
    public void checkstyleIntegrationTest() throws IOException {
        File reportsDir = new File(projectDir.getRoot(), "circle/reports");
        env.set("CIRCLE_TEST_REPORTS", reportsDir.toString());
        env.set("TEST_CLASSPATH", pluginClasspath());

        copyTestFile("build.gradle", projectDir, "build.gradle");
        copyTestFile("checkstyle.xml", projectDir, "config/checkstyle/checkstyle.xml");
        copyTestFile("checkstyle-violating-class", projectDir, "src/main/java/com/example/MyClass.java");

        BuildResult result = GradleRunner.create()
                .withProjectDir(projectDir.getRoot())
                .withArguments("--stacktrace", "checkstyleMain")
                .buildAndFail();
        assertThat(result.getOutput()).contains("Checkstyle rule violations were found");

        File report = new File(reportsDir, "checkstyle/checkstyleMain.xml");
        assertThat(report).exists();
        String reportXml = Files.asCharSource(report, UTF_8).read();
        assertThat(reportXml).contains("Name 'a_constant' must match pattern");
        System.out.println(reportXml);
    }

    private static String pluginClasspath() {
        URLClassLoader classloader = (URLClassLoader) ClassLoader.getSystemClassLoader();
        List<String> classpath = new ArrayList<>();
        for (URL url : classloader.getURLs()) {
            classpath.add(url.getFile());
        }
        return Joiner.on(':').join(classpath);
    }
}
