package com.hyperq.analyzer;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = "com.hyperq")

public class AnalyzerApplication {

	private static final Logger logger = LoggerFactory.getLogger(AnalyzerApplication.class);
	
	public static void main(String[] args) {
		logger.info("the program is starting.");
		SpringApplication.run(AnalyzerApplication.class, args);
	}

}
