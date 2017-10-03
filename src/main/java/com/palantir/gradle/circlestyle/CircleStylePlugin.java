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

import java.io.File;

import org.gradle.api.Action;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.UnknownTaskException;
import org.gradle.api.plugins.quality.Checkstyle;
import org.gradle.api.plugins.quality.FindBugs;
import org.gradle.api.plugins.quality.FindBugsXmlReport;
import org.gradle.api.reporting.SingleFileReport;
import org.gradle.api.tasks.TaskContainer;

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
                        configureCheckstyleTask(project, checkstyleTask, circleReportsDir, timer);
                    }
                });
                project.getTasks().withType(FindBugs.class, new Action<FindBugs>() {
                    @Override
                    public void execute(FindBugs findbugsTask) {
                        configureFindbugsTask(project, findbugsTask, circleReportsDir, timer);
                    }
                });
            }
        });
    }

    private void configureBuildFailureFinalizer(Project rootProject, String circleReportsDir) {
        int attemptNumber = 1;
        File targetFile = new File(new File(circleReportsDir, "gradle"), "build.xml");
        while (targetFile.exists()) {
            targetFile = new File(new File(circleReportsDir, "gradle"), "build" + attemptNumber + ".xml");
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

    private void configureCheckstyleTask(
            final Project project,
            final Checkstyle checkstyleTask,
            final String circleReportsDir,
            final StyleTaskTimer timer) {
        // Ensure XML output is enabled
        checkstyleTask.doFirst(new Action<Task>() {
            @Override
            public void execute(Task task) {
                checkstyleTask.getReports().findByName("xml").setEnabled(true);
            }
        });

        // Configure the finalizer task
        CircleStyleFinalizer finalizer = createTask(
                project.getTasks(),
                checkstyleTask.getName() + "CircleFinalizer",
                CircleStyleFinalizer.class);
        if (finalizer == null) {
            // Already registered (happens if the user applies us to the root project and subprojects)
            return;
        }
        finalizer.setReportParser(CheckstyleReportHandler.PARSER);
        finalizer.setStyleTask(checkstyleTask);
        finalizer.setReporting(checkstyleTask);
        finalizer.setStyleTaskTimer(timer);
        finalizer.setTargetFile(new File(
                new File(circleReportsDir, "checkstyle"),
                project.getName() + "-" + checkstyleTask.getName() + ".xml"));

        checkstyleTask.finalizedBy(finalizer);
    }

    private void configureFindbugsTask(
            final Project project,
            final FindBugs findbugsTask,
            final String circleReportsDir,
            final StyleTaskTimer timer) {
        // Ensure XML output is enabled
        findbugsTask.doFirst(new Action<Task>() {
            @Override
            public void execute(Task task) {
                for (SingleFileReport report : findbugsTask.getReports()) {
                    report.setEnabled(false);
                }
                FindBugsXmlReport xmlReport = (FindBugsXmlReport) findbugsTask.getReports().findByName("xml");
                xmlReport.setEnabled(true);
                xmlReport.setWithMessages(true);
            }
        });

        // Configure the finalizer task
        CircleStyleFinalizer finalizer = createTask(
                project.getTasks(),
                findbugsTask.getName() + "CircleFinalizer",
                CircleStyleFinalizer.class);
        if (finalizer == null) {
            // Already registered (happens if the user applies us to the root project and subprojects)
            return;
        }
        finalizer.setReportParser(FindBugsReportHandler.PARSER);
        finalizer.setStyleTask(findbugsTask);
        finalizer.setReporting(findbugsTask);
        finalizer.setStyleTaskTimer(timer);
        finalizer.setTargetFile(new File(
                new File(circleReportsDir, "findbugs"),
                project.getName() + "-" + findbugsTask.getName() + ".xml"));

        findbugsTask.finalizedBy(finalizer);
    }

    private static <T extends Task> T createTask(TaskContainer tasks, String preferredName, Class<T> type) {
        String name = preferredName;
        int count = 1;
        while (true) {
            try {
                Task existingTask = tasks.getByName(name);
                if (type.isInstance(existingTask)) {
                    return null;
                }
            } catch (UnknownTaskException e) {
                return tasks.create(name, type);
            }
            count++;
            name = preferredName + count;
        }
    }
}
