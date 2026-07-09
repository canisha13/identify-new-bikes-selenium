package com.capita.pages;

import com.capita.models.BikeInfo;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NewBikesPage {

    private static final Logger log = Log.getLogger(NewBikesPage.class);

    private final WebDriver driver;
    private final WebDriverWait wait;

    private final By consentButton = By.xpath(
            "//button[translate(normalize-space(.), " +
                    "'abcdefghijklmnopqrstuvwxyz', 'ABCDEFGHIJKLMNOPQRSTUVWXYZ')='CONSENT']"
    );

    private final By bikeNameLinks = By.xpath(
            "//a[contains(@href,'/honda-bikes/') and not(@href='/honda-bikes/') " +
                    "and not(contains(@href,'/honda-bikes/service-centers/')) " +
                    "and not(contains(@href,'/honda-bikes/dealers/'))]"
    );

    public NewBikesPage(WebDriver driver) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(15));
    }

    private void dismissConsentIfPresent() {
        try {
            WebDriverWait shortWait = new WebDriverWait(driver, Duration.ofSeconds(8));

            try {
                WebElement consent = shortWait.until(
                        ExpectedConditions.elementToBeClickable(consentButton)
                );
                consent.click();
                log.info("Consent popup dismissed (main page)");
                return;
            } catch (TimeoutException e) {
                log.debug("No consent popup on main page, checking iframes");
            }

            List<WebElement> frames = driver.findElements(By.tagName("iframe"));

            for (WebElement frame : frames) {
                try {
                    driver.switchTo().frame(frame);

                    List<WebElement> consentButtons = driver.findElements(consentButton);
                    if (!consentButtons.isEmpty() && consentButtons.get(0).isDisplayed()) {
                        consentButtons.get(0).click();
                        log.info("Consent popup dismissed (inside iframe)");
                        driver.switchTo().defaultContent();
                        return;
                    }

                    driver.switchTo().defaultContent();

                } catch (Exception e) {
                    driver.switchTo().defaultContent();
                }
            }

        } catch (Exception e) {
            driver.switchTo().defaultContent();
        }
    }

    public void open(String url) {
        log.info("Navigating to Upcoming Honda Bikes page: {}", url);
        driver.navigate().to(url);
        dismissConsentIfPresent();
        wait.until(ExpectedConditions.visibilityOfElementLocated(bikeNameLinks));
        dismissConsentIfPresent();
    }

    private List<WebElement> getBikeNameLinks() {
        wait.until(ExpectedConditions.visibilityOfElementLocated(bikeNameLinks));
        return driver.findElements(bikeNameLinks);
    }

    private WebElement findCardContainer(WebElement nameLink) {
        WebElement current = nameLink;

        for (int i = 0; i < 6; i++) {
            try {
                WebElement parent = current.findElement(By.xpath(".."));
                if (parent.getText().contains("Expected Launch")) {
                    return parent;
                }
                current = parent;
            } catch (Exception e) {
                break;
            }
        }
        return current;
    }

    public List<BikeInfo> getAllUpcomingHondaBikes() {
        List<WebElement> nameLinks = getBikeNameLinks();
        log.info("Found {} candidate bike name links", nameLinks.size());

        List<BikeInfo> bikes = new ArrayList<>();

        for (WebElement nameLink : nameLinks) {
            String name = nameLink.getText().trim();
            if (name.isEmpty()) {
                continue;
            }

            WebElement card = findCardContainer(nameLink);
            String fullText = card.getText();

            if (!fullText.contains("Expected Launch")) {
                continue;
            }

            String priceText = extractLineContaining(fullText, "Rs.", "Price To Be Announced");
            String launchText = extractLineContaining(fullText, "Expected Launch");
            double priceInLakh = parsePriceToLakh(priceText);

            BikeInfo bike = new BikeInfo(name, priceText, priceInLakh, launchText);
            bikes.add(bike);
            log.debug("Parsed bike: {}", bike);
        }

        log.info("Parsed {} valid upcoming Honda bikes", bikes.size());
        return bikes;
    }

    public List<BikeInfo> getUpcomingHondaBikesUnderPrice(double maxPriceInLakh) {
        List<BikeInfo> filtered = new ArrayList<>();
        for (BikeInfo bike : getAllUpcomingHondaBikes()) {
            if (bike.getPriceInLakh() > 0 && bike.getPriceInLakh() < maxPriceInLakh) {
                filtered.add(bike);
            }
        }
        log.info("{} bikes found under Rs. {} Lakh", filtered.size(), maxPriceInLakh);
        return filtered;
    }

    private String extractLineContaining(String text, String... keywords) {
        for (String line : text.split("\n")) {
            for (String keyword : keywords) {
                if (line.contains(keyword)) {
                    return line.trim();
                }
            }
        }
        return "";
    }

    private double parsePriceToLakh(String priceText) {
        if (priceText == null || priceText.isBlank()
                || priceText.toLowerCase().contains("to be announced")) {
            return -1;
        }

        Pattern lakhPattern = Pattern.compile("Rs\\.?\\s*([\\d,.]+)\\s*Lakh", Pattern.CASE_INSENSITIVE);
        Matcher lakhMatcher = lakhPattern.matcher(priceText);
        if (lakhMatcher.find()) {
            return Double.parseDouble(lakhMatcher.group(1).replace(",", ""));
        }

        Pattern crorePattern = Pattern.compile("Rs\\.?\\s*([\\d,.]+)\\s*Crore", Pattern.CASE_INSENSITIVE);
        Matcher croreMatcher = crorePattern.matcher(priceText);
        if (croreMatcher.find()) {
            return Double.parseDouble(croreMatcher.group(1).replace(",", "")) * 100;
        }

        Pattern rupeePattern = Pattern.compile("Rs\\.?\\s*([\\d,]+)");
        Matcher rupeeMatcher = rupeePattern.matcher(priceText);
        if (rupeeMatcher.find()) {
            double rupees = Double.parseDouble(rupeeMatcher.group(1).replace(",", ""));
            return rupees / 100000.0;
        }

        return -1;
    }
}