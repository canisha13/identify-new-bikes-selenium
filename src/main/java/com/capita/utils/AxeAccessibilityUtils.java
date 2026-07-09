package com.capita.utils;

import com.deque.html.axecore.results.Results;
import com.deque.html.axecore.selenium.AxeBuilder;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.WebDriver;

import java.util.Arrays;

public final class AxeAccessibilityUtils {

    private static final Logger log = Log.getLogger(AxeAccessibilityUtils.class);

    private static final String[] WCAG_TAGS = {
            "wcag2a",
            "wcag2aa",
            "wcag21a",
            "wcag21aa",
            "wcag22a",
            "wcag22aa"
    };

    private AxeAccessibilityUtils() {
    }

    public static Results scanEntirePage(WebDriver driver) {

        log.info("Running axe-core accessibility scan on: {}", driver.getCurrentUrl());

        return new AxeBuilder()
                .withTags(Arrays.asList(WCAG_TAGS))
                .analyze(driver);
    }

    public static Results scanIncludedArea(WebDriver driver, String cssSelector) {

        log.info("Running axe-core accessibility scan on selector '{}' at: {}",
                cssSelector, driver.getCurrentUrl());

        return new AxeBuilder()
                .include(cssSelector)
                .withTags(Arrays.asList(WCAG_TAGS))
                .analyze(driver);
    }

    public static Results scanExcludingArea(WebDriver driver, String cssSelector) {

        return new AxeBuilder()
                .exclude(cssSelector)
                .withTags(Arrays.asList(WCAG_TAGS))
                .analyze(driver);
    }

    public static boolean hasNoViolations(Results results) {

        String report =
                AccessibilityReportFormatter.formatViolations(results.getViolations());

        if (!results.violationFree()) {
            log.warn(report);
        } else {
            log.info("No accessibility violations found.");
        }

        return results.violationFree();
    }
}