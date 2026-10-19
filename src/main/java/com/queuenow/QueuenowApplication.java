package com.queuenow;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main entry point for the QueueNow application.
 *
 * <p>The {@link SpringBootApplication} annotation is a convenience annotation that enables:
 * <ul>
 *   <li>{@code @EnableAutoConfiguration}: Automatically configures Spring application based on classpath dependencies.</li>
 *   <li>{@code @ComponentScan}: Scans for Spring components, configurations, controllers, and services in this package and sub-packages.</li>
 *   <li>{@code @SpringBootConfiguration}: Marks this class as a configuration source for beans.</li>
 * </ul>
 * </p>
 */
@SpringBootApplication
public class QueuenowApplication {

	/**
	 * Main method used to launch the Spring Boot application.
	 *
	 * @param args command-line arguments passed to the application
	 */
	public static void main(String[] args) {
		// Launches the Spring Boot application, initializing the ApplicationContext and embedded web server
		SpringApplication.run(QueuenowApplication.class, args);
	}

}

