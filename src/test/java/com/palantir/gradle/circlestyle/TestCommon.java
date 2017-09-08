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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;
import java.util.List;

import org.gradle.internal.impldep.com.google.common.collect.ImmutableList;
import org.junit.rules.TemporaryFolder;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;

import me.nallar.whocalled.WhoCalled;

public class TestCommon {

    public static final File ROOT = new File("/home/ubuntu/fooproject");
    public static final File CLASSFILE = new File(
            ROOT, "fooproject/src/main/java/org/example/server/FooApplication.java");
    public static final List<Failure> CHECKSTYLE_FAILURES = ImmutableList.of(
            new Failure.Builder()
                    .source("com.puppycrawl.tools.checkstyle.checks.naming.ParameterNameCheck")
                    .severity("ERROR")
                    .file(CLASSFILE)
                    .line(135)
                    .message("Parameter name 'b' must match pattern '^[a-z][a-zA-Z0-9][a-zA-Z0-9]*$'.")
                    .build(),
            new Failure.Builder()
                    .source("com.puppycrawl.tools.checkstyle.checks.naming.ParameterNameCheck")
                    .severity("ERROR")
                    .file(CLASSFILE)
                    .line(181)
                    .message("Parameter name 'c' must match pattern '^[a-z][a-zA-Z0-9][a-zA-Z0-9]*$'.")
                    .build());
    public static final long FAILED_CHECKSTYLE_TIME_NANOS = 321_000_000_000L;

    public static URL testFile(String filename) {
        return WhoCalled.$.getCallingClass().getResource(filename);
    }

    public static File copyTestFile(String source, TemporaryFolder root, String target) {
        File targetFile = new File(root.getRoot(), target);
        targetFile.getParentFile().mkdirs();
        try (OutputStream stream = new FileOutputStream(targetFile)) {
            Resources.copy(WhoCalled.$.getCallingClass().getResource(source), stream);
            return targetFile;
        } catch (IOException e) {
            throw new AssertionError(e);
        }
    }

    public static String readTestFile(String filename) {
        try {
            return Resources.toString(WhoCalled.$.getCallingClass().getResource(filename), Charsets.UTF_8);
        } catch (IOException e) {
            throw new AssertionError(e);
        }
    }

    private TestCommon() { }
}
