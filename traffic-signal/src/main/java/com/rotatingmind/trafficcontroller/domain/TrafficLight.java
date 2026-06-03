package com.rotatingmind.trafficcontroller.domain;

import com.rotatingmind.trafficcontroller.service.SignalState;

public class TrafficLight {

    private SignalState state;
    private final Direction direction;

    public TrafficLight(SignalState state, Direction direction) {
        this.state = state;
        this.direction = direction;
    }

    /** Replace current state — used by Intersection after safety checks. */
    public void transitionTo(SignalState newState) {
        this.state = newState;
        // STEP: notify observers / log state change here if needed
    }

    /** Delegate to the State pattern — light asks its current state to apply itself. */
    public void applyCurrentState() {
        state.apply(this);
    }

    public SignalState getState() {
        return state;
    }

    public Direction getDirection() {
        return direction;
    }
}
