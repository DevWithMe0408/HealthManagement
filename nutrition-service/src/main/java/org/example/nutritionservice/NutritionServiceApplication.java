package org.example.nutritionservice;

import org.example.nutritionservice.repository.config.GoalConfigRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.Bean;

@EnableDiscoveryClient
@SpringBootApplication(scanBasePackages = {
        "org.example.nutritionservice",
        "org.example.web"
})
public class NutritionServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(NutritionServiceApplication.class, args);
    }
}
