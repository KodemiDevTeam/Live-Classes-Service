package com.example.live_classes_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients

public class LiveClassesDemoApplication {

	public static void main(String[] args) {
		SpringApplication.run(LiveClassesDemoApplication.class, args);
	}

}
