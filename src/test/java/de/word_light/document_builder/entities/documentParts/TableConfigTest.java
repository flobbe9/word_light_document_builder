package de.word_light.document_builder.entities.documentParts;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.word_light.document_builder.entites.documentParts.TableConfig;


/**
 * Unit tests for {@link TableConfig}.
 * 
 * @since 0.0.1
 */
public class TableConfigTest {

    private TableConfig tableConfig;

    
    @BeforeEach
    void setup() {

        this.tableConfig = new TableConfig(3, 5, 1);
    }


    @Test
    void getEndIndex_shouldBeCorrect() {

        int endIndex = this.tableConfig.getStartIndex() + this.tableConfig.getNumColumns() * this.tableConfig.getNumRows() - 1;

        assertEquals(endIndex, tableConfig.getEndIndex());
    }


    @Test
    void isValid_tableBiggerThanContent_shouldBeTrue() {

        // make table bigger
        this.tableConfig.setNumColumns(this.tableConfig.getNumColumns() + 1);
        assertTrue(this.tableConfig.isValid());
    }

    @Test
    void isValid_useExactValues_shouldBeTrue() {

        assertTrue(this.tableConfig.isValid());
    }
}