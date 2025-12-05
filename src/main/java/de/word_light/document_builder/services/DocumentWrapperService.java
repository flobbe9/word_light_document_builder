package de.word_light.document_builder.services;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import de.word_light.document_builder.abstracts.AbstractService;
import de.word_light.document_builder.entites.documentParts.DocumentWrapper;
import de.word_light.document_builder.exception.ApiException;
import de.word_light.document_builder.repositories.DocumentWrapperRepository;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;


/**
 * Class handling {@link Document} logic.
 * 
 * @since 0.0.5
 */
@Service
@Validated
// TODO: add tests

public class DocumentWrapperService extends AbstractService<DocumentWrapper, DocumentWrapperRepository> {

    @Autowired
    private DocumentWrapperRepository repository;
    

    public byte[] getPictureByFileName(@NotBlank(message = "'fileName' cannot be blank or null") String fileName, @NotNull(message = "'documentId' cannot be null") Long documentId) {

        DocumentWrapper document = getById(documentId);

        Map<String, byte[]> pictures = document.getPictures();
        if (pictures == null || pictures.isEmpty())
            return null;

        return pictures.entrySet()
                        .stream()
                        .filter(entry -> entry.getKey().equals(fileName))
                        .findAny()
                        .orElseThrow(() -> new ApiException("Failed to get picture by file name: " + fileName))
                        .getValue();
    }
}