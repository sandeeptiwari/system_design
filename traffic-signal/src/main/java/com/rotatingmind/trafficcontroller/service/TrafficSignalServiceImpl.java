package com.rotatingmind.trafficcontroller.service;

/**
 * @deprecated Superseded by {@link com.rotatingmind.trafficcontroller.api.RoundRobinController}
 *             + State pattern classes. Kept as a stub to avoid breaking references.
 *
 * TODO: remove once all callers migrate to TrafficController.
 */
@Deprecated
public class TrafficSignalServiceImpl implements TrafficSignalService {

    @Override
    public void startSignal() {
        throw new UnsupportedOperationException(
                "Use ControllerFactory.create(config).start() instead");
    }
}
