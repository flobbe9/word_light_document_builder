package de.word_light.document_builder.controllers;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import de.word_light.document_builder.entites.documentParts.BasicParagraph;
import de.word_light.document_builder.entites.documentParts.DocumentWrapper;
import de.word_light.document_builder.entites.documentParts.TableConfig;
import de.word_light.document_builder.entites.documentParts.style.Style;
import de.word_light.document_builder.utils.TestUtils;
import de.word_light.document_builder.utils.Utils;
import static de.word_light.document_builder.utils.Utils.DOCX_FOLDER;
import static de.word_light.document_builder.utils.Utils.PDF_FOLDER;
import static de.word_light.document_builder.utils.Utils.PICTURES_FOLDER;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.List;

import org.apache.poi.xwpf.usermodel.ParagraphAlignment;


/**
 * Integration test for {@link DocumentController}.
 * 
 * @since 0.0.1
 */
@SpringBootTest
@TestInstance(Lifecycle.PER_CLASS)
@AutoConfigureMockMvc(addFilters = false)
@TestMethodOrder(OrderAnnotation.class)
public class DocumentControllerTest {

    @Value("${BASE_URL}")
    private String BASE_URL;
    
    @Value("${MAPPING}")
    private String MAPPING;

    private TestUtils testUtils;

    @Autowired
    private MockMvc mockMvc;

    private Style style;
    private List<BasicParagraph> content;
    private List<TableConfig> tableConfigs;
    private DocumentWrapper documentWrapper;
    private String docxFileName;


    @BeforeEach
    void setup() {
        
        this.testUtils = new TestUtils(mockMvc, this.BASE_URL + "/" + this.MAPPING);
        this.style = new Style(8, "Calibri", "000000", true, true, true, ParagraphAlignment.LEFT, null);
        this.content = List.of(new BasicParagraph("header", this.style), new BasicParagraph("text", this.style), new BasicParagraph("footer", this.style));
        this.tableConfigs = new ArrayList<>(List.of(new TableConfig(2, 1, 0)));
        this.docxFileName = "Document_1.docx";
        this.documentWrapper = new DocumentWrapper(this.content, tableConfigs, false, this.docxFileName, 1, 1);
    }


    @Test
    @Order(0)
    void buildAndWrite_shouldBeStatus200() throws Exception {

        MvcResult response = this.testUtils.performPost("/buildAndWrite", this.documentWrapper, null)
                                            .andExpect(status().isOk())
                                            .andReturn();

        TestUtils.checkApiExceptionFormatPrettySuccess(response.getResponse().getContentAsString(), OK);
    }


    @Test
    void buildAndWrite_shouldBeStatus400_bodyNull() throws Exception {

        MvcResult response = this.testUtils.performPost("/buildAndWrite", null, null)
                                            .andExpect(status().isBadRequest())
                                            .andReturn();

        String jsonResponse = response.getResponse().getContentAsString();

        TestUtils.checkJsonApiExceptionFormat(jsonResponse, HttpStatus.BAD_REQUEST);
    }


    @Test 
    @Order(2)
    void buildAndWrite_shouldBeStatus400_invalidContent() throws Exception {

        this.documentWrapper.getContent().get(0).setText(null);
        
        MvcResult response = this.testUtils.performPost("/buildAndWrite", this.documentWrapper, null)
                            .andExpect(status().isBadRequest())
                            .andReturn();

        String jsonResponse = response.getResponse().getContentAsString();

        TestUtils.checkJsonApiExceptionFormat(jsonResponse, HttpStatus.BAD_REQUEST);
    }


    @Test
    @Order(4)
    void buildAndWrite_shouldBeStatus400_invalidNumColumns() throws Exception {

        this.documentWrapper.setNumColumns(0);
        
        MvcResult response = this.testUtils.performPost("/buildAndWrite", this.documentWrapper, null)
                            .andExpect(status().isBadRequest())
                            .andReturn();

        String jsonResponse = response.getResponse().getContentAsString();

        TestUtils.checkJsonApiExceptionFormat(jsonResponse, HttpStatus.BAD_REQUEST);
    }


    @Test
    @Order(4)
    void buildAndWrite_shouldBeStatus400_invalidNumSingleColumnLines() throws Exception {

        this.documentWrapper.setNumSingleColumnLines(this.content.size());
        
        MvcResult response = this.testUtils.performPost("/buildAndWrite", this.documentWrapper, null)
                            .andExpect(status().isBadRequest())
                            .andReturn();

        String jsonResponse = response.getResponse().getContentAsString();

        TestUtils.checkJsonApiExceptionFormat(jsonResponse, HttpStatus.BAD_REQUEST);
    }


    @Test
    void download_shouldBeStatus409_didNotCreateDocument() throws Exception {

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("pdf", "false");

        MvcResult response = this.testUtils.performPost("/download", null,params)
                            .andExpect(status().isConflict())
                            .andReturn();

        TestUtils.checkJsonApiExceptionFormat(response.getResponse().getContentAsString(), HttpStatus.CONFLICT);
    }


    @AfterAll
    void cleanUp() {

        Utils.clearFolderByFileName(DOCX_FOLDER);
        Utils.clearFolderByFileName(PDF_FOLDER);
        Utils.clearFolderByFileName(PICTURES_FOLDER);
    }
}