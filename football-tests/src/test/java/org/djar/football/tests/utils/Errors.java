package org.djar.football.tests.utils;

import org.junit.rules.TestWatcher;
import org.junit.runner.Description;

public class Errors extends TestWatcher {

    private static int failedTests;
    private static int succeededTests;

    @Override
    protected void failed(Throwable e, Description description) {
        failedTests++;
    }

    @Override
    protected void succeeded(Description description) {
        succeededTests++;
    }

    public static int count() {
        int failed = failedTests;
        failedTests = 0;
        return failed;
    }
}
