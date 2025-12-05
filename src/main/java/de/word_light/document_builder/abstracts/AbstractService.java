package de.word_light.document_builder.abstracts;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import de.word_light.document_builder.exception.ApiException;
import de.word_light.document_builder.repositories.Dao;


/**
 * Abstract class defining minimum fields all entites must have. <p>
 * 
 * Note that an entity annotated with {@code @Valid} will again be validated when calling {@code repository.save(E)}, 
 * even though the {@code @Valid} annotation is missing here.
 * 
 * @since 0.0.1
 */
@Service
public abstract class AbstractService<E extends AbstractEntity, Repository extends Dao<E>> {
    
    @Autowired
    private Repository repository;


    /**
     * Save abstract entity as new entity or update existing one.
     * 
     * @param entity to save
     * @return saved abstract entity
     */
    public E save(E entity) {

        if (entity == null)
            throw new ApiException(HttpStatus.BAD_REQUEST, "Failed to save entity. 'entity' cannot be null.");

        return this.repository.save(entity);
    }


    public E getById(Long id) {

        if (id == null)
            throw new ApiException(HttpStatus.BAD_REQUEST, "Failed find entity by id. 'id' cannot be null.");

        return this.repository.findById(id)
                              .orElseThrow(() -> new ApiException("Failed to find entity with id: " + id + "."));
    }


    public void delete(E entity) {

        if (entity == null)
            throw new ApiException(HttpStatus.BAD_REQUEST, "Failed to save entity. 'entity' cannot be null.");

        this.repository.delete(entity);
    }
}