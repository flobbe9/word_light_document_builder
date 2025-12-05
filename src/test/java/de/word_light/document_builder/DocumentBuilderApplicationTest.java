package de.word_light.document_builder;

import java.io.IOException;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import de.word_light.document_builder.utils.Utils;
import lombok.extern.log4j.Log4j2;

@SpringBootTest
@Log4j2
class DocumentBuilderApplicationTest {

    @BeforeAll
    static void init() throws IOException {
        log.info("Running tests in CI mode: {}", Utils.isCI());
        
        if (Utils.isCI()) {
            // use h2 db in pipeline for simplicity
            System.setProperty("spring.datasource.url", "jdbc:h2:mem:cidb");
        }
    }

	@Test
	void contextLoads() {
	}

}
