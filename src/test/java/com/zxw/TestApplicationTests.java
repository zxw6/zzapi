package com.zxw;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
		"spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration",
		"spring.main.lazy-initialization=true"
})
class TestApplicationTests {

	@Test
	void contextLoads() {
	}

}
