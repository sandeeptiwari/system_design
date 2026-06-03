package com.rotatingmind.trafficcontroller.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import java.io.File;

public class ConfigLoader {
    public static AppConfig load(String file) throws Exception {

        ObjectMapper mapper =  new ObjectMapper(new YAMLFactory());

        return mapper.readValue(
                new File(file),
                AppConfig.class
        );
    }
}
