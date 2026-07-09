package com.capita.driver;

import com.capita.config.ConfigReader;
import org.openqa.selenium.Capabilities;
import org.openqa.selenium.Point;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.edge.EdgeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.remote.RemoteWebDriver;

import java.net.MalformedURLException;
import java.net.URL;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class DriverFactory {

    private static final ThreadLocal<WebDriver> DRIVER = new ThreadLocal<>();
    private static final AtomicInteger WINDOW_COUNTER = new AtomicInteger(0);
    private static final int CASCADE_OFFSET = 60;

    public static WebDriver initDriver() {
        return initDriverInternal(ConfigReader.getProperty("browser"));
    }

    public static WebDriver initDriver(String browserOverride) {
        String browser = (browserOverride != null && !browserOverride.isBlank())
                ? browserOverride
                : ConfigReader.getProperty("browser");
        return initDriverInternal(browser);
    }

    private static WebDriver initDriverInternal(String browser) {

        boolean headless = Boolean.parseBoolean(ConfigReader.getProperty("headless"));
        boolean useGrid = Boolean.parseBoolean(ConfigReader.getProperty("useGrid"));

        WebDriver driver;

        if (browser.equalsIgnoreCase("chrome")) {

            ChromeOptions options = new ChromeOptions();

            if (headless) {
                options.addArguments("--headless=new");
            }

            Map<String, Object> prefs = new HashMap<>();
            prefs.put("profile.default_content_setting_values.notifications", 2);
            options.setExperimentalOption("prefs", prefs);

            options.addArguments(
                    "--disable-notifications",
                    "--no-sandbox",
                    "--disable-dev-shm-usage",
                    "--window-size=1920,1080"
            );

            driver = useGrid ? createRemoteDriver(options) : new ChromeDriver(options);

        } else if (browser.equalsIgnoreCase("edge")) {

            EdgeOptions options = new EdgeOptions();

            if (headless) {
                options.addArguments("--headless=new");
            }

            options.addArguments(
                    "--disable-notifications",
                    "--window-size=1920,1080"
            );

            driver = useGrid ? createRemoteDriver(options) : new EdgeDriver(options);

        } else if (browser.equalsIgnoreCase("firefox")) {

            FirefoxOptions options = new FirefoxOptions();

            if (headless) {
                options.addArguments("-headless");
            }

            options.addPreference("dom.webnotifications.enabled", false);

            driver = useGrid ? createRemoteDriver(options) : new FirefoxDriver(options);

        } else {
            throw new RuntimeException("Browser not supported: " + browser);
        }

        driver.manage().timeouts().implicitlyWait(Duration.ZERO);

        if (!useGrid && !headless) {
            cascadeWindow(driver);
        }

        DRIVER.set(driver);
        return driver;
    }

    private static WebDriver createRemoteDriver(Capabilities options) {
        try {
            String gridUrl = ConfigReader.getProperty("gridUrl");
            return new RemoteWebDriver(new URL(gridUrl), options);
        } catch (MalformedURLException e) {
            throw new RuntimeException("Invalid Grid URL in config.properties", e);
        }
    }

    /**
     * Offsets each new window's position slightly so multiple parallel browsers
     * are visually distinguishable on screen, without shrinking their size -
     * shrinking width risks crossing the site's responsive breakpoint and
     * switching it to mobile layout, which breaks desktop-only locators.
     * Only relevant for local, non-Grid runs.
     */
    private static void cascadeWindow(WebDriver driver) {
        int index = WINDOW_COUNTER.getAndIncrement() % 6;
        int offset = index * CASCADE_OFFSET;
        driver.manage().window().setPosition(new Point(offset, offset));
    }

    public static WebDriver getDriver() {
        return DRIVER.get();
    }

    public static void quitDriver() {
        WebDriver driver = DRIVER.get();
        if (driver != null) {
            driver.quit();
            DRIVER.remove();
        }
    }
}