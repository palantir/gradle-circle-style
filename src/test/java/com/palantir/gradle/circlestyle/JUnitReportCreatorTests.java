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
import static com.palantir.gradle.circlestyle.TestCommon.CHECKSTYLE_FAILURES;
import static com.palantir.gradle.circlestyle.TestCommon.FAILED_CHECKSTYLE_TIME_NANOS;
import static com.palantir.gradle.circlestyle.TestCommon.ROOT;
import static com.palantir.gradle.circlestyle.TestCommon.readTestFile;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.StringWriter;

import javax.xml.transform.TransformerException;

import org.gradle.internal.impldep.com.google.common.collect.ImmutableList;
import org.junit.Test;
import org.w3c.dom.Document;

public class JUnitReportCreatorTests {

    @Test
    public void testNoErrors() throws TransformerException {
        Document junitReport = createReport(
                ROOT, "myproject", "checkstyleMain", 123_000_000_000L, ImmutableList.<Failure>of());
        String xml = XmlUtils.write(new StringWriter(), junitReport).toString();

        assertThat(xml).isEqualTo(readTestFile("empty-checkstyle-report.xml"));
    }

    @Test
    public void testTwoErrors() throws TransformerException {
        Document junitReport = createReport(
                ROOT, "fooproject", "checkstyleTest", FAILED_CHECKSTYLE_TIME_NANOS, CHECKSTYLE_FAILURES);
        String xml = XmlUtils.write(new StringWriter(), junitReport).toString();

        assertThat(xml).isEqualTo(readTestFile("two-namecheck-failures-checkstyle-report.xml"));

    }
}
