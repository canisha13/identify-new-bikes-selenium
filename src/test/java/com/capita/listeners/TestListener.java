package com.capita.listeners;

import com.aventstack.extentreports.ExtentReports;
import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.Status;
import com.capita.config.ConfigReader;
import com.capita.driver.DriverFactory;
import com.capita.reporting.ExtentManager;
import com.capita.reporting.ScreenshotUtils;
import io.qameta.allure.Allure;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.testng.ITestContext;
import org.testng.ITestListener;
import org.testng.ITestResult;

import java.io.ByteArrayInputStream;
import java.nio.file.Path;
import java.nio.file.Paths;

public class TestListener implements ITestListener {

    private static final Logger log = LogManager.getLogger(TestListener.class);
    private static final ThreadLocal<ExtentTest> extentTest = new ThreadLocal<>();
    private static ExtentReports extent;

    @Override
    public void onStart(ITestContext context) {
        log.info("Test execution started: {}", context.getName());
        extent = ExtentManager.getInstance();
    }

    @Override
    public void onTestStart(ITestResult result) {
        log.info("Starting test: {}", result.getMethod().getMethodName());
        ExtentTest test = extent.createTest(result.getMethod().getMethodName());
        extentTest.set(test);
    }

    @Override
    public void onTestSuccess(ITestResult result) {
        log.info("Test passed: {}", result.getMethod().getMethodName());
        extentTest.get().log(Status.PASS, "Test passed");
    }

    @Override
    public void onTestFailure(ITestResult result) {
        log.error("Test failed: {}", result.getMethod().getMethodName(), result.getThrowable());

        WebDriver driver = DriverFactory.getDriver();
        if (driver != null) {

            // Extent report: save screenshot to disk, attach via relative path
            String screenshotPath = ScreenshotUtils.capture(driver, result.getMethod().getMethodName());
            try {
                Path reportDir = Paths.get(ConfigReader.getProperty("extentReportDirectory")).toAbsolutePath();
                Path screenshotAbsolute = Paths.get(screenshotPath).toAbsolutePath();
                String relativePath = reportDir.relativize(screenshotAbsolute).toString().replace("\\", "/");

                extentTest.get().fail(result.getThrowable());
                extentTest.get().addScreenCaptureFromPath(relativePath);
            } catch (Exception e) {
                log.error("Could not attach screenshot to Extent report", e);
                extentTest.get().fail(result.getThrowable());
            }

            // Allure report: attach raw screenshot bytes directly
            try {
                byte[] screenshotBytes = ((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES);
                Allure.addAttachment(
                        "Failure Screenshot",
                        "image/png",
                        new ByteArrayInputStream(screenshotBytes),
                        ".png"
                );
                log.info("Screenshot attached to Allure report");
            } catch (Exception e) {
                log.error("Could not attach screenshot to Allure report", e);
            }

        } else {
            extentTest.get().fail(result.getThrowable());
        }
    }

    @Override
    public void onTestSkipped(ITestResult result) {
        log.warn("Test skipped: {}", result.getMethod().getMethodName());
        if (extentTest.get() != null) {
            extentTest.get().log(Status.SKIP, "Test skipped");
        }
    }

    @Override
    public void onFinish(ITestContext context) {
        log.info("Test execution finished: {}", context.getName());
        extent.flush();
    }
}