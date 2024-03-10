package de.word_light.document_builder.controllers;

import static de.word_light.document_builder.utils.Utils.PICTURES_FOLDER;
import static de.word_light.document_builder.utils.Utils.prependSlash;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.annotation.SessionScope;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import de.word_light.document_builder.documentBuilder.DocumentBuilder;
import de.word_light.document_builder.documentBuilder.PictureUtils;
import de.word_light.document_builder.entites.documentParts.BasicParagraph;
import de.word_light.document_builder.entites.documentParts.DocumentWrapper;
import de.word_light.document_builder.exception.ApiException;
import de.word_light.document_builder.exception.ApiExceptionFormat;
import de.word_light.document_builder.exception.ApiExceptionHandler;
import de.word_light.document_builder.utils.Utils;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.extern.log4j.Log4j2;


/**
 * REST controller handling all requests related to document building logic.
 * 
 * @since 0.0.1
 */
@RestController
@RequestMapping("${MAPPING}")
@SessionScope
@Validated
@Log4j2
@Tag(name = "Document builder logic")
public class DocumentController {

    @Value("${ENV}")
    private String ENV;

    private DocumentWrapper documentWrapper = new DocumentWrapper();

    private File file;


    /**
     * Builds word document, writes to .docx file. <p>
     * 
     * Assuming that: <p>
     * first {@link BasicParagraph} is the header <p>
     * last {@link BasicParagraph} is the footer <p>
     * anything in between is main content <p>.
     * 
     * @param documentWrapper wrapper object containing all document information
     * @param bindingResult for handling bad requests
     */
    @PostMapping("/buildAndWrite")
    @Operation(summary = "Build document and write to .docx.")
    public ApiExceptionFormat buildAndWrite(@RequestBody @Valid DocumentWrapper wrapper, BindingResult bindingResult, @RequestHeader Map<String, String> headers) {

        // pictures may have been uploaded before
        wrapper.setPictures(this.documentWrapper.getPictures());

        this.documentWrapper = wrapper;

        // build docx
        File file = buildAndWriteDocument();

        this.file = file;

        return ApiExceptionHandler.returnPrettySuccess(OK);
    }


    /**
     * Convert given file to stream and delete file afterwards.<p>
     * 
     * Deletes {@link #file} and clears {@code this.documentWrapper.getPictures()} after download (successful or not).
     * 
     * @param file to download
     * @param fileName to use for downloaded file
     * @return {@link StreamingResponseBody} of file with correct headers for download
     */
    @PostMapping(path = "/download", produces = {"application/octet-stream", "application/json"})
    @Operation(summary = "Download existing .docx or .pdf file. Needs a call to '/buildAndWrite' first.")
    public ResponseEntity<StreamingResponseBody> downloadDocument(@RequestParam(name = "pdf") boolean pdf) {

        log.info("Downloading document...");

        // case: no document created yet
        if (this.documentWrapper == null || this.file == null || !this.file.exists()) 
            throw new ApiException(HttpStatus.CONFLICT, "Failed to download document. No document created yet.");
        
        // INFO: disabled in prod until I find a way to install ms word on linux
        // case: pdf
        if (pdf && !ENV.equals("prod"))
            file = convertDocxToPdf(file);

        try {
            return ResponseEntity.ok()
                                .headers(getDownloadHeaders(this.documentWrapper.getFileName()))
                                .contentLength(file.length())
                                .contentType(MediaType.parseMediaType("application/octet-stream"))
                                .body(os -> {
                                    try {
                                        Files.copy(file.toPath(), os);

                                    } finally {
                                        file.delete();
                                        this.documentWrapper.getPictures().clear();
                                    }
                                });

        } finally {
            log.info("Download finished");
        }
    }


    /**
     * Upload a {@link MultipartFile} file and add it to {@code this.documentWrapper}.
     * 
     * @param picture picture as multipart file
     */
    @PostMapping(path = "/uploadPicture", consumes = "multipart/form-data")
    @Operation(summary = "Upload a picture as multipart file to filesystem in backend.")
    public ApiExceptionFormat uploadFile(@RequestBody @NotNull(message = "Failed to upload picture. 'file' cannot be null.") MultipartFile picture) {

        log.info("Starting to upload files...");

        String fileName = picture.getOriginalFilename();
        // case: not a picture
        if (PictureUtils.getPictureType(fileName) == null) 
            throw new ApiException(UNPROCESSABLE_ENTITY, "Failed to upload picture. File " + fileName + " is not recognized as picture.");

        String completeFileName = PICTURES_FOLDER + prependSlash(fileName);
        try (OutputStream os = new FileOutputStream(completeFileName);
             InputStream is = picture.getInputStream()) {
            
            // write to file
            os.write(is.readAllBytes());

            // check file exists
            File uploadedFile = new File(completeFileName);
            if (!uploadedFile.exists()) 
                throw new ApiException("Failed to write stream to file.");

            // updated document
            this.documentWrapper.getPictures().put(fileName, Utils.fileToByteArray(uploadedFile));

        } catch (Exception e) {
            throw new ApiException("Failed to upload picture.", e);

        } finally {
            // clean up
            Utils.clearFolderByFileName(PICTURES_FOLDER, fileName);
            
            log.info("Upload finished");
        }

        return ApiExceptionHandler.returnPrettySuccess(OK);
    }


    /**
     * Extracts csrf token from session of given http request.
     * 
     * @return csrf token from request header or {@code ""} if null
     */
    @GetMapping("/getCsrfToken")
    @Operation(summary = "Extracts csrf token from session of given http request and returns token string or '' if token is null.")
    public String getCsrfToken(CsrfToken csrfToken) {

        return csrfToken == null ? "" : csrfToken.getToken();
    }


    /**
     * Build document with {@code this.documentWrapper} and write to file
     * 
     * @return generated .docx file
     */
    private File buildAndWriteDocument() {

        DocumentBuilder documentBuilder = new DocumentBuilder(this.documentWrapper.getContent(), 
                                                                this.documentWrapper.getFileName(), 
                                                                this.documentWrapper.getNumColumns(),
                                                                this.documentWrapper.getNumSingleColumnLines(),
                                                                this.documentWrapper.isLandscape(),
                                                                this.documentWrapper.getPictures(),
                                                                this.documentWrapper.getTableConfigs());
        
        // build
        return documentBuilder.build().writeDocxFile();
    }


    /**
     * Convert given '.docx' file to pdf.
     *  
     * @param docxFile ending on '.docx' to convert to '.pdf'
     */
    private File convertDocxToPdf(File docxFile) {

        String pdfFileName = docxFile.getName();

        return DocumentBuilder.docxToPdfDocuments4j(docxFile, pdfFileName);
    }


    /**
     * Create http headers for the download request.
     * 
     * @param fileName to use for the downloaded file.
     * @return {@link HttpHeaders} object.
     */
    private HttpHeaders getDownloadHeaders(String fileName) {

        HttpHeaders header = new HttpHeaders();

        header.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + fileName);
        header.add(HttpHeaders.CACHE_CONTROL, "no-cache, no-store, must-revalidate");
        header.add(HttpHeaders.PRAGMA, "no-cache");
        header.add(HttpHeaders.EXPIRES, "0");

        return header;
    }
}