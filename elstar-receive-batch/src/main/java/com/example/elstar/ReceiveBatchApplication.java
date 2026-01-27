package com.example.elstar;

import com.example.elstar.config.BatchConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

@SpringBootApplication
@Import(BatchConfiguration.class)
public class ReceiveBatchApplication {

    public static void main(String[] args) {
        SpringApplication.run(ReceiveBatchApplication.class, args);
    }
}
