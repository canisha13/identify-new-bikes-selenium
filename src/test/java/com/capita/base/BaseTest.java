package com.capita.base;

import com.capita.config.ConfigReader;
import com.capita.driver.DriverFactory;
import io.qameta.allure.Allure;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.testng.ITestResult;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;

import java.io.ByteArrayInputStream;

public class BaseTest {

    @Parameters("browser")
    @BeforeMethod
    public void setUp(@Optional String browser) {
        DriverFactory.initDriver(browser);
        DriverFactory.getDriver()
                .get(ConfigReader.getProperty("baseUrl"));
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