package com.rotatingmind.trafficcontroller.api;

import com.rotatingmind.trafficcontroller.config.AppConfig;
import com.rotatingmind.trafficcontroller.domain.Direction;

/**
 * Manual controller — no automatic cycling.
 * Operator calls {@link #setGreen(Direction)} to grant right-of-way at any time.
 *
 * TODO: wire to a REST endpoint or CLI command for production use.
 */
public class ManualController extends TrafficController {

    public ManualController(AppConfig appConfig) {
        super(appConfig);
    }

    @Override
    public void start() {
        // STEP 1: Initialize intersection — all directions RED, waiting for operator input.
        for (Direction direction : directionOrder) {
            intersection.activateRed(direction);
        }
        System.out.println("[ManualController] Ready — call setGreen(Direction) to override.");
    }

    /**
     * Manual override entry point.
     * STEP 2: Force all conflicting directions RED, grant GREEN to the requested direction.
     * STEP 3: (future) optionally schedule return to auto mode after a timeout.
     */
    public void setGreen(Direction direction) {
        manualOverride(direction);
        System.out.printf("[ManualController] Override — %s is GREEN, all others RED%n", direction);
    }
}
