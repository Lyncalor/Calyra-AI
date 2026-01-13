package com.lyncalor.calyra;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class CalyraApplication {

    public static void main(String[] args) {
        SpringApplication.run(CalyraApplication.class, args);
    }

}
