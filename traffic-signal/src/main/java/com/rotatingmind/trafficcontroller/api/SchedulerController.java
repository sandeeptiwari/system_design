package com.rotatingmind.trafficcontroller.api;

import com.rotatingmind.trafficcontroller.config.AppConfig;

/**
 * Placeholder for a time-of-day / traffic-density scheduler.
 *
 * TODO: implement scheduled phase plans (rush-hour vs off-peak durations).
 */
public class SchedulerController extends TrafficController {

    public SchedulerController(AppConfig appConfig) {
        super(appConfig);
    }

    @Override
    public void start() {
        // STEP 1: Load schedule profile from config (peak hours, weekend, etc.)
        // STEP 2: Delegate to RoundRobinController with profile-specific durations
        throw new UnsupportedOperationException("SchedulerController not yet implemented");
    }
}
