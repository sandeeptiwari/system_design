package com.rotatingmind.trafficcontroller.config;

import com.rotatingmind.trafficcontroller.domain.Direction;

import java.util.List;

public class ControllerConfig {

    private String algorithm;
    private List<Direction> directionOrder;

    public String getAlgorithm() {
        return algorithm;
    }

    public void setAlgorithm(String algorithm) {
        this.algorithm = algorithm;
    }

    /**
     * Ordered list of directions for round-robin cycling.
     * Falls back to NORTH → EAST → SOUTH → WEST when not configured.
     */
    public List<Direction> getDirectionOrder() {
        if (directionOrder == null || directionOrder.isEmpty()) {
            return List.of(Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST);
        }
        return directionOrder;
    }

    public void setDirectionOrder(List<Direction> directionOrder) {
        this.directionOrder = directionOrder;
    }
}
