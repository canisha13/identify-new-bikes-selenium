package com.capita.tests;

import com.capita.base.BaseTest;
import com.capita.driver.DriverFactory;
import com.capita.listeners.TestListener;
import com.capita.pages.GoogleLoginPage;
import com.capita.utils.Log;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.WebDriver;
import org.testng.Assert;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

@Listeners(TestListener.class)
public class GoogleLoginTest extends BaseTest {

    private static final Logger log = Log.getLogger(GoogleLoginTest.class);

    @Test
    public void invalidGoogleLoginShowsError() {

        WebDriver driver = DriverFactory.getDriver();
        String originalWindow = driver.getWindowHandle();

        GoogleLoginPage loginPage = new GoogleLoginPage(driver);

        loginPage.openLoginModal();
        loginPage.clickGoogleLogin();

        String errorMessage = loginPage.attemptInvalidLogin("thisIsNotARealAccount12345xyz@gmail.com");

        boolean isExpectedError =
                errorMessage.toLowerCase().contains("couldn't sign you in") ||
                        errorMessage.toLowerCase().contains("find your google account") ||
                        errorMessage.toLowerCase().contains("may not be secure");

        Assert.assertTrue(isExpectedError,
                "Expected a login-rejection error message, but got: " + errorMessage);

        loginPage.closeAndReturnToOriginal(originalWindow);

        log.info("Test complete. Captured error: {}", errorMessage);
    }
}