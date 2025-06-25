package com.miproject.finalwork;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * @author zengyijun
 */

@SpringBootApplication
public class AdminApplication {
    public static void main(String[] args){
        SpringApplication.run(AdminApplication.class, args);
    }
}
