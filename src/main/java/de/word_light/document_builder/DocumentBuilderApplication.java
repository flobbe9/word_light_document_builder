package de.word_light.document_builder;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;

import lombok.extern.log4j.Log4j2;


@SpringBootApplication
@Log4j2
public class DocumentBuilderApplication {

    @Value("${custom.version}")
    private String API_VERSION;

    
	public static void main(String[] args) {
        SpringApplication.run(DocumentBuilderApplication.class, args);
	}

    
    /**
     * Executed after {@code SpringApplication.run()} is completely done. At this point all beans and dependencies are injected / initialized.<p>
     * 
     * NOTE: Annotations like {@code @Value} or {@code @Autowired} work in here
     */
    @EventListener(ApplicationReadyEvent.class)
    public void postStartUp() {
        
        log.info("Finished initializing API version " + API_VERSION + "...");
    }
}