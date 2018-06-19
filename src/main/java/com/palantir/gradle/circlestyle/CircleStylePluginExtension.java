package com.palantir.gradle.circlestyle;

public class CircleStylePluginExtension {

    private String testReportsEnvVariable = "CIRCLE_TEST_REPORTS";

    public void setTestReportsEnvVariable(String testReportsEnvVariable) {
        this.testReportsEnvVariable = testReportsEnvVariable;
    }

    public String getTestReportsEnvVariable() {
        return testReportsEnvVariable;
    }

}
