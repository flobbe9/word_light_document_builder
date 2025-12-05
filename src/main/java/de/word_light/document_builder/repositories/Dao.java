package de.word_light.document_builder.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;

import de.word_light.document_builder.abstracts.AbstractEntity;


@NoRepositoryBean
public interface Dao <E extends AbstractEntity> extends JpaRepository<E, Long> {
    
}