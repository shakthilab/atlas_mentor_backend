package com.lab.atlasmentor;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class AtlasmentorApplication {

	public static void main(String[] args) {
		SpringApplication.run(AtlasmentorApplication.class, args);
	}

}

