package com.capita.reporting;

import com.aventstack.extentreports.ExtentReports;
import com.aventstack.extentreports.reporter.ExtentSparkReporter;
import com.capita.config.ConfigReader;

import java.time.format.DateTimeFormatter;

public final class ExtentManager {

    private static ExtentReports extent;

    private ExtentManager() {}

    public static synchronized ExtentReports getInstance() {
        if (extent == null) {
            String reportDir = ConfigReader.getProperty("extentReportDirectory");
            String timestamp = java.time.LocalDateTime.now()
                    .format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));

            String reportPath = reportDir + "/ExtentReport_" + timestamp + ".html";

            ExtentSparkReporter spark = new ExtentSparkReporter(reportPath);
            spark.config().setDocumentTitle("Identify New Bikes - Test Report");
            spark.config().setReportName("Selenium TestNG Execution");

            extent = new ExtentReports();
            extent.attachReporter(spark);
            extent.setSystemInfo("Environment", "QA");
            extent.setSystemInfo("Browser", ConfigReader.getProperty("browser"));
        }
        return extent;
    }
}