package com.rotatingmind.trafficcontroller;

import com.rotatingmind.trafficcontroller.api.ControllerFactory;
import com.rotatingmind.trafficcontroller.api.TrafficController;
import com.rotatingmind.trafficcontroller.config.AppConfig;
import com.rotatingmind.trafficcontroller.config.ConfigLoader;

public class Main {

    static void main() {
        try {
            AppConfig config =
                    ConfigLoader.load("/Users/vibhatiwari/dev/system_design/traffic-signal/src/main/resources/application.yml");

            TrafficController controller = ControllerFactory.create(config);

            controller.start();

            Thread.currentThread().join();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
