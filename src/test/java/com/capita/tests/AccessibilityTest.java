package com.capita.tests;

import com.capita.base.BaseTest;
import com.capita.config.ConfigReader;
import com.capita.driver.DriverFactory;
import com.capita.listeners.TestListener;
import com.capita.utils.Log;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.testng.Assert;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import java.util.List;

/**
 * Basic accessibility checks against the live zigwheels.com site.
 * <p>
 * Buttons missing accessible text are treated as a hard, zero-tolerance
 * failure - a screen reader user cannot use an unlabeled button at all.
 * <p>
 * Images missing alt text use a tolerance threshold instead, since a small,
 * pre-existing number (e.g. decorative spacer images) is common on real
 * commercial sites and shouldn't permanently block the build.
 */
@Listeners(TestListener.class)
public class AccessibilityTest extends BaseTest {

    private static final Logger logger = Log.getLogger(AccessibilityTest.class);

    private static final double MAX_BUTTON_VIOLATION_RATE = 0.0;   // zero tolerance
    private static final double MAX_IMAGE_VIOLATION_RATE = 0.25;   // 25% tolerated

    @Test
    public void pageShouldHaveATitle() {

        WebDriver driver = DriverFactory.getDriver();
        driver.get(ConfigReader.getProperty("baseUrl"));

        logger.info("Page Title: {}", driver.getTitle());

        Assert.assertFalse(
                driver.getTitle().trim().isEmpty(),
                "Page title should not be empty."
        );
    }

    @Test
    public void imagesShouldHaveAltText() {

        WebDriver driver = DriverFactory.getDriver();
        driver.get(ConfigReader.getProperty("baseUrl"));

        List<WebElement> images = driver.findElements(By.tagName("img"));
        Assert.assertTrue(images.size() > 0, "No images found on the page.");

        int missingAlt = 0;

        for (WebElement image : images) {
            String alt = image.getAttribute("alt");
            if (alt == null || alt.trim().isEmpty()) {
                missingAlt++;
                logger.warn("Image missing alt text: {}", image.getAttribute("src"));
            }
        }

        double violationRate = (double) missingAlt / images.size();

        logger.info("Images found: {}", images.size());
        logger.info("Images missing alt text: {} ({}%)", missingAlt, Math.round(violationRate * 100));

        Assert.assertTrue(
                violationRate <= MAX_IMAGE_VIOLATION_RATE,
                String.format(
                        "%.0f%% of images are missing alt text (%d of %d), exceeding the %.0f%% threshold.",
                        violationRate * 100, missingAlt, images.size(), MAX_IMAGE_VIOLATION_RATE * 100
                )
        );
    }

    @Test
    public void buttonsShouldHaveAccessibleText() {

        WebDriver driver = DriverFactory.getDriver();
        driver.get(ConfigReader.getProperty("baseUrl"));

        List<WebElement> buttons = driver.findElements(
                By.xpath("//button | //input[@type='button'] | //input[@type='submit'] | //*[@role='button']")
        );
        Assert.assertTrue(buttons.size() > 0, "No buttons found on the page.");

        int inaccessibleButtons = 0;

        for (WebElement button : buttons) {
            String text = button.getText().trim();
            String ariaLabel = button.getAttribute("aria-label");

            if (text.isEmpty() && (ariaLabel == null || ariaLabel.trim().isEmpty())) {
                inaccessibleButtons++;
                logger.warn("Button missing accessible text.");
            }
        }

        double violationRate = (double) inaccessibleButtons / buttons.size();

        logger.info("Buttons found: {}", buttons.size());
        logger.info("Buttons missing accessible text: {} ({}%)", inaccessibleButtons, Math.round(violationRate * 100));

        Assert.assertTrue(
                violationRate <= MAX_BUTTON_VIOLATION_RATE,
                String.format(
                        "%.0f%% of buttons lack accessible text (%d of %d), exceeding the %.0f%% threshold.",
                        violationRate * 100, inaccessibleButtons, buttons.size(), MAX_BUTTON_VIOLATION_RATE * 100
                )
        );
    }

    @Test
    public void keyboardNavigationShouldWork() {

        WebDriver driver = DriverFactory.getDriver();
        driver.get(ConfigReader.getProperty("baseUrl"));

        WebElement body = driver.findElement(By.tagName("body"));
        body.sendKeys(Keys.TAB);

        WebElement focused = driver.switchTo().activeElement();

        logger.info("Focused element: {}", focused.getTagName());

        Assert.assertNotNull(focused, "No element received keyboard focus.");
        Assert.assertTrue(focused.isDisplayed(), "Focused element is not visible.");
    }
}