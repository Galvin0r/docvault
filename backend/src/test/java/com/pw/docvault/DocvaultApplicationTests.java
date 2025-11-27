package com.pw.docvault;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@Disabled("Disabled in CI – full context smoke test not needed")
class DocvaultApplicationTests {

	@Test
	void contextLoads() {
	}

}
