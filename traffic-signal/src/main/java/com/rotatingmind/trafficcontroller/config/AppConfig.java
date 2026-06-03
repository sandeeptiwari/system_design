package com.rotatingmind.trafficcontroller.config;

import com.rotatingmind.trafficcontroller.domain.Direction;

import java.util.EnumMap;
import java.util.Map;

public class AppConfig {

    private SignalConfig signal;
    private ControllerConfig controller;
    private boolean lightStatus = true;

    public SignalConfig getSignal() {
        return signal;
    }

    public void setSignal(SignalConfig signal) {
        this.signal = signal;
    }

    public ControllerConfig getController() {
        return controller;
    }

    public void setController(ControllerConfig controller) {
        this.controller = controller;
    }

    public boolean isLightStatus() {
        return lightStatus;
    }

    public void setLightStatus(boolean lightStatus) {
        this.lightStatus = lightStatus;
    }

    /**
     * STEP: Resolve duration for a direction — per-direction override if present, else global default.
     */
    public DurationConfig getDurationFor(Direction direction) {
        Map<String, DurationConfig> overrides = signal.getPerDirection();
        if (overrides != null) {
            DurationConfig override = overrides.get(direction.name());
            if (override != null) {
                return override;
            }
        }
        return signal.getDuration();
    }
}
