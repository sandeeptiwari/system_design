package com.rotatingmind.trafficcontroller.api;

import com.rotatingmind.trafficcontroller.config.AppConfig;
import com.rotatingmind.trafficcontroller.config.ControllerConfig;
import com.rotatingmind.trafficcontroller.config.DurationConfig;
import com.rotatingmind.trafficcontroller.config.SignalConfig;
import com.rotatingmind.trafficcontroller.domain.Direction;
import com.rotatingmind.trafficcontroller.domain.State;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestTrafficController {

    private AppConfig appConfig;
    private RoundRobinController controller;

    @BeforeEach
    public void setup() {
        DurationConfig duration = new DurationConfig();
        duration.setGreen(100);
        duration.setYellow(50);
        duration.setRed(50);

        SignalConfig signal = new SignalConfig();
        signal.setDuration(duration);

        ControllerConfig controllerConfig = new ControllerConfig();
        controllerConfig.setAlgorithm("round-robin");

        appConfig = new AppConfig();
        appConfig.setSignal(signal);
        appConfig.setController(controllerConfig);
        appConfig.setLightStatus(true);

        controller = new RoundRobinController(appConfig);
    }

    @Test
    @DisplayName("Manual override grants GREEN to requested direction")
    public void manualOverride_setsRequestedDirectionGreen() {
        controller.start();
        controller.manualOverride(Direction.EAST);

        assertEquals(State.GREEN, controller.intersection.getLight(Direction.EAST).getState().name());
        assertEquals(State.RED, controller.intersection.getLight(Direction.NORTH).getState().name());
        assertEquals(State.RED, controller.intersection.getLight(Direction.SOUTH).getState().name());
        assertEquals(State.RED, controller.intersection.getLight(Direction.WEST).getState().name());

        controller.stop();
    }

    @Test
    @DisplayName("Per-direction duration override is resolved from config")
    public void perDirectionDuration_usesOverrideWhenPresent() {
        DurationConfig northOverride = new DurationConfig();
        northOverride.setGreen(999);
        northOverride.setYellow(50);
        northOverride.setRed(50);
        appConfig.getSignal().getPerDirection().put("NORTH", northOverride);

        assertEquals(999, appConfig.getDurationFor(Direction.NORTH).getGreen());
        assertEquals(100, appConfig.getDurationFor(Direction.EAST).getGreen());
    }
}
