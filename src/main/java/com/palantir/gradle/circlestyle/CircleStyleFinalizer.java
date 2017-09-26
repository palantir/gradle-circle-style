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

import static com.palantir.gradle.circlestyle.JUnitReportCreator.createReport;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

import javax.inject.Inject;
import javax.xml.transform.TransformerException;

import org.gradle.api.DefaultTask;
import org.gradle.api.Task;
import org.gradle.api.reporting.ReportContainer;
import org.gradle.api.reporting.Reporting;
import org.gradle.api.reporting.SingleFileReport;
import org.gradle.api.tasks.TaskAction;
import org.w3c.dom.Document;

class CircleStyleFinalizer extends DefaultTask {

    private ReportParser reportParser;
    private Task styleTask;
    private Reporting<? extends ReportContainer<SingleFileReport>> reporting;
    private StyleTaskTimer styleTaskTimer;
    private File targetFile;

    @Inject
    public CircleStyleFinalizer() { }

    public ReportParser getReportParser() {
        return reportParser;
    }

    public void setReportParser(ReportParser reportParser) {
        this.reportParser = reportParser;
    }

    public Task getStyleTask() {
        return styleTask;
    }

    public void setStyleTask(Task styleTask) {
        this.styleTask = styleTask;
    }

    public Reporting<? extends ReportContainer<SingleFileReport>> getReporting() {
        return reporting;
    }

    public void setReporting(Reporting<? extends ReportContainer<SingleFileReport>> reporting) {
        this.reporting = reporting;
    }

    public StyleTaskTimer getStyleTaskTimer() {
        return styleTaskTimer;
    }

    public void setStyleTaskTimer(StyleTaskTimer styleTaskTimer) {
        this.styleTaskTimer = styleTaskTimer;
    }

    public File getTargetFile() {
        return targetFile;
    }

    public void setTargetFile(File targetFile) {
        this.targetFile = targetFile;
    }

    @TaskAction
    public void createCircleReport() throws IOException, TransformerException {
        if (!styleTask.getDidWork()) {
            setDidWork(false);
            return;
        }

        File rootDir = getProject().getRootProject().getProjectDir();
        String projectName = getProject().getName();
        File sourceReport = reporting.getReports().findByName("xml").getDestination();

        List<Failure> failures = reportParser.loadFailures(new FileInputStream(sourceReport));
        long taskTimeNanos = styleTaskTimer.getTaskTimeNanos(styleTask);
        Document report = createReport(rootDir, projectName, styleTask.getName(), taskTimeNanos, failures);
        targetFile.getParentFile().mkdirs();
        try (FileWriter writer = new FileWriter(targetFile)) {
            XmlUtils.write(writer, report);
        }
    }
}
