package com.gov.grid;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.gov.grid.mapper")
public class GovGridFlowApplication {

    public static void main(String[] args) {
        SpringApplication.run(GovGridFlowApplication.class, args);
    }
}
