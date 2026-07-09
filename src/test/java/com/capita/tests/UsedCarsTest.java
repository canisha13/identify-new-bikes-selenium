package com.capita.tests;

import com.capita.base.BaseTest;
import com.capita.config.ConfigReader;
import com.capita.driver.DriverFactory;
import com.capita.listeners.TestListener;
import com.capita.pages.UsedCarsPage;
import com.capita.utils.Log;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.WebDriver;
import org.testng.Assert;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import java.util.List;

@Listeners(TestListener.class)
public class UsedCarsTest extends BaseTest {

    private static final Logger logger =
            Log.getLogger(UsedCarsTest.class);

    @Test
    public void usedCarsTest() {

        WebDriver driver = DriverFactory.getDriver();

        UsedCarsPage usedCarsPage =
                new UsedCarsPage(driver);

        String url = ConfigReader.getProperty("usedCarsChennaiUrl");
        usedCarsPage.openUsedCarsChennai(url);

        List<String> popularModels =
                usedCarsPage.getPopularModels();

        popularModels.forEach(logger::info);

        Assert.assertFalse(
                popularModels.isEmpty(),
                "Expected popular used car models for Chennai, but none were found."
        );
    }
}