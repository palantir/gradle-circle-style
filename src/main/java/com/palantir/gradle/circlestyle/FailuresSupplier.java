package com.palantir.gradle.circlestyle;

import java.io.IOException;
import java.util.List;

interface FailuresSupplier {
    List<Failure> getFailures() throws IOException;
}
