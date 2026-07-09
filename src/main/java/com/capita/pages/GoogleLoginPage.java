package com.capita.pages;

import com.capita.utils.Log;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.By;
import org.openqa.selenium.ElementClickInterceptedException;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.List;
import java.util.Set;

public class GoogleLoginPage {

    private static final Logger log = Log.getLogger(GoogleLoginPage.class);

    private final WebDriver driver;
    private final WebDriverWait wait;

    private final By consentButton = By.xpath(
            "//button[translate(normalize-space(.), " +
                    "'abcdefghijklmnopqrstuvwxyz','ABCDEFGHIJKLMNOPQRSTUVWXYZ')='CONSENT']"
    );

    private final By accountIcon = By.xpath(
            "//div[contains(@class,'zw-icon-my-account') and contains(@class,'c-p')]"
    );

    private final By googleLoginOption = By.xpath(
            "//button[contains(., 'Google')] | //div[contains(., 'Google') and contains(@class,'c-p')] | //a[contains(., 'Google')]"
    );

    private final By googleEmailInput = By.id("identifierId");
    private final By googleNextButton = By.id("identifierNext");

    private final By googleErrorMessage = By.xpath(
            "//*[contains(text(),\"Couldn't sign you in\") or " +
                    "contains(text(),\"Couldn't find your Google Account\") or " +
                    "contains(text(),'may not be secure')]"
    );

    public GoogleLoginPage(WebDriver driver) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(15));
    }

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

    /** Clicks an element normally, falling back to a JS click if something is overlapping it
     *  (e.g. Google's Funding Choices ad-consent overlay, which appears intermittently). */
    private void resilientClick(WebElement element, String description) {
        try {
            element.click();
            log.info("Clicked {}", description);
        } catch (ElementClickInterceptedException e) {
            log.warn("Click on {} intercepted by an overlay, falling back to JS click", description);
            ((JavascriptExecutor) driver).executeScript("arguments[0].click();", element);
            log.info("Clicked {} via JavaScript", description);
        }
    }

    public void openLoginModal() {

        dismissConsentIfPresent();

        WebElement icon;
        try {
            icon = wait.until(ExpectedConditions.visibilityOfElementLocated(accountIcon));
        } catch (TimeoutException e) {
            log.error("=== DIAGNOSTIC: accountIcon locator did not match ===");
            List<WebElement> candidates = driver.findElements(
                    By.xpath("//header//*[self::img or self::button or self::a or self::div][position()<=20]")
            );
            log.error("Found {} candidate header elements:", candidates.size());
            for (WebElement el : candidates) {
                log.error(" - tag={} class={} alt={} text={}",
                        el.getTagName(), el.getAttribute("class"), el.getAttribute("alt"), el.getText());
            }
            throw e;
        }

        resilientClick(icon, "account icon");
        log.info("Login modal opened");
    }

    public void clickGoogleLogin() {
        Set<String> windowsBefore = driver.getWindowHandles();

        WebElement googleOption;
        try {
            googleOption = wait.until(ExpectedConditions.visibilityOfElementLocated(googleLoginOption));
        } catch (TimeoutException e) {
            log.error("=== DIAGNOSTIC: googleLoginOption did not match ===");
            List<WebElement> candidates = driver.findElements(By.xpath("//*[contains(., 'Google')]"));
            log.error("Found {} elements containing 'Google':", candidates.size());
            for (WebElement el : candidates) {
                log.error(" - tag={} class={} text={}", el.getTagName(), el.getAttribute("class"), el.getText());
            }
            throw e;
        }

        resilientClick(googleOption, "Google login option");

        wait.until(d -> d.getWindowHandles().size() > windowsBefore.size());

        Set<String> windowsAfter = driver.getWindowHandles();
        windowsAfter.removeAll(windowsBefore);

        if (windowsAfter.isEmpty()) {
            throw new IllegalStateException("Google login did not open a new window");
        }

        String newWindow = windowsAfter.iterator().next();
        driver.switchTo().window(newWindow);
        log.info("Switched to Google login window: {}", newWindow);
    }

    public String attemptInvalidLogin(String fakeEmail) {

        WebElement emailInput = wait.until(ExpectedConditions.visibilityOfElementLocated(googleEmailInput));
        emailInput.sendKeys(fakeEmail);
        log.info("Entered invalid email: {}", fakeEmail);

        WebElement nextButton = wait.until(ExpectedConditions.elementToBeClickable(googleNextButton));
        nextButton.click();
        log.info("Clicked Next");

        try {
            WebElement error = wait.until(ExpectedConditions.visibilityOfElementLocated(googleErrorMessage));
            String errorText = error.getText().trim();
            log.info("Captured Google error message: {}", errorText);
            return errorText;
        } catch (TimeoutException e) {
            log.error("No error message appeared. Current URL: {}, Title: {}",
                    driver.getCurrentUrl(), driver.getTitle());
            throw e;
        }
    }

    public void closeAndReturnToOriginal(String originalWindowHandle) {
        driver.close();
        driver.switchTo().window(originalWindowHandle);
        log.info("Closed Google window, returned to original window");
    }
}