package com.capita.tests;

import com.capita.base.BaseTest;
import com.capita.config.ConfigReader;
import com.capita.driver.DriverFactory;
import com.capita.listeners.TestListener;
import com.capita.pages.HomePage;
import com.capita.utils.Log;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.Assert;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import java.time.Duration;

@Listeners(TestListener.class)
public class SearchBikeTest extends BaseTest {

    private static final Logger log = Log.getLogger(SearchBikeTest.class);

    @Test
    public void searchForBikeTest() {

        log.info("=== TEST: searchForBikeTest START ===");

        String bikeName = ConfigReader.getProperty("searchBikeName");

        WebDriver driver = DriverFactory.getDriver();
        HomePage homePage = new HomePage(driver);

        homePage.searchForBike(bikeName);

        log.info("Waiting for URL to contain 'q={}'", bikeName);
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        wait.until(ExpectedConditions.urlContains("q=" + bikeName));

        String currentUrl = driver.getCurrentUrl();
        log.info("Current URL after search: {}", currentUrl);

        Assert.assertTrue(
                currentUrl.contains("q=" + bikeName),
                "Expected URL to contain 'q=" + bikeName + "' after search, but was: " + currentUrl
        );

        log.info("=== TEST: searchForBikeTest PASSED ===");
    }
}