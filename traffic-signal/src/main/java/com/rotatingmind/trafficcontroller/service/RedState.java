package com.rotatingmind.trafficcontroller.service;

import com.rotatingmind.trafficcontroller.config.AppConfig;
import com.rotatingmind.trafficcontroller.domain.Direction;
import com.rotatingmind.trafficcontroller.domain.State;
import com.rotatingmind.trafficcontroller.domain.TrafficLight;

public class RedState implements SignalState {

    private final AppConfig appConfig;
    private final SignalStateFactory factory;

    public RedState(AppConfig appConfig) {
        this.appConfig = appConfig;
        this.factory = SignalStateFactory.forConfig(appConfig);
    }

    @Override
    public State name() {
        return State.RED;
    }

    @Override
    public long duration(Direction direction) {
        // FIX: was incorrectly returning getGreen()
        return appConfig.getDurationFor(direction).getRed();
    }

    @Override
    public SignalState next() {
        return factory.green();
    }

    @Override
    public void apply(TrafficLight light) {
        light.transitionTo(this);
    }
}
