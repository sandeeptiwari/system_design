package com.rotatingmind.trafficcontroller.api;

import com.rotatingmind.trafficcontroller.config.AppConfig;
import com.rotatingmind.trafficcontroller.domain.Direction;
import com.rotatingmind.trafficcontroller.service.GreenState;
import com.rotatingmind.trafficcontroller.service.RedState;
import com.rotatingmind.trafficcontroller.service.SignalState;
import com.rotatingmind.trafficcontroller.service.YellowState;

import com.rotatingmind.trafficcontroller.service.SignalStateFactory;

import java.util.concurrent.Executors;

/**
 * Automatic round-robin controller.
 *
 * Core cycle for each direction:
 *   GREEN (wait) → YELLOW (wait) → RED (wait) → advance to next direction
 *
 * All transition logic is delegated to {@link Intersection} + State pattern classes;
 * this controller only orchestrates timing and index advancement.
 */
public class RoundRobinController extends TrafficController {

    private int currentIndex;

    public RoundRobinController(AppConfig appConfig) {
        super(appConfig);
        this.currentIndex = 0;
    }

    @Override
    public void start() {
        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "traffic-signal-cycle");
            t.setDaemon(true);
            return t;
        });

        // STEP 1: Kick off the cycle with the first direction in the configured order.
        runCycleForDirection(directionOrder.get(currentIndex));
    }

    @Override
    public void manualOverride(Direction direction) {
        // STEP 2: Cancel the pending scheduled phase, apply override, then resume cycle.
        super.manualOverride(direction);
        currentIndex = directionOrder.indexOf(direction);
        if (currentIndex < 0) {
            currentIndex = 0;
        }
        // STEP 3: Reschedule — after override the operator may want to hold GREEN;
        //         for now we restart the full cycle from the overridden direction.
        schedule(() -> runCycleForDirection(direction), 0);
    }

    /**
     * Runs a complete GREEN → YELLOW → RED phase for one direction, then schedules the next.
     */
    private void runCycleForDirection(Direction direction) {
        if (!running || !appConfig.isLightStatus()) {
            return;
        }

        SignalStateFactory factory = SignalStateFactory.forConfig(appConfig);

        // --- PHASE A: GREEN ---
        GreenState green = factory.green();
        intersection.activateGreen(direction);
        logPhase(direction, green);
        schedule(() -> runYellowPhase(direction), green.duration(direction));
    }

    private void runYellowPhase(Direction direction) {
        if (!running || !appConfig.isLightStatus()) {
            return;
        }

        // --- PHASE B: YELLOW ---
        YellowState yellow = SignalStateFactory.forConfig(appConfig).yellow();
        intersection.activateYellow(direction);
        logPhase(direction, yellow);
        schedule(() -> runRedPhase(direction), yellow.duration(direction));
    }

    private void runRedPhase(Direction direction) {
        if (!running || !appConfig.isLightStatus()) {
            return;
        }

        // --- PHASE C: RED (clearance before handing off) ---
        RedState red = SignalStateFactory.forConfig(appConfig).red();
        intersection.activateRed(direction);
        logPhase(direction, red);
        schedule(() -> advanceToNextDirection(direction), red.duration(direction));
    }

    /** STEP 4: Move round-robin index forward and start the next direction's cycle. */
    private void advanceToNextDirection(Direction completedDirection) {
        if (!running || !appConfig.isLightStatus()) {
            return;
        }

        currentIndex = (directionOrder.indexOf(completedDirection) + 1) % directionOrder.size();
        Direction next = directionOrder.get(currentIndex);
        runCycleForDirection(next);
    }

    private void logPhase(Direction direction, SignalState state) {
        System.out.printf("[Intersection] %s → %s (%d ms)%n",
                direction, state.name(), state.duration(direction));
    }
}
