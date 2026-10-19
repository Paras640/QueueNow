package com.queuenow;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Basic integration test to ensure that the Spring application context loads properly.
 *
 * <p>Annotated with {@link SpringBootTest} to bootstrap the full application context
 * and verify that all beans, configurations, and services are wired correctly without errors.</p>

*/
@SpringBootTest
class QueuenowApplicationTests {

	/**
	 * Verifies that the Spring application context starts up and initializes without exceptions.
	 */
	@Test
	void contextLoads() {
		// Context loading smoke test: passes if ApplicationContext starts successfully
	}

}


