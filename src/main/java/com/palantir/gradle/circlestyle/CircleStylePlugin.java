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
import org.gradle.api.tasks.TaskContainer;

public class CircleStylePlugin implements Plugin<Project> {

    @Override
    public void apply(Project rootProject) {
        final String circleReportsDir = System.getenv("CIRCLE_TEST_REPORTS");
        if (circleReportsDir == null) {
            return;
        }

        final StyleTaskTimer timer = new StyleTaskTimer();
        rootProject.getGradle().addListener(timer);

        rootProject.allprojects(new Action<Project>() {
            @Override
            public void execute(final Project project) {
                project.getTasks().withType(Checkstyle.class, new Action<Checkstyle>() {
                    @Override
                    public void execute(Checkstyle checkstyleTask) {
                        CircleCheckstyleFinalizer finalizer = createTask(
                                project.getTasks(),
                                checkstyleTask.getName() + "CircleFinalizer",
                                CircleCheckstyleFinalizer.class);
                        if (finalizer == null) {
                            // Already registered (happens if the user applies us to the root project and subprojects)
                            return;
                        }
                        finalizer.setCheckstyleTask(checkstyleTask);
                        finalizer.setStyleTaskTimer(timer);
                        finalizer.setTargetFile(new File(
                                new File(circleReportsDir, "checkstyle"),
                                checkstyleTask.getName() + ".xml"));

                        checkstyleTask.finalizedBy(finalizer);
                    }
                });
            }
        });
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
