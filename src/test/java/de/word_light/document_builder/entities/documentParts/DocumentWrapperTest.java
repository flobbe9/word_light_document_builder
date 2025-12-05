package de.word_light.document_builder.entities.documentParts;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;

import de.word_light.document_builder.entites.documentParts.BasicParagraph;
import de.word_light.document_builder.entites.documentParts.DocumentWrapper;
import de.word_light.document_builder.entites.documentParts.TableConfig;
import de.word_light.document_builder.entites.documentParts.style.Style;


/**
 * Unit tests for {@link DocumentWrapper}.
 * 
 * @since 0.0.6
 */
@TestInstance(Lifecycle.PER_CLASS)
public class DocumentWrapperTest {

    private Style style;

    private List<BasicParagraph> content;

    private List<TableConfig> tableConfigs;

    private DocumentWrapper documentWrapper;


    @BeforeEach
    void setup() {
        
        this.style = new Style(8, "Calibri", "000000", true, true, true, ParagraphAlignment.LEFT, null);
        this.content = List.of(new BasicParagraph("header", this.style), 
                               new BasicParagraph("par1", this.style), // singleColumnLine
                               new BasicParagraph("par2", this.style), // table
                               new BasicParagraph("par3", this.style), // table
                               new BasicParagraph("par4", this.style), // table
                               new BasicParagraph("par5", this.style), // table
                               new BasicParagraph("footer", this.style)); // table
        this.tableConfigs = new ArrayList<>(List.of(new TableConfig(1, 1, 2),
                                                    new TableConfig(2, 1, 3), 
                                                    new TableConfig(2, 1, 5))); 
        this.documentWrapper = new DocumentWrapper(this.content, this.tableConfigs, false, "Document_1.docx", 2, 1);
    }


    @Test
    void isTableConfigsNotOverlap_shouldBeValid() {

        assertTrue(this.documentWrapper.isTableConfigsNotOverlap());
    }


    @Test
    void isTableConfigsNotOverlap_isOverlapping() {

        assertTrue(this.documentWrapper.isTableConfigsNotOverlap());

        // set start index of second table config equal end index of first one
        int firstTableConfigEndIndex = this.documentWrapper.getTableConfigs().get(0).getEndIndex(); 
        this.documentWrapper.getTableConfigs().get(1).setStartIndex(firstTableConfigEndIndex);
        assertFalse(this.documentWrapper.isTableConfigsNotOverlap());

        // set start index of second table config less than end index of first one
        this.documentWrapper.getTableConfigs().get(1).setStartIndex(firstTableConfigEndIndex - 1);
        assertFalse(this.documentWrapper.isTableConfigsNotOverlap());

        // set start index of second table config greater than end index of first one
        this.documentWrapper.getTableConfigs().get(1).setStartIndex(firstTableConfigEndIndex + 1);
        assertTrue(this.documentWrapper.isTableConfigsNotOverlap());
    }


    @Test
    void isIndicesNotExceedContentSize_shouldBeValid() {

        assertTrue(this.documentWrapper.isIndicesNotExceedContentSize());
    }


    @Test
    void isIndicesNotExceedContentSize_startIndexShouldExceedContentSize() {

        assertTrue(this.documentWrapper.isIndicesNotExceedContentSize());

        // increase start index
        this.documentWrapper.getTableConfigs().get(0).setStartIndex(this.content.size());
        assertFalse(this.documentWrapper.isIndicesNotExceedContentSize());
    }

    @Test
    void isIndicesNotExceedContentSize_endIndexShouldExceedContentSize() {

        assertTrue(this.documentWrapper.isIndicesNotExceedContentSize());

        // increase end index
        TableConfig firstTableConfig = this.documentWrapper.getTableConfigs().get(0);
        firstTableConfig.setNumRows(this.content.size() + firstTableConfig.getStartIndex() + 1);
        assertFalse(this.documentWrapper.isIndicesNotExceedContentSize());
    }


    @Test
    void isSingleColumnLineNotInsideTable_isInsideTable() {

        assertTrue(this.documentWrapper.isSingleColumnLineNotInsideTable());

        int firstTableConfigStartIndex = this.documentWrapper.getTableConfigs().get(0).getStartIndex(); 
        this.documentWrapper.setNumSingleColumnLines(firstTableConfigStartIndex);
        assertFalse(this.documentWrapper.isSingleColumnLineNotInsideTable());

        this.documentWrapper.setNumSingleColumnLines(firstTableConfigStartIndex - 1);
        assertTrue(this.documentWrapper.isSingleColumnLineNotInsideTable());
    }


    @Test
    void isSingleColumnLineNotInsideTable_shouldBeValid() {

        assertTrue(this.documentWrapper.isSingleColumnLineNotInsideTable());

        this.documentWrapper.setNumSingleColumnLines(0);
        assertTrue(this.documentWrapper.isSingleColumnLineNotInsideTable());
    }


    @Test
    void isNumSingleColumnLinesValid_shouldBeValid() {

        assertTrue(this.documentWrapper.isNumSingleColumnLinesValid());
    }

    
    @Test
    void isNumSingleColumnLinesValid_isTooLarge() {

        assertTrue(this.documentWrapper.isNumSingleColumnLinesValid());

        this.documentWrapper.setNumSingleColumnLines(this.content.size() - 1);
        assertFalse(this.documentWrapper.isNumSingleColumnLinesValid());

        this.documentWrapper.setNumSingleColumnLines(this.content.size() - 2);
        assertTrue(this.documentWrapper.isNumSingleColumnLinesValid());

        this.documentWrapper.setNumSingleColumnLines(this.content.size() - 3);
        assertTrue(this.documentWrapper.isNumSingleColumnLinesValid());
    }


    // TODO: add pattern tests
    // no special chars at start
    // correct chars at start -_. 
    // ...
    @Test
    void fileNamePattern_shouldBeInvalid() {


    }

}