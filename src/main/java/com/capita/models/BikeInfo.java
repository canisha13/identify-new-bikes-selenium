package com.capita.models;

public class BikeInfo {

    private final String name;
    private final String priceText;
    private final double priceInLakh;
    private final String expectedLaunch;

    public BikeInfo(String name, String priceText, double priceInLakh, String expectedLaunch) {
        this.name = name;
        this.priceText = priceText;
        this.priceInLakh = priceInLakh;
        this.expectedLaunch = expectedLaunch;
    }

    public String getName() { return name; }
    public String getPriceText() { return priceText; }
    public double getPriceInLakh() { return priceInLakh; }
    public String getExpectedLaunch() { return expectedLaunch; }

    @Override
    public String toString() {
        return name + " | " + priceText + " | Launch: " + expectedLaunch;
    }
}