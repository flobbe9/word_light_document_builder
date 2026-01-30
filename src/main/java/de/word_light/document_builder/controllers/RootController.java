package de.word_light.document_builder.controllers;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;


/**
 * Class handling http requests for root path '/'.
 * 
 * @since 0.0.6
 */
@RequestMapping("/")
@RestController
@Tag(name = "Root endpoints")
public class RootController {

    @Value("${custom.version}")
    private String API_VERSION;
    
    
    @GetMapping("/version")
    @Operation(summary = "View api version.")
    public String getVersion() {
        return this.API_VERSION;
    }
}