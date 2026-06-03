package com.rotatingmind.trafficcontroller.service;

import com.rotatingmind.trafficcontroller.domain.Direction;
import com.rotatingmind.trafficcontroller.domain.State;
import com.rotatingmind.trafficcontroller.domain.TrafficLight;

/**
 * State pattern contract — each concrete state knows:
 *   1. its enum name
 *   2. how long it lasts for a given direction
 *   3. which state follows it
 *   4. how to apply itself to a traffic light
 */
public interface SignalState {

    State name();

    /** Duration in milliseconds for this state at the given direction. */
    long duration(Direction direction);

    /** Returns the next state in the GREEN → YELLOW → RED cycle. */
    SignalState next();

    /** Apply this state to the light (State pattern entry point). */
    void apply(TrafficLight light);
}
