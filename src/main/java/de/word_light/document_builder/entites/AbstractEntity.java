package de.word_light.document_builder.entites;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonIgnore;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;


/**
 * Abstract class that all classes annotated as {@code @Entity} should extend.
 * 
 * @since 0.0.1
 */
@MappedSuperclass
@Getter
@Setter
public abstract class AbstractEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Schema(hidden = true)
    private Long id;

    @JsonIgnore
    private LocalDateTime created;

    @JsonIgnore
    private LocalDateTime updated;


    public AbstractEntity() {

        if (this.created == null)
            this.created = LocalDateTime.now();

        this.updated = LocalDateTime.now();
    }
}