package de.word_light.document_builder.documentBuilder;

import static de.word_light.document_builder.utils.Utils.STATIC_FOLDER;
import static de.word_light.document_builder.utils.Utils.RESOURCE_FOLDER;
import static de.word_light.document_builder.utils.Utils.DOCX_FOLDER;
import static de.word_light.document_builder.utils.Utils.prependSlash;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.UnderlinePatterns;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTPageMar;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTSectPr;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STPageOrientation;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import de.word_light.document_builder.entites.documentParts.BasicParagraph;
import de.word_light.document_builder.entites.documentParts.TableConfig;
import de.word_light.document_builder.entites.documentParts.style.Style;
import de.word_light.document_builder.exception.ApiException;
import de.word_light.document_builder.utils.Utils;


/**
 * Unit tests for {@link DocumentBuilder}.
 * 
 * @since 0.0.1
 */
@TestInstance(Lifecycle.PER_CLASS)
@TestMethodOrder(OrderAnnotation.class)
public class DocumentBuilderTest {

    public static final String TEST_RESOURCE_FOLDER = "./src/main/resources/static/test";

    private XWPFDocument document;

    private String testDocxFileName;

    private String docxFileName;
    private boolean landscape;
    
    private Style style;
    
    private BasicParagraph header; 
    private BasicParagraph title; 
    private BasicParagraph tableCell;
    private BasicParagraph picture;
    private BasicParagraph footer; 
    
    private List<BasicParagraph> content;

    private List<TableConfig> tableConfigs = new ArrayList<>();
    private int numColumns;
    private int numSingleColumnLines;

    private int numTableColumns;
    private int numRows;
    private int startIndex;

    private PictureUtils pictureUtils;
    private String testPictureName;
    private Map<String, byte[]> pictures = new HashMap<>();

    private DocumentBuilder documentBuilder;


    @BeforeEach
    void setup() {
        
        // picture
        this.testPictureName = "test.png";
        this.pictures.put(this.testPictureName, Utils.fileToByteArray(new File(TEST_RESOURCE_FOLDER + Utils.prependSlash(testPictureName))));
        this.pictureUtils = new PictureUtils(this.pictures);

        // content
        this.style = new Style(11, 
                                    "times new roman", 
                                    "2B01FF", // blue
                                    true, 
                                    true, 
                                    true,
                                    ParagraphAlignment.CENTER, 
                                    null);        
        this.header = new BasicParagraph("This is the header", this.style);
        this.title = new BasicParagraph("This is the title", this.style);
        this.tableCell = new BasicParagraph("This is a table cell", this.style);
        this.picture = new BasicParagraph("${" + this.testPictureName + "}", style);
        this.footer = new BasicParagraph("This is the footer", this.style);
        this.content = Arrays.asList(this.header, this.title, this.tableCell, this.picture, this.footer);
        
        // table
        this.numColumns = 2;
        this.numTableColumns = 1;
        this.numSingleColumnLines = 1;
        this.numRows = 1;
        this.startIndex = 2;
        this.tableConfigs.add(new TableConfig(this.numTableColumns, this.numRows, this.startIndex));
        
        // document
        this.testDocxFileName = "test/test.docx";
        this.landscape = true;
        this.documentBuilder = new DocumentBuilder(this.content, "temp.docx", this.numColumns, this.numSingleColumnLines, this.landscape, this.pictures, this.tableConfigs);
        this.docxFileName = this.documentBuilder.getDocxFileName();
        this.document = this.documentBuilder.getDocument();
        this.documentBuilder.setPictureUtils(this.pictureUtils);
    }


//----------- build()
    @Test
    void build_paragraphShouldExist() {

        // should have no paragraphs
        assertTrue(this.document.getParagraphs().size() == 0);

        // file should not exist
        assertFalse(new File(RESOURCE_FOLDER + "/" + this.docxFileName).exists());

        this.documentBuilder.build();

        // should have number of paragraphs minus header, footer
        assertEquals(this.content.size() - 2, this.document.getParagraphs().size());  
        
        // should have empty paragraph at i == numSingleColumnLines + 1
        assertTrue(StringUtils.isBlank(this.document.getParagraphs().get(this.numSingleColumnLines + 1).getText()));

        // should have section
        XWPFParagraph lastSingleColumnLineParagraph = this.document.getParagraphs().get(0);
        assertTrue(lastSingleColumnLineParagraph.getCTP().getPPr().xmlText().contains("</main:sectPr>"));

        // check orientation
        assertEquals(this.landscape ? STPageOrientation.LANDSCAPE : STPageOrientation.PORTRAIT, this.documentBuilder.getPgSz().getOrient());
    }


//----------- addContent()
    @Test
    void addContent_shouldHaveNoParagraphWithoutContent() {

        // claer content
        this.documentBuilder.setContent(new ArrayList<>());

        // addContent
        this.documentBuilder.addContent();

        // expect num paragraphs 0
        assertEquals(0, this.document.getParagraphs().size());
    }


    @Test
    void addContent_shouldHaveParagraph() {

        // should have no paragraphs
        assertEquals(0, this.document.getParagraphs().size());

        this.documentBuilder.addContent();

        // should have number of paragraphs minus header, footer
        assertEquals(this.content.size() - 2, this.document.getParagraphs().size());
    }


//----------- setDocumentMargins()
    @Test
    void setDocumentMargins_shouldWork() {

        assertNull(this.document.getDocument().getBody().getSectPr());

        int right = 100;
        int left = 110;
        this.documentBuilder.setDocumentMargins(DocumentBuilder.MINIMUM_MARGIN_TOP, right, DocumentBuilder.MINIMUM_MARGIN_BOTTOM, left);

        CTPageMar pageMar = this.document.getDocument().getBody().getSectPr().getPgMar();
        assertEquals(DocumentBuilder.MINIMUM_MARGIN_TOP, Integer.parseInt(pageMar.getTop().toString()));
        assertEquals(DocumentBuilder.MINIMUM_MARGIN_BOTTOM, Integer.parseInt(pageMar.getBottom().toString()));
        assertEquals(right, Integer.parseInt(pageMar.getRight().toString()));
        assertEquals(left, Integer.parseInt(pageMar.getLeft().toString()));
    }


//----------- setDocumentColumns()
    @Test
    void setDocumentColumns_shouldWork() {

        assertNull(this.document.getDocument().getBody().getSectPr());

        this.documentBuilder.setNumColumns(2);
        this.documentBuilder.setDocumentColumns();

        CTSectPr ctSectPr = this.document.getDocument().getBody().getSectPr();
        String xmlCol1Tag = "<main:cols main:num=\"1\"/>";
        String xmlCol2Tag = "<main:cols main:num=\"2\"/>";
        String ctSectPrString = ctSectPr.toString();

        assertTrue(ctSectPrString.contains(xmlCol1Tag));
        assertTrue(ctSectPrString.contains(xmlCol2Tag));
    }


//----------- setIsTabStopsByFontSize()
    @Test 
    void setIsTabStopsByFontSize_shouldWork() {

        XWPFParagraph paragraph = this.document.createParagraph();

        assertThrows(NullPointerException.class, () -> paragraph.getCTP().getPPr().getTabs().getTabList());
        
        // dont set tab stops by font size
        this.documentBuilder.setIsTabStopsByFontSize(false);
        this.documentBuilder.applyStyle(paragraph, this.style);
        assertNull(paragraph.getCTP().getPPr().getTabs());

        // set tab stops by font size
        this.documentBuilder.setIsTabStopsByFontSize(true);
        this.documentBuilder.applyStyle(paragraph, this.style);

        assertDoesNotThrow(() -> paragraph.getCTP().getPPr().getTabs().getTabList().get(0));
    }


//----------- addParagraph()
    @Test
    void addParagraph_pictureInsideTable_shouldDoNothing() {

        this.tableCell.setText("picture.png");
        int currentContentIndex = this.content.indexOf(this.tableCell);

        // should have no paragraphs
        assertTrue(this.document.getParagraphs().size() == 0);

        this.documentBuilder.addParagraph(currentContentIndex);

        // should still have no paragraph
        assertTrue(this.document.getParagraphs().isEmpty());
    }


    @Test
    void addParagraph_basicParagraphNull_shouldThrow() {

        // set basicParagraph null
        int currentContentIndex = this.content.indexOf(this.title);
        this.content.set(currentContentIndex, null);
        
        assertThrows(ApiException.class, () -> this.documentBuilder.addParagraph(currentContentIndex));
    }


    @Test
    void addParagraph_shouldAddTextAndStyle() {

        // use title
        int currentContentIndex = this.content.indexOf(this.title);

        this.documentBuilder.addParagraph(currentContentIndex);

        BasicParagraph expectedParagraph = this.content.get(currentContentIndex);
        XWPFParagraph actualParagraph = this.document.getLastParagraph();
        
        // should have text
        assertEquals(expectedParagraph.getText(), actualParagraph.getText());

        // should have style, check one attribute
        assertEquals(expectedParagraph.getStyle().getTextAlign(), actualParagraph.getAlignment());
    }


    @Test
    void addParagraph_shouldAddEmptyLineIfTextBlank() {

        // turn title text blank
        int currentContentIndex = this.content.indexOf(this.title);
        BasicParagraph basicParagraph = this.content.get(currentContentIndex);
        basicParagraph.setText(" ");

        XWPFParagraph paragraph = this.documentBuilder.addParagraph(currentContentIndex);

        List<XWPFRun> runs = paragraph.getRuns();
        // should have created two runs
        assertEquals(2, runs.size());

        XWPFRun invisibleRun = paragraph.getRuns().get(0);
        assertEquals("_", invisibleRun.text());
        assertEquals("FFFFFF", invisibleRun.getColor());

        XWPFRun spaceCharRun = paragraph.getRuns().get(1);
        assertEquals(" ", spaceCharRun.text());
    }


    @Test
    void addParagraph_shouldAddEmptyLineIfTextHasTabsOnly() {

        // turn title text blank
        int currentContentIndex = this.content.indexOf(this.title);
        BasicParagraph basicParagraph = this.content.get(currentContentIndex);
        basicParagraph.setText("\t ");

        XWPFParagraph paragraph = this.documentBuilder.addParagraph(currentContentIndex);

        List<XWPFRun> runs = paragraph.getRuns();
        // should have created two runs
        assertEquals(2, runs.size());

        XWPFRun invisibleRun = paragraph.getRuns().get(0);
        assertEquals("_", invisibleRun.text());
        assertEquals("FFFFFF", invisibleRun.getColor());

        XWPFRun spaceCharRun = paragraph.getRuns().get(1);
        assertEquals(" ", spaceCharRun.text());
    }


//----------- createParagraphByContentIndex()
    @Test
    void createParagraphByContentIndex_isTableIndex_shouldReturnTableParagraph() {

        // should not have tables
        assertEquals(0, this.documentBuilder.getDocument().getTables().size());
        
        this.documentBuilder.createParagraphByContentIndex(this.startIndex, this.style);

        assertEquals(this.numTableColumns * this.numRows, this.documentBuilder.getDocument().getTables().get(0).getRows().get(0).getTableCells().size());
    }


    @Test
    void createParagraphByContentIndex_isTableIndex_shouldNotCreateParagraph() {

        // should not have tables
        assertEquals(0, this.documentBuilder.getDocument().getTables().size());
        
        this.documentBuilder.setTableUtils(null);
        this.documentBuilder.createParagraphByContentIndex(this.startIndex, this.style);

        // should still have no tables
        assertEquals(0, this.documentBuilder.getDocument().getTables().size());
    }


    @Test
    void createParagraphByContentIndex_isNoTableIndex_shouldNotAddTable() {

        // should not have tables
        assertEquals(0, this.documentBuilder.getDocument().getTables().size());

        this.documentBuilder.createParagraphByContentIndex(this.startIndex - 1, this.style);

        // should still have no tables
        assertEquals(0, this.documentBuilder.getDocument().getTables().size());
    }
    

    @Test
    void createParagraphByContentIndex_blankText_shouldNotAddHeaderParagraph() {

        // should not have header yet
        assertThrows(NullPointerException.class, () -> this.documentBuilder.getDocument().getHeaderFooterPolicy().getDefaultHeader().getParagraphs());

        // set blank text (not empty)
        this.content.get(0).setText(" ");

        this.documentBuilder.createParagraphByContentIndex(0, this.style);
        assertThrows(NullPointerException.class, () -> this.documentBuilder.getDocument().getHeaderFooterPolicy().getDefaultHeader().getParagraphs());
    }

    
    @Test
    void createParagraphByContentIndex_shouldAddHeaderParagraph() {

        // should not have header yet
        assertThrows(NullPointerException.class, () -> this.documentBuilder.getDocument().getHeaderFooterPolicy().getDefaultHeader().getParagraphs());

        this.documentBuilder.createParagraphByContentIndex(0, this.style);
        assertEquals(1, this.documentBuilder.getDocument().getHeaderFooterPolicy().getDefaultHeader().getParagraphs().size());
    }


    @Test
    void createParagraphByContentIndex_blankText_shouldNotAddFooterParagraph() {

        // should not have header yet
        assertThrows(NullPointerException.class, () -> this.documentBuilder.getDocument().getHeaderFooterPolicy().getDefaultFooter().getParagraphs());

        // set blank text (not empty)
        this.content.get(this.content.size() - 1).setText(" ");

        this.documentBuilder.createParagraphByContentIndex(this.content.size() - 1, this.style);
        assertThrows(NullPointerException.class, () -> this.documentBuilder.getDocument().getHeaderFooterPolicy().getDefaultFooter().getParagraphs());
    }


    @Test
    void createParagraphByContentIndex_shouldAddFooterParagraph() {

        // should not have header yet
        assertThrows(NullPointerException.class, () -> this.documentBuilder.getDocument().getHeaderFooterPolicy().getDefaultFooter().getParagraphs());

        this.documentBuilder.createParagraphByContentIndex(this.content.size() - 1, this.style);
        assertEquals(1, this.documentBuilder.getDocument().getHeaderFooterPolicy().getDefaultFooter().getParagraphs().size());
    }


    @Test
    void createParagraphByContentIndex_shouldReturnNormalParagraph() {

        // should not have paragraphs yet
        assertTrue(this.documentBuilder.getDocument().getParagraphs().size() == 0);

        this.documentBuilder.createParagraphByContentIndex(1, this.style);
        assertEquals(1, this.documentBuilder.getDocument().getParagraphs().size());
    }


//----------- addText() 
    @Test
    void addText_notAPicture_shouldNotAddPicture() {

        // use wrong index
        int titleIndex = this.content.indexOf(this.title);

        this.documentBuilder.addText(this.document.createParagraph(), this.title, titleIndex);

        // expect no picture
        assertEquals(0, this.document.getAllPictures().size());
    }

    
    @Test
    void addText_shouldAddPicture() {

        // should start without pictures
        assertEquals(0, this.document.getAllPictures().size());

        // add inside header to avoid table
        this.documentBuilder.addText(this.document.createParagraph(), this.picture, 0);

        // expect picture
        assertEquals(1, this.document.getAllPictures().size());
    }


    @Test
    void addText_tableUtilsNull_shouldNotAddCell() {

        int tableCellIndex = this.content.indexOf(this.tableCell);

        // set tableUtils null
        this.documentBuilder.setTableUtils(null);

        this.documentBuilder.addText(this.document.createParagraph(), this.tableCell, tableCellIndex);
        
        // expect no table
        assertEquals(0, this.document.getTables().size());        
    }


    @Test
    void addText_notATable_shouldNotAddTable() {

        // use wrong index
        int titleIndex = this.content.indexOf(this.title);

        this.documentBuilder.addText(this.document.createParagraph(), this.title, titleIndex);

        // expect no table
        assertEquals(0, this.document.getAllPictures().size());
    }


    @Test
    void addText_shouldAddText_shouldNotHaveTableOrPicture() {

        int titleIndex = this.content.indexOf(this.title);

        this.documentBuilder.addText(document.createParagraph(), this.title, titleIndex);

        // expect no table
        assertEquals(0, this.document.getAllPictures().size());

        // expect no picture
        assertEquals(0, this.document.getAllPictures().size());

        // expect title text
        assertEquals(this.title.getText(), this.document.getLastParagraph().getText());
    }


//----------- applyStyle()
    @Test
    void addStyle_paragraphNull_shouldNotThrow() {

        assertDoesNotThrow(() -> new DocumentBuilder().applyStyle(null, this.style));
    }


    @Test
    void addStyle_styleNull_shouldNotThrow() {

        assertDoesNotThrow(() -> new DocumentBuilder().applyStyle(this.document.createParagraph(), null));
    }


    @Test
    void addStyle_shouldApplyStyleCorrectly() {

        // add some test content
        XWPFParagraph paragraph = this.document.createParagraph();
        XWPFRun run = paragraph.createRun();
        new DocumentBuilder().applyStyle(paragraph, this.style);

        // check each style attribute
        assertEquals(this.style.getFontSize(), (int) Math.round(run.getFontSizeAsDouble()));
        assertEquals(this.style.getFontFamily(), run.getFontFamily());
        assertEquals(this.style.getColor(), run.getColor());
        assertEquals(this.style.getBold(), run.isBold());
        assertEquals(this.style.getItalic(), run.isItalic());
        assertEquals(this.style.getUnderline(), run.getUnderline().equals(UnderlinePatterns.SINGLE));
        assertEquals(this.style.getTextAlign(), paragraph.getAlignment());
    }


    @Test 
    void addStyle_breakTypeNull_shouldNotThrow() {

        this.style.setBreakType(null);
        assertDoesNotThrow(() -> new DocumentBuilder().applyStyle(this.document.createParagraph(), this.style));
    }


// ---------- readDocxFile()
    @Test
    void readDocxFile_shouldWorkWithFalsyInput() {
        
        // mock request context for ApiExceptionHandler
        MockHttpServletRequest request = new MockHttpServletRequest();
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        XWPFDocument document = this.documentBuilder.readDocxFile("some non existing file");

        // expect clean document
        assertEquals(0, document.getParagraphs().size());
    }


    @Test
    void readDocxFile_shouldWorkWithTruthyInput() {

        XWPFDocument document = this.documentBuilder.readDocxFile(STATIC_FOLDER + prependSlash(this.testDocxFileName));

        // expect clean document
        assertEquals(0, document.getParagraphs().size());
    }


//----------- writeDocxFile()
    @Test
    void writeToDocxFile_fileNameWithoutSlash_shouldBeTrue() {

        // file should not exist yet
        assertFalse(new File(DOCX_FOLDER + "/" + this.docxFileName).exists());

        // should return true
        this.documentBuilder.writeDocxFile();

        // file should exist
        assertTrue(new File(DOCX_FOLDER + "/" + this.docxFileName).exists());
    }


    @Test
    void writeToDocxFile_fileNameWithSlash_shouldBeTrue() {

        // file should not exist yet
        assertFalse(new File(DOCX_FOLDER + "/" + this.docxFileName).exists());

        this.documentBuilder.setDocxFileName("/" + this.docxFileName);
        this.documentBuilder.writeDocxFile();

        // file should exist
        assertTrue(new File(DOCX_FOLDER + "/" + this.docxFileName).exists());
    }


    @AfterEach
    void cleanUp() throws IOException {

        Utils.clearFolder(DOCX_FOLDER, null);
    }
}