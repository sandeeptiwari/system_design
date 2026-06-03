package com.rotatingmind.trafficcontroller.api;

import com.rotatingmind.trafficcontroller.config.AppConfig;
import com.rotatingmind.trafficcontroller.domain.Direction;
import com.rotatingmind.trafficcontroller.domain.Intersection;

import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Base controller — shared intersection state, direction order, and lifecycle hooks.
 */
public abstract class TrafficController {

    protected final AppConfig appConfig;
    protected final Intersection intersection;
    protected final List<Direction> directionOrder;

    protected volatile boolean running = true;
    protected ScheduledExecutorService scheduler;
    protected ScheduledFuture<?> currentPhase;

    protected TrafficController(AppConfig appConfig) {
        this.appConfig = appConfig;
        this.directionOrder = appConfig.getController().getDirectionOrder();
        this.intersection = new Intersection(appConfig, directionOrder);
    }

    public abstract void start();

    /** Gracefully stop the cycle — cancels any pending phase transition. */
    public void stop() {
        running = false;
        appConfig.setLightStatus(false);
        if (currentPhase != null) {
            currentPhase.cancel(false);
        }
        if (scheduler != null) {
            scheduler.shutdownNow();
        }
    }

    /**
     * Manual override — pauses auto scheduling and grants GREEN to the requested direction.
     * Subclasses that run an auto loop should override to cancel/reschedule their timer.
     */
    public void manualOverride(Direction direction) {
        if (currentPhase != null) {
            currentPhase.cancel(false);
        }
        intersection.manualOverride(direction);
    }

    protected void schedule(Runnable task, long delayMs) {
        currentPhase = scheduler.schedule(task, delayMs, TimeUnit.MILLISECONDS);
    }
}
