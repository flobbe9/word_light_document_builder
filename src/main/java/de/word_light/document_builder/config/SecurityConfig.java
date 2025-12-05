package de.word_light.document_builder.config;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import jakarta.annotation.PostConstruct;
import lombok.extern.log4j.Log4j2;


/**
 * Configuration class to authentiacate requests.<p>
 * 
 * @since 0.0.1
 */
@Configuration
@EnableWebSecurity
@Log4j2
public class SecurityConfig {
    
    @Value("${FRONTEND_BASE_URL}")
    private String FRONTEND_BASE_URL;

    @Value("${MAPPING}")
    private String MAPPING;

    @Value("${ENV}")
    private String ENV;


    @PostConstruct
    void init() {

        log.info("Configuring api security...");
    }

    
    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        
        // enable csrf in prod only
        if (!this.ENV.equalsIgnoreCase("prod"))
            http.csrf(csrf -> csrf.disable());
        
        // routes
        http.authorizeHttpRequests(request -> request
                .anyRequest()
                .permitAll())
            .cors(cors -> cors
                .configurationSource(corsConfig()));

        return http.build();
    }


    /**
     * Configure cors.
     * 
     * @return the configured {@link CorsConfigurationSource}
     */
    private CorsConfigurationSource corsConfig() {

        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of(this.FRONTEND_BASE_URL));
        configuration.setAllowedMethods(List.of("GET", "POST", "UPDATE", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/" + this.MAPPING + "/**", configuration);

        return source;
    }
}