package com.capita.base;

import com.capita.config.ConfigReader;
import com.capita.driver.DriverFactory;
import io.qameta.allure.Allure;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.testng.ITestResult;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;

import java.io.ByteArrayInputStream;

public class BaseTest {

    private static final int MAX_NAVIGATION_ATTEMPTS = 3;
    private static final long RETRY_DELAY_MS = 2000;

    @Parameters("browser")
    @BeforeMethod
    public void setUp(@Optional String browser) {
        DriverFactory.initDriver(browser);
        navigateToBaseUrlWithRetry();
    }

    private void navigateToBaseUrlWithRetry() {
        String baseUrl = ConfigReader.getProperty("baseUrl");
        WebDriver driver = DriverFactory.getDriver();

        int attempt = 0;
        while (true) {
            attempt++;
            try {
                driver.get(baseUrl);
                return; // success
            } catch (WebDriverException e) {
                if (attempt >= MAX_NAVIGATION_ATTEMPTS) {
                    throw e; // out of retries, let it fail for real
                }
                try {
                    Thread.sleep(RETRY_DELAY_MS);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    @AfterMethod(alwaysRun = true)
    public void tearDown(ITestResult result) {

        if (result.getStatus() == ITestResult.FAILURE) {
            WebDriver driver = DriverFactory.getDriver();
            if (driver != null) {
                try {
                    byte[] screenshot = ((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES);
                    Allure.addAttachment(
                            "Failure Screenshot",
                            "image/png",
                            new ByteArrayInputStream(screenshot),
                            ".png"
                    );
                } catch (Exception ignored) {
                    // Screenshot attachment is best-effort; don't fail teardown over it.
                }
            }
        }

        DriverFactory.quitDriver();
    }
}