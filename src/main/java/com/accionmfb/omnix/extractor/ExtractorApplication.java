package com.accionmfb.omnix.extractor;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;

@SpringBootApplication
public class ExtractorApplication {

	public static void main(String[] args) {
		SpringApplication.run(ExtractorApplication.class, args);
	}

}
