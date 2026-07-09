package com.capita.tests;

import com.capita.base.BaseTest;
import com.capita.config.ConfigReader;
import com.capita.driver.DriverFactory;
import com.capita.listeners.TestListener;
import com.capita.models.BikeInfo;
import com.capita.pages.NewBikesPage;
import com.capita.utils.Log;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.WebDriver;
import org.testng.Assert;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import java.util.List;

@Listeners(TestListener.class)
public class UpcomingHondaBikesTest extends BaseTest {

    private static final Logger log = Log.getLogger(UpcomingHondaBikesTest.class);

    @Test
    public void upcomingHondaBikesUnder4LakhTest() {

        log.info("=== TEST: upcomingHondaBikesUnder4LakhTest START ===");

        double priceLimit = Double.parseDouble(ConfigReader.getProperty("hondaBikePriceLimitLakh"));
        String url = ConfigReader.getProperty("upcomingHondaBikesUrl");

        WebDriver driver = DriverFactory.getDriver();
        NewBikesPage newBikesPage = new NewBikesPage(driver);

        newBikesPage.open(url);

        List<BikeInfo> allBikes = newBikesPage.getAllUpcomingHondaBikes();
        Assert.assertFalse(allBikes.isEmpty(), "Expected at least one upcoming Honda bike to be listed");

        List<BikeInfo> underLimit = newBikesPage.getUpcomingHondaBikesUnderPrice(priceLimit);

        log.info("Upcoming Honda bikes under {} Lakh:", priceLimit);
        for (BikeInfo bike : underLimit) {
            log.info(" - {}", bike);
        }

        Assert.assertFalse(underLimit.isEmpty(),
                "Expected at least one upcoming Honda bike under " + priceLimit + " Lakh, but found none");

        for (BikeInfo bike : underLimit) {
            Assert.assertTrue(bike.getPriceInLakh() < priceLimit,
                    "Bike " + bike.getName() + " price " + bike.getPriceInLakh() + " is not under " + priceLimit + " Lakh");
        }

        log.info("=== TEST: upcomingHondaBikesUnder4LakhTest PASSED with {} bikes ===", underLimit.size());
    }
}