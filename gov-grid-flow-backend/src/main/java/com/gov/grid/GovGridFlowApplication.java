package com.gov.grid;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@MapperScan("com.gov.grid.mapper")
@EnableScheduling
public class GovGridFlowApplication {

    public static void main(String[] args) {
        SpringApplication.run(GovGridFlowApplication.class, args);
    }
}
