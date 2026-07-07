package com.nhnacademy.cruxmate;

import org.springframework.boot.SpringApplication;

public class TestCruxmateApplication {

    public static void main(String[] args) {
        SpringApplication.from(CruxmateApplication::main).with(TestcontainersConfiguration.class).run(args);
    }

}
