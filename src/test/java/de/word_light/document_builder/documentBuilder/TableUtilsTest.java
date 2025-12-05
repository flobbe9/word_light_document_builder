package de.word_light.document_builder.documentBuilder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.List;

import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFFooter;
import org.apache.poi.xwpf.usermodel.XWPFHeader;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;

import de.word_light.document_builder.entites.documentParts.TableConfig;
import de.word_light.document_builder.entites.documentParts.style.Style;


/**
 * Unit tests for {@link TableUtils}.
 * 
 * @since 0.0.1
 */
@TestInstance(Lifecycle.PER_CLASS)
public class TableUtilsTest {

    private XWPFDocument document;

    private int contentSize;

    private Style style;

    private String cellText;

    private TableConfig headerTable;
    private TableConfig bodyTable;
    private TableConfig footerTable;
    private List<TableConfig> tableConfigs;

    private TableUtils tableUtils;


    @BeforeEach
    void init() {

        this.document = new XWPFDocument();
        // make this larger than total table cells
        this.contentSize = 30;
        this.style = Style.getDefaultInstance();
        this.cellText = "cellText";
                                    
        // keep some margin between last body table and footer table
        this.headerTable = new TableConfig(1, 2, 0);
        this.bodyTable = new TableConfig(3, 3, 2);
        this.footerTable = new TableConfig(1, 2, this.contentSize - 2);
        
        this.tableConfigs = List.of(this.headerTable, this.bodyTable, this.footerTable);
        this.tableUtils = new TableUtils(document, this.tableConfigs);
        this.tableUtils.setDoesDocumentHaveHeaderTable(true);
    }


//---------- createTableParagraph()
    @Test
    void createTableParagraph_notATableIndex_returnNull() {

        assertNull(this.tableUtils.createTableParagraph(this.footerTable.getStartIndex() - 1, this.contentSize, this.style));
    }


    @Test
    void createTableParagraph_shouldCreateNewTableInBody() {

        assertTrue(this.document.getTables().isEmpty());

        assertNotNull(this.tableUtils.createTableParagraph(this.bodyTable.getStartIndex(), this.contentSize, this.style));

        assertFalse(this.document.getTables().isEmpty());

        XWPFTable table = this.document.getTables().get(0);
        assertEquals(this.bodyTable.getNumRows(), table.getNumberOfRows());
    }

    
    @Test
    void createTableParagraph_shouldUseExistingTableInBody() {

        assertTrue(this.document.getTables().isEmpty());

        assertNotNull(this.tableUtils.createTableParagraph(this.bodyTable.getStartIndex(), this.contentSize, this.style));
        assertNotNull(this.tableUtils.createTableParagraph(this.bodyTable.getStartIndex() + 1, this.contentSize, this.style));

        assertEquals(1, this.document.getTables().size());
    }
    

    @Test
    void createTableParagraph_shouldCreateNewTableInHeader() {

        // no table in header yet
        assertNull(this.document.getHeaderFooterPolicy());

        assertNotNull(this.tableUtils.createTableParagraph(this.headerTable.getStartIndex(), this.contentSize, this.style));
        
        XWPFHeader header = this.document.getHeaderFooterPolicy().getDefaultHeader();
        assertNotNull(header);

        // should have created table inside header
        XWPFTable table = header.getTables().get(0);
        assertNotNull(table);

        assertEquals(this.headerTable.getNumRows(), table.getNumberOfRows());
    }

    
    @Test
    void createTableParagraph_shouldUseExistingTableInHeader() {
        
        assertNull(this.document.getHeaderFooterPolicy());

        assertNotNull(this.tableUtils.createTableParagraph(this.headerTable.getStartIndex(), this.contentSize, this.style));
        assertNotNull(this.tableUtils.createTableParagraph(this.headerTable.getStartIndex() + 1, this.contentSize, this.style));

        assertEquals(1, this.document.getHeaderFooterPolicy().getDefaultHeader().getTables().size());
    }


    @Test
    void createTableParagraph_shouldNotCreateHeaderTable() {

        assertNull(this.document.getHeaderFooterPolicy());

        this.headerTable.setStartIndex(this.headerTable.getStartIndex() + 1);
        this.tableUtils.createTableParagraph(this.headerTable.getStartIndex(), contentSize, style);

        // no header table
        assertNull(this.document.getHeaderFooterPolicy());

        // body table instead
        assertEquals(1, this.document.getTables().size());
    }


    @Test
    void createTableParagraph_shouldCreateNewTableInFooter() {

        // no table in header yet
        assertNull(this.document.getHeaderFooterPolicy());

        assertNotNull(this.tableUtils.createTableParagraph(this.footerTable.getStartIndex(), this.contentSize, this.style));
        
        XWPFFooter footer = this.document.getHeaderFooterPolicy().getDefaultFooter();
        assertNotNull(footer);

        // should have created table inside footer
        XWPFTable table = footer.getTables().get(0);
        assertNotNull(table);

        assertEquals(this.footerTable.getNumRows(), table.getNumberOfRows());
    }


    @Test
    void createTableParagraph_shouldUseExistingTableInFooter() {

        assertNull(this.document.getHeaderFooterPolicy());

        assertNotNull(this.tableUtils.createTableParagraph(this.footerTable.getStartIndex(), this.contentSize, this.style));
        assertNotNull(this.tableUtils.createTableParagraph(this.footerTable.getStartIndex() + 1, this.contentSize, this.style));

        assertEquals(1, this.document.getHeaderFooterPolicy().getDefaultFooter().getTables().size());
    }


    @Test
    void createTableParagraph_shouldNotCreateFooterTable() {

        assertNull(this.document.getHeaderFooterPolicy());

        this.footerTable.setStartIndex(this.footerTable.getStartIndex() - 1);
        this.tableUtils.createTableParagraph(this.footerTable.getStartIndex(), contentSize, style);

        // no footer table
        assertNull(this.document.getHeaderFooterPolicy());

        // body table instead
        assertEquals(1, this.document.getTables().size());
    }


//---------- fillTableCell()
    @Test
    void addTableCell_shouldAddText() {

        XWPFParagraph tableParagraph = this.document.createTable().createRow().createCell().addParagraph();

        // should be blank text
        assertTrue(tableParagraph.getText().isBlank());

        this.tableUtils.fillTableCell(tableParagraph, this.cellText, this.style);

        // should be cell text
        assertEquals(this.cellText, tableParagraph.getText());
    }


    @Test
    void addTableCell_shouldAddStyle() {

        XWPFParagraph tableParagraph = this.document.createTable().createRow().createCell().addParagraph();

        // should have no paragraphs yet
        assertTrue(tableParagraph.getRuns().isEmpty());

        this.tableUtils.fillTableCell(tableParagraph, this.cellText, this.style);

        // should be correct style
        assertEquals(this.style.getFontSize(), tableParagraph.getRuns().get(0).getFontSizeAsDouble().intValue());
        assertEquals(this.style.getFontFamily(), tableParagraph.getRuns().get(0).getFontFamily());
    }


//---------- isTableIndex()
    @Test
    void isTableIndex_tableHasNotStarted_shouldBeFalse() {

        assertTrue(this.tableUtils.isTableIndex(this.footerTable.getStartIndex()));
        assertFalse(this.tableUtils.isTableIndex(this.footerTable.getStartIndex() - 1));
    }


    @Test
    void isTableIndex_tableHasEnded_shouldBeFalse() {

        assertTrue(this.tableUtils.isTableIndex(this.bodyTable.getEndIndex()));
        assertFalse(this.tableUtils.isTableIndex(this.bodyTable.getEndIndex() + 1));
    }


    @AfterAll
    void cleanUp() throws IOException {

        this.document.close();
    }
}