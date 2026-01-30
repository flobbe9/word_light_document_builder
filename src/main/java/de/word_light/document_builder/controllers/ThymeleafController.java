package de.word_light.document_builder.controllers;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;


/**
 * Class handling endpoints using html pages in '/resources/templates' folder.
 * 
 * @since 0.0.6
 */
@Controller
@Tag(name = "Html template endpoints")
public class ThymeleafController {

    @Value("${WEBSITE_NAME}")
    private String WEBSITE_NAME;

    @Value("${API_NAME}")
    private String API_NAME;

    @Value("${BASE_URL}")
    private String BASE_URL;

    @Value("${DB_VERSION}")
    private String DB_VERSION;
    
    @Value("${custom.version}")
    private String API_VERSION;

    
    @GetMapping("/")
    @Operation(summary = "View basic information about api.")
    public String getIndex(Model model) {

        model.addAttribute("WEBSITE_NAME", this.WEBSITE_NAME);
        model.addAttribute("API_NAME", this.API_NAME);
        model.addAttribute("BASE_URL", this.BASE_URL);
        model.addAttribute("API_VERSION", this.API_VERSION);
        model.addAttribute("DB_VERSION", this.DB_VERSION);

        return "index";
    }
}