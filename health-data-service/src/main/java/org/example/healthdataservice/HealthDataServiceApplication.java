package org.example.healthdataservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication(scanBasePackages = {
        "org.example.healthdataservice",
        "org.example.web"
})
@EnableDiscoveryClient
public class HealthDataServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(HealthDataServiceApplication.class, args);
    }

}
