package de.word_light.document_builder.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import org.springframework.http.HttpStatus;
import org.springframework.lang.Nullable;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;

import de.word_light.document_builder.exception.ApiException;
import de.word_light.document_builder.exception.ApiExceptionFormat;


/**
 * Class defining some util methods for test classes.
 * 
 * @since 0.0.6
 */
public class TestUtils {

    private MockMvc mockMvc;
    private String baseUrl;


    public TestUtils() {}
    
    
    public TestUtils(MockMvc mockMvc, String baseUrl) {

        this.mockMvc = mockMvc;
        this.baseUrl = baseUrl;
    }


    /**
     * Perform http post request using {@link #APPLICATION_JSON} as content type and {@link #baseUrl}.
     * 
     * @param path to append to base url, no slashes are hard coded
     * @param body to use as payload
     * @param params to use in url
     * @return result
     * @throws Exception
     */
    public ResultActions performPost(String path, @Nullable Object body, @Nullable MultiValueMap<String, String> params) throws Exception {

        return this.mockMvc.perform(post(this.baseUrl + path)
                                    .contentType(APPLICATION_JSON)
                                    .params(params == null ? new LinkedMultiValueMap<>() : params)
                                    .content(body == null ? "" : Utils.objectToJson(body)));
    }
    

    /**
     * Perform http get request using {@link #APPLICATION_JSON} as content type and {@link #baseUrl}.
     * 
     * @param path to append to base url, no slashes are hard coded
     * @param params to use in url
     * @return result
     * @throws Exception
     */
    public ResultActions performGet(String path, @Nullable MultiValueMap<String, String> params) throws Exception {

        return this.mockMvc.perform(get(this.baseUrl + path)
                                    .params(params == null ? new LinkedMultiValueMap<>() : params)
                                    .contentType(APPLICATION_JSON));
    }


    /**
     * Assert that given json String is an {@link ApiExceptionFormat} object. Also check the {@code status} and {@code error} values match
     * the given HttpStatus.
     * 
     * @param json String formatted as json to check
     * @param status http status
     */
    public static void checkJsonApiExceptionFormat(String json, HttpStatus status) {

        JsonNode jsonNode = jsonToNode(json);

        assertEquals(4, jsonNode.size());

        assertTrue(jsonNode.has("status"));
        assertTrue(jsonNode.has("error"));
        assertTrue(jsonNode.has("message"));
        assertTrue(jsonNode.has("path"));

        assertEquals(status.value(), jsonNode.get("status").asInt());
        assertEquals(status.getReasonPhrase(), jsonNode.get("error").asText());
    }


    /**
     * Check given jsonResponse and assert {@link ApiExceptionFormat}s {@code returnPrettySuccess()} as result.
     * 
     * @param jsonResponse json response to check
     * @param status http status to assert
     */
    public static void checkApiExceptionFormatPrettySuccess(String jsonResponse, HttpStatus status) {
        assertTrue(jsonResponse.contains("\"error\":null"));

        // modify response for the sake of check method
        jsonResponse = jsonResponse.replace("null", "\"" + status.name() + "\"");
        checkJsonApiExceptionFormat(jsonResponse, OK);
    }


    /**
     * @param json string in json format
     * @return json string as {@link JsonNode} object
     * @throws ApiException if fails to read json string
     */
    public static JsonNode jsonToNode(String json) {

        ObjectReader objectReader = new ObjectMapper().reader();

        try {
            return objectReader.readTree(json);

        } catch (Exception e) {
            e.printStackTrace();
            throw new ApiException("Failed to convert object to json String.", e);
        }
    }
}
