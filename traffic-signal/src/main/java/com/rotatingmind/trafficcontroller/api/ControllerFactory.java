package com.rotatingmind.trafficcontroller.api;

import com.rotatingmind.trafficcontroller.config.AppConfig;

public class ControllerFactory {

    public static TrafficController create(AppConfig config) {
        var algorithm = config.getController().getAlgorithm();
        return switch (algorithm.toLowerCase()) {
            case "round-robin" -> new RoundRobinController(config);
            case "scheduler" -> new SchedulerController(config);
            case "manual" -> new ManualController(config);
            default -> throw new IllegalArgumentException("Unknown algorithm: " + algorithm);
        };
    }
}
