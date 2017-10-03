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

import static com.palantir.gradle.circlestyle.FailuresReportGenerator.failuresReport;
import static com.palantir.gradle.circlestyle.TestCommon.CHECKSTYLE_FAILURES;
import static com.palantir.gradle.circlestyle.TestCommon.FAILED_CHECKSTYLE_TIME_NANOS;
import static com.palantir.gradle.circlestyle.TestCommon.REPORT;
import static com.palantir.gradle.circlestyle.TestCommon.ROOT;
import static org.assertj.core.api.Assertions.assertThat;

import javax.xml.transform.TransformerException;

import org.junit.Test;

import com.google.common.collect.ImmutableList;

public class FailuresReportGeneratorTests {

    @Test
    public void testNoErrors() throws TransformerException {
        Report report = failuresReport(
                ROOT, "fooproject", "checkstyleTest", FAILED_CHECKSTYLE_TIME_NANOS, ImmutableList.<Failure>of());
        assertThat(report).isEqualTo(new Report.Builder()
                .name("fooproject")
                .subname("checkstyleTest")
                .elapsedTimeNanos(FAILED_CHECKSTYLE_TIME_NANOS)
                .build());
    }

    @Test
    public void testTwoErrors() throws TransformerException {
        Report report = failuresReport(
                ROOT, "fooproject", "checkstyleTest", FAILED_CHECKSTYLE_TIME_NANOS, CHECKSTYLE_FAILURES);
        assertThat(report).isEqualTo(REPORT);

    }
}
