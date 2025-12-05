package de.word_light.document_builder.entites.documentParts;

import de.word_light.document_builder.abstracts.AbstractEntity;
import de.word_light.document_builder.entites.documentParts.style.Style;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


/**
 * Represents a simple paragraph in a document with some style information. <p>
 * 
 * Should be extended by any class that holds any kind of text content.
 * 
 * @since 0.0.1
 * @see Style
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BasicParagraph extends AbstractEntity {
    
    @NotNull(message = "'text' cannot be null.")
    private String text;
    
    @NotNull(message = "'style' cannot be null.")
    @Valid
    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn
    private Style style;
}