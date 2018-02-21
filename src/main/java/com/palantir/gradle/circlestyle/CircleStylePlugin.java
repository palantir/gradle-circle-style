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

import static com.palantir.gradle.circlestyle.CircleStyleFinalizer.registerFinalizer;

import java.io.File;

import org.gradle.api.Action;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.plugins.quality.Checkstyle;
import org.gradle.api.plugins.quality.FindBugs;

public class CircleStylePlugin implements Plugin<Project> {

    @Override
    public void apply(Project rootProject) {
        final String circleReportsDir = System.getenv("CIRCLE_TEST_REPORTS");
        if (circleReportsDir == null) {
            return;
        }

        configureBuildFailureFinalizer(rootProject, circleReportsDir);

        final StyleTaskTimer timer = new StyleTaskTimer();
        rootProject.getGradle().addListener(timer);

        rootProject.allprojects(new Action<Project>() {
            @Override
            public void execute(final Project project) {
                project.getTasks().withType(Checkstyle.class, new Action<Checkstyle>() {
                    @Override
                    public void execute(Checkstyle checkstyleTask) {
                        registerFinalizer(
                                project,
                                checkstyleTask,
                                new File(circleReportsDir, "checkstyle"),
                                timer,
                                new CheckstyleReportHandler());
                    }
                });
                project.getTasks().withType(FindBugs.class, new Action<FindBugs>() {
                    @Override
                    public void execute(FindBugs findbugsTask) {
                        registerFinalizer(
                                project,
                                findbugsTask,
                                new File(circleReportsDir, "findbugs"),
                                timer,
                                new FindBugsReportHandler());
                    }
                });
            }
        });
    }

    private static void configureBuildFailureFinalizer(Project rootProject, String circleReportsDir) {
        int attemptNumber = 1;
        File targetFile = new File(new File(circleReportsDir, "gradle"), "build.xml");
        while (targetFile.exists()) {
            targetFile = new File(new File(circleReportsDir, "gradle"), "build" + (++attemptNumber) + ".xml");
        }
        Integer container;
        try {
            container = Integer.parseInt(System.getenv("CIRCLE_NODE_INDEX"));
        } catch (NumberFormatException e) {
            container = null;
        }
        CircleBuildFailureListener listener = new CircleBuildFailureListener();
        CircleBuildFinishedAction action = new CircleBuildFinishedAction(container, targetFile, listener);
        rootProject.getGradle().addListener(listener);
        rootProject.getGradle().buildFinished(action);
    }
}
