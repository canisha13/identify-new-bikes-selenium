package com.capita.pages;

import com.capita.utils.Log;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.By;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public class UsedCarsPage {

    private static final Logger log = Log.getLogger(UsedCarsPage.class);

    private final WebDriver driver;
    private final WebDriverWait wait;

    public UsedCarsPage(WebDriver driver) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(10));
    }

    private final By consentButton = By.xpath(
            "//button[translate(normalize-space(.), " +
                    "'abcdefghijklmnopqrstuvwxyz','ABCDEFGHIJKLMNOPQRSTUVWXYZ')='CONSENT']"
    );

    private final By popularModelsHeading =
            By.xpath("//div[normalize-space(text())='Popular Models']");

    private void dismissConsentIfPresent() {
        try {
            WebElement consent = wait.until(
                    ExpectedConditions.elementToBeClickable(consentButton)
            );
            consent.click();
            log.info("Consent popup dismissed");
        } catch (TimeoutException ignored) {
            log.debug("No consent popup present");
        }
    }

    public void openUsedCarsChennai(String url) {

        log.info("Navigating to Used Cars Chennai page: {}", url);
        driver.navigate().to(url);

        dismissConsentIfPresent();

        try {
            wait.until(ExpectedConditions.visibilityOfElementLocated(popularModelsHeading));
        } catch (TimeoutException e) {
            log.error("popularModelsHeading did not match. Page title: {}, Current URL: {}",
                    driver.getTitle(), driver.getCurrentUrl());
            throw e;
        }

        dismissConsentIfPresent();
    }

    public List<String> getPopularModels() {

        List<String> models = new ArrayList<>();

        WebElement heading = driver.findElement(popularModelsHeading);
        WebElement container = heading.findElement(By.xpath(".."));

        List<WebElement> items = container.findElements(By.xpath(".//label"));

        if (items.isEmpty()) {
            items = container.findElements(By.xpath(".//li"));
        }

        if (items.isEmpty()) {
            items = heading.findElements(By.xpath("following-sibling::*[1]//label"));
        }

        if (items.isEmpty()) {
            items = heading.findElements(By.xpath("following-sibling::*[1]//li"));
        }

        if (items.isEmpty()) {
            log.warn("No model items found near 'Popular Models' heading. Container tag: {}, text: {}",
                    container.getTagName(), container.getText());
        }

        for (WebElement item : items) {
            String name = item.getText().trim();
            if (!name.isEmpty() && !name.equals("Popular Models")) {
                models.add(name);
            }
        }

        log.info("Extracted {} popular used car models", models.size());
        return models;
    }
}