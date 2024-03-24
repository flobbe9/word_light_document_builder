package de.word_light.document_builder.abstracts;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonIgnore;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
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


    @PrePersist
    @PreUpdate
    void update() {

        if (this.created == null)
            this.created = LocalDateTime.now();

        this.updated = LocalDateTime.now();
    }
}