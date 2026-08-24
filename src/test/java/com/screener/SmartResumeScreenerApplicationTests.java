package com.screener;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1",
        "llm.groq.api-key=test-key"
})
class SmartResumeScreenerApplicationTests {

    @Test
    void contextLoads() {
        // Verifies the Spring application context starts successfully.
    }
}
