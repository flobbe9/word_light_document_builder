package de.word_light.document_builder.documentBuilder;

import static de.word_light.document_builder.documentBuilder.DocumentBuilderTest.TEST_RESOURCE_FOLDER;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.poi.common.usermodel.PictureType;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;

import de.word_light.document_builder.exception.ApiException;
import de.word_light.document_builder.utils.Utils;


/**
 * Unit tests for {@link PictureUtils}.
 * 
 * @since 0.0.1
 */
@TestInstance(Lifecycle.PER_CLASS)
public class PictureUtilsTest {

    private XWPFDocument document;

    private XWPFRun run;
    
    private String testPictureName;
    
    private PictureUtils pictureUtils;

    private Map<String, byte[]> pictures = new HashMap<>();

    @BeforeAll
    void beforeAll() {
        this.testPictureName = "test.png";
        try (FileInputStream fis = new FileInputStream(new File(TEST_RESOURCE_FOLDER + Utils.prependSlash(testPictureName)));
            BufferedInputStream bis = new BufferedInputStream(fis)) {
            this.pictures.put(testPictureName, bis.readAllBytes());

        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        this.pictureUtils = new PictureUtils(this.pictures);
    }

    @BeforeEach
    void init() {
        // initialize fields
        this.testPictureName = "test.png";
        this.document = new XWPFDocument();
        this.run = document.createParagraph().createRun();
    }

//------------ addPicture()
    @Test
    void addPicture_picturesNull_shouldNotThrow() {

        // set pictures null
        this.pictureUtils.setPictures(null);

        // should not throw
        assertDoesNotThrow(() -> this.pictureUtils.addPicture(run, testPictureName));
    }


    @Test
    void addPicture_picturesEmpty_shouldNotThrow() {

        this.pictureUtils.setPictures(Map.of());

        assertDoesNotThrow(() -> this.pictureUtils.addPicture(run, testPictureName));
    }


    @Test
    void addPicture_fielNameNotInList_shouldThrow() {

        this.testPictureName = "mockName.png";
        
        assertThrows(ApiException.class, () -> this.pictureUtils.addPicture(run, testPictureName));
    }


    @Test
    void addPicture_runNull_shouldThrow() {

        this.run = null;
        
        assertThrows(ApiException.class, () -> this.pictureUtils.addPicture(run, testPictureName));
    }


    @Test
    void addPicture_fileNameNull_shouldThrow() {

        // set fileName null
        this.testPictureName = null;
        
        assertThrows(ApiException.class, () -> this.pictureUtils.addPicture(run, testPictureName));
    }


    @Test 
    void addPicture_notAPicture_shouldThrow() {

        this.testPictureName = "mockName.pdf";

        assertThrows(ApiException.class, () -> this.pictureUtils.addPicture(run, testPictureName));
    }


    @Test 
    void addPicture_shouldAddPicture() {

        // should start without pictures
        assertTrue(this.run.getEmbeddedPictures().isEmpty());

        // should not throw
        assertDoesNotThrow(() -> this.pictureUtils.addPicture(run, testPictureName));

        // should have picture
        assertFalse(this.run.getEmbeddedPictures().isEmpty());
    }


//------------ getPictureType()
    @Test
    void getPictureType_fileNameNull_shouldReturnNull() {

        assertEquals(null, PictureUtils.getPictureType(null));
    }


    @Test
    void getPictureType_invalidFileName_shouldReturnNull() {

        assertEquals(null, PictureUtils.getPictureType(""));

        assertEquals(null, PictureUtils.getPictureType("mockName"));

        assertEquals(null, PictureUtils.getPictureType("mockName.xyz"));

        assertEquals(null, PictureUtils.getPictureType("mockName.pdf"));

        // .jpeg not supported, only .jpg
        assertEquals(null, PictureUtils.getPictureType("mockName.jpeg"));
    }


    @Test
    void getPictureType_shouldReturnCorrectType() {
        assertEquals(PictureType.PNG, PictureUtils.getPictureType(this.testPictureName));

        assertEquals(PictureType.JPEG, PictureUtils.getPictureType("test.jpg"));
    }


//----------- isPicture()
    @Test 
    void isPicture_shouldBeTrue() {

        String pictureName = "${" + this.testPictureName + "}";
        assertTrue(PictureUtils.isPicture(pictureName));

        pictureName = "${s.png}";
        assertTrue(PictureUtils.isPicture(pictureName));

        pictureName = "${.png}";
        assertTrue(PictureUtils.isPicture(pictureName));
    }


    @Test
    void isPicture_shouldBeFalse() {

        String pictureName = "${" + this.testPictureName + "}";
        assertTrue(PictureUtils.isPicture(pictureName));

        pictureName = "{" + this.testPictureName + "}";
        assertFalse(PictureUtils.isPicture(pictureName));

        pictureName = "$" + this.testPictureName + "}";
        assertFalse(PictureUtils.isPicture(pictureName));

        pictureName = "" + this.testPictureName + "}";
        assertFalse(PictureUtils.isPicture(pictureName));

        pictureName = "${" + this.testPictureName + "";
        assertFalse(PictureUtils.isPicture(pictureName));

        pictureName = "" + this.testPictureName + "";
        assertFalse(PictureUtils.isPicture(pictureName));

        pictureName = "${png}";
        assertFalse(PictureUtils.isPicture(pictureName));

        assertFalse(PictureUtils.isPicture(null));

        assertFalse(PictureUtils.isPicture(""));
    }
}