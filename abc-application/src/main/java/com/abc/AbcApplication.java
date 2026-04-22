package com.abc;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.abc.**.mapper")
public class AbcApplication {
    public static void main(String[] args) {
        SpringApplication.run(AbcApplication.class, args);
    }
}
