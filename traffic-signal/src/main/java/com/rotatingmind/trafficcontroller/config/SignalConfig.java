package com.rotatingmind.trafficcontroller.config;

import java.util.HashMap;
import java.util.Map;

public class SignalConfig {

    private DurationConfig duration;
    private Map<String, DurationConfig> perDirection = new HashMap<>();

    public DurationConfig getDuration() {
        return duration;
    }

    public void setDuration(DurationConfig duration) {
        this.duration = duration;
    }

    public Map<String, DurationConfig> getPerDirection() {
        return perDirection;
    }

    public void setPerDirection(Map<String, DurationConfig> perDirection) {
        this.perDirection = perDirection;
    }
}
