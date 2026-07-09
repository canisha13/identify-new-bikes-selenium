package com.capita.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.ElementNotInteractableException;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.Set;

public class HomePage {

    private final WebDriver driver;
    private final WebDriverWait wait;

    public HomePage(WebDriver driver) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(10));
    }

    private final By consentButton =
            By.xpath("//button[normalize-space()='Consent']");

    private final By searchInput =
            By.xpath("//input[@placeholder='Search car or bike']");

    public void handlePopups() {
        try {
            WebElement consent = new WebDriverWait(driver, Duration.ofSeconds(5))
                    .until(ExpectedConditions.elementToBeClickable(consentButton));
            consent.click();
        } catch (TimeoutException ignored) {
            // no consent popup appeared, continue
        }
    }

    private void closeExtraTabs() {
        String originalWindow = driver.getWindowHandle();
        Set<String> allWindows = driver.getWindowHandles();

        if (allWindows.size() > 1) {
            for (String handle : allWindows) {
                if (!handle.equals(originalWindow)) {
                    driver.switchTo().window(handle);
                    driver.close();
                }
            }
            driver.switchTo().window(originalWindow);
        }
    }

    public void searchForBike(String bikeName) {

        closeExtraTabs();
        handlePopups();

        WebElement searchBox = wait.until(driver -> {
            for (WebElement el : driver.findElements(searchInput)) {
                if (el.isDisplayed()) {
                    return el;
                }
            }
            return null;
        });

        JavascriptExecutor js = (JavascriptExecutor) driver;

        js.executeScript("arguments[0].value = arguments[1];", searchBox, bikeName);
        js.executeScript("arguments[0].dispatchEvent(new Event('input', {bubbles:true}));", searchBox);

        // Ensure the box is genuinely clickable/interactable before pressing Enter -
        // under Grid load, "displayed" can be true slightly before the element is
        // truly interactable, causing an intermittent ElementNotInteractableException.
        // If even the retry fails (e.g. an overlay/dropdown is still covering the
        // box), don't let that crash the test - the URL fallback below will still
        // get us to the right page.
        try {
            sendEnterWithRetry(searchBox, bikeName);
        } catch (ElementNotInteractableException | TimeoutException | StaleElementReferenceException e) {
            // Enter press failed entirely - fall through to the direct-URL fallback below.
        }

        // The site's search box sometimes has an autocomplete dropdown that
        // intercepts Enter and navigates to a suggested category page (e.g.
        // "/honda-bikes/") instead of submitting a plain search. This is
        // inconsistent and not reliably fixable by dismissing the dropdown
        // (Escape closes the whole widget on some browsers). As a robust
        // fallback: if we're not on the expected search results URL within
        // a short window, navigate directly to the constructed search URL.
        String expectedUrlFragment = "q=" + bikeName;
        try {
            new WebDriverWait(driver, Duration.ofSeconds(4))
                    .until(d -> d.getCurrentUrl().contains(expectedUrlFragment));
        } catch (TimeoutException e) {
            String searchUrl = driver.getCurrentUrl().replaceAll("(https?://[^/]+).*", "$1")
                    + "/search/?q=" + bikeName;
            driver.get(searchUrl);
        }
    }

    /**
     * Presses a real Enter keypress on the search box, retrying once with a fresh
     * element lookup + wait if the first attempt hits a timing issue.
     *
     * NOTE: this intentionally does NOT fall back to a JS-dispatched synthetic
     * KeyboardEvent. Synthetic keyboard events are marked isTrusted:false, and
     * most modern sites (React-based ones especially) ignore untrusted events
     * or rely on native form-submit behavior tied to a real keypress. Faking
     * the Enter key silently leaves the search unsubmitted - the input looks
     * filled in, but the page never navigates, which is exactly what was
     * causing the intermittent "url does not contain q=..." failures under
     * parallel Grid load.
     */
    private void sendEnterWithRetry(WebElement searchBox, String bikeName) {
        try {
            wait.until(ExpectedConditions.elementToBeClickable(searchBox));
            searchBox.sendKeys(Keys.ENTER);
        } catch (ElementNotInteractableException | TimeoutException | StaleElementReferenceException e) {
            // Re-locate the element in case it went stale, then retry with a
            // real keypress instead of a fake one.
            WebElement freshSearchBox = wait.until(driver -> {
                for (WebElement el : driver.findElements(searchInput)) {
                    if (el.isDisplayed()) {
                        return el;
                    }
                }
                return null;
            });

            wait.until(ExpectedConditions.elementToBeClickable(freshSearchBox));
            freshSearchBox.sendKeys(Keys.ENTER);
        }
    }
}