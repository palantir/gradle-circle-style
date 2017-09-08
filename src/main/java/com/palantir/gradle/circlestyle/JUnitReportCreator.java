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
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

class JUnitReportCreator {

    private static final Pattern JAVA_FILE_RX = Pattern.compile(".*src/\\w+/java/(.*)\\.java");

    public static Document createReport(
            File rootDir,
            String projectName,
            String taskName,
            long elapsedTimeNanos,
            List<Failure> failures) {
        try {
            Document report = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
            String elapsedTimeString = String.format("%.03f", elapsedTimeNanos / 1e9);

            Element testsuites = report.createElement("testsuites");
            report.appendChild(testsuites);
            testsuites.setAttribute("id", projectName);
            testsuites.setAttribute("name", projectName);
            testsuites.setAttribute("tests", Integer.toString(failures.size()));
            testsuites.setAttribute("failures", Integer.toString(failures.size()));
            testsuites.setAttribute("time", elapsedTimeString);

            Element testsuite = report.createElement("testsuite");
            testsuites.appendChild(testsuite);
            testsuite.setAttribute("id", taskName);
            testsuite.setAttribute("name", taskName);
            testsuite.setAttribute("tests", Integer.toString(failures.size()));
            testsuite.setAttribute("failures", Integer.toString(failures.size()));
            testsuite.setAttribute("time", elapsedTimeString);

            for (Failure failure : failures) {
                String shortSource = failure.source().replaceAll(".*\\.", "");
                String className = getClassName(failure.file());

                Element testcase = report.createElement("testcase");
                testsuite.appendChild(testcase);
                testcase.setAttribute("id", shortSource + "." + className);
                testcase.setAttribute("name", shortSource + " - " + className);

                Element failureElement = report.createElement("failure");
                testcase.appendChild(failureElement);
                failureElement.setAttribute(
                        "message", failure.file().getName() + ":" + failure.line() + ": " + failure.message());
                failureElement.setAttribute("type", failure.severity());
                failureElement.setTextContent(
                        failure.severity() + ": " + failure.message() + "\n"
                                + "Category: " + failure.source() + "\n"
                                + "File: " + rootDir.toPath().relativize(failure.file().toPath()) + "\n"
                                + "Line: " + failure.line() + "\n");
            }

            return report;
        } catch (ParserConfigurationException e) {
            throw new RuntimeException(e);
        }
    }

    private static String getClassName(File file) {
        Matcher matcher = JAVA_FILE_RX.matcher(file.toString());
        if (matcher.matches()) {
            return matcher.group(1).replace('/', '.');
        }
        return file.toString();
    }

    private JUnitReportCreator() { }
}
