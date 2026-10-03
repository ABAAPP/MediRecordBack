package com.api.medirecord;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class MedirecordApplication {

	public static void main(String[] args) {
		SpringApplication.run(MedirecordApplication.class, args);
	}

}