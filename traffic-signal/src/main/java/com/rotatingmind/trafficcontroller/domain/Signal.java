package com.rotatingmind.trafficcontroller.domain;


public class Signal {
    private Direction direction;
    private State state;


    public Direction getDirection() {
        return direction;
    }

    public void setDirection(Direction direction) {
        this.direction = direction;
    }

    public State getState() {
        return state;
    }

    public void setState(State state) {
        this.state = state;
    }
}
