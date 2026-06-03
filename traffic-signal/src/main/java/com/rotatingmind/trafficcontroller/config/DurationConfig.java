package com.rotatingmind.trafficcontroller.config;

public class DurationConfig {

    // All values are in milliseconds
    private long green;
    private long yellow;
    private long red;

    public long getGreen() {
        return green;
    }

    public void setGreen(long green) {
        this.green = green;
    }

    public long getYellow() {
        return yellow;
    }

    public void setYellow(long yellow) {
        this.yellow = yellow;
    }

    public long getRed() {
        return red;
    }

    public void setRed(long red) {
        this.red = red;
    }
}
