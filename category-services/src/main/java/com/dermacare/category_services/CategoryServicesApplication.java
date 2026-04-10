package com.dermacare.category_services;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class CategoryServicesApplication {

	public static void main(String[] args) {
		SpringApplication.run(CategoryServicesApplication.class, args);
	}

}
