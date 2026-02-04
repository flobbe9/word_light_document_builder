package de.word_light.document_builder.controllers;

import static org.springframework.http.HttpStatus.NOT_IMPLEMENTED;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.info.OsInfo;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
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


    /**
     * Builds word document, writes to .docx file. <p>
     * 
     * Assuming that: <p>
     * first {@link BasicParagraph} is the header <p>
     * last {@link BasicParagraph} is the footer <p>
     * anything in between is main content <p>.
     * 
     * Clears {@code this.documentWrapper.getPictures()} after download (successful or not).
     * 
     * @param pdf optional. Set to {@code true} in order to convert the generated document to pdf
     * @param wrapper to use for downloaded file
     * @return {@link StreamingResponseBody} of file with correct headers for download
     */
    @PostMapping(path = "/buildAndDownload", produces = {MediaType.APPLICATION_OCTET_STREAM_VALUE, MediaType.APPLICATION_JSON_VALUE})
    @Operation(summary = "Write given wrapper to .docx, optionally convert to pdf and then download the file")
    public ResponseEntity<StreamingResponseBody> buildAndDownload(
        @RequestParam("pdf") Optional<Boolean> pdf,
        @RequestBody @Valid DocumentWrapper wrapper
    ) {
        // pictures may have been uploaded before
        wrapper.setPictures(this.documentWrapper.getPictures());

        this.documentWrapper = wrapper;

        // build docx
        AtomicReference<ByteArrayOutputStream> bos = new AtomicReference<>(buildAndWriteDocument());

        // convert to pdf possibly
        boolean isPdf = pdf.orElse(false);
        if (isPdf)
            bos.set(convertDocxToPdf(bos.get()));
        
        String fileName = this.documentWrapper.getFileName();
        if (isPdf)
            fileName = fileName.replace(".docx", ".pdf");

        log.info("Downloading {}", fileName);

        try {
            return ResponseEntity.ok()
                .headers(getDownloadHeaders(fileName))
                .contentLength(bos.get().size())
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(os -> {
                    bos.get().writeTo(os);
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
    public ApiExceptionFormat uploadFile(@RequestParam("picture") @NotNull(message = "Failed to upload picture. 'file' cannot be null.") MultipartFile picture) {
        log.info("Starting to upload files...");

        String fileName = picture.getOriginalFilename();
        log.debug("Uploading file {}", fileName);

        // case: not a picture
        if (PictureUtils.getPictureType(fileName) == null) 
            throw new ApiException(UNPROCESSABLE_ENTITY, "Failed to upload picture. File " + fileName + " is not recognized as picture.");

        try (InputStream is = picture.getInputStream()) {
            this.documentWrapper.getPictures().put(fileName, is.readAllBytes());

            log.info("Upload finished");

        } catch (Exception e) {
            throw new ApiException("Failed to upload picture.", e);
        }

        return ApiExceptionHandler.returnPrettySuccess(OK);
    }

    /**
     * Build document with {@code this.documentWrapper} and write to stream
     * 
     * @return generated .docx outputStream
     */
    private ByteArrayOutputStream buildAndWriteDocument() {
        DocumentBuilder documentBuilder = new DocumentBuilder(
            this.documentWrapper.getContent(), 
            this.documentWrapper.getFileName(), 
            this.documentWrapper.getNumColumns(),
            this.documentWrapper.getNumSingleColumnLines(),
            this.documentWrapper.isLandscape(),
            this.documentWrapper.getPictures(),
            this.documentWrapper.getTableConfigs()
        );
        
        return documentBuilder.build().writeDocx();
    }

    /**
     * Convert given '.docx' file to pdf.
     *  
     * @param docxFile ending on '.docx' to convert to '.pdf'
     */
    private ByteArrayOutputStream convertDocxToPdf(ByteArrayOutputStream docxOs) {
        if (Utils.isWindowsOs()) {
            return DocumentBuilder.docxToPdfDocuments4j(docxOs);

        } else if (Utils.isLinuxOs())
            return DocumentBuilder.docxToPdfLibreOffice(docxOs);

        throw new ApiException(NOT_IMPLEMENTED, "No pdf converter implemented for current OS '%s'".formatted(new OsInfo().getName()));
    }

    /**
     * Create http headers for the download request. These make sure the file name is passed correctly and that the browser
     * is downloading the file instead of trying to display it. 
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