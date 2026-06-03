package com.rotatingmind.trafficcontroller.service;

import com.rotatingmind.trafficcontroller.config.AppConfig;
import com.rotatingmind.trafficcontroller.domain.Direction;
import com.rotatingmind.trafficcontroller.domain.State;
import com.rotatingmind.trafficcontroller.domain.TrafficLight;

public class GreenState implements SignalState {

    private final AppConfig appConfig;
    private final SignalStateFactory factory;

    public GreenState(AppConfig appConfig) {
        this.appConfig = appConfig;
        this.factory = SignalStateFactory.forConfig(appConfig);
    }

    @Override
    public State name() {
        return State.GREEN;
    }

    @Override
    public long duration(Direction direction) {
        // STEP: resolve per-direction override, fall back to global default
        return appConfig.getDurationFor(direction).getGreen();
    }

    @Override
    public SignalState next() {
        return factory.yellow();
    }

    @Override
    public void apply(TrafficLight light) {
        light.transitionTo(this);
    }
}
