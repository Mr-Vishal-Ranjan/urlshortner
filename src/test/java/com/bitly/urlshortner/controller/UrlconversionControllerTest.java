package com.bitly.urlshortner.controller;

import com.bitly.urlshortner.controller.dto.UrlConversionRequest;
import com.bitly.urlshortner.controller.dto.UrlConversionResponse;
import com.bitly.urlshortner.dao.model.Url;
import com.bitly.urlshortner.dao.postgres.UrlsRepository;
import com.bitly.urlshortner.exception.GlobalExceptionHandler;
import com.bitly.urlshortner.exception.UrlNotFoundException;
import com.bitly.urlshortner.service.UrlConversion.UrlConversionService;
import com.bitly.urlshortner.utils.RedisService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Slice tests for {@link UrlconversionController}.
 * Uses @WebMvcTest so only the web layer is loaded – services and
 * repositories are mocked with @MockitoBean.
 */
@WebMvcTest(UrlconversionController.class)
@Import(GlobalExceptionHandler.class)
class UrlconversionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UrlConversionService urlConversionService;

    @MockitoBean
    private UrlsRepository urlsRepository;

    @MockitoBean
    private RedisService redisService;

    // ==========================================================
    // POST /api/v1/shorturl – Happy path
    // ==========================================================

    @Test
    void shortUrl_withValidRequest_shouldReturn200AndResponse() throws Exception {
        UrlConversionRequest request = UrlConversionRequest.builder()
                .url("https://example.com/some/long/path")
                .userId("user-42")
                .build();

        UrlConversionResponse mockResponse = UrlConversionResponse.builder()
                .originalUrl(request.getUrl())
                .shortUrl("https://bitly.com/abc1234")
                .userId(request.getUserId())
                .createdAt(System.currentTimeMillis())
                .build();

        when(urlConversionService.shortUrl(any(UrlConversionRequest.class)))
                .thenReturn(mockResponse);

        mockMvc.perform(post("/api/v1/shorturl")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.shortUrl").value("https://bitly.com/abc1234"))
                .andExpect(jsonPath("$.originalUrl").value(request.getUrl()))
                .andExpect(jsonPath("$.userId").value("user-42"));
    }

    // ==========================================================
    // POST /api/v1/shorturl – Validation: url is blank
    // ==========================================================

    @Test
    void shortUrl_withBlankUrl_shouldReturn400WithValidationError() throws Exception {
        UrlConversionRequest request = UrlConversionRequest.builder()
                .url("")          // blank – violates @NotBlank
                .userId("user-1")
                .build();

        mockMvc.perform(post("/api/v1/shorturl")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.errors", hasItem(containsString("url"))));
    }

    @Test
    void shortUrl_withNullUrl_shouldReturn400WithValidationError() throws Exception {
        UrlConversionRequest request = UrlConversionRequest.builder()
                .url(null)        // null – violates @NotBlank
                .userId("user-1")
                .build();

        mockMvc.perform(post("/api/v1/shorturl")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.errors", hasItem(containsString("url"))));
    }

    // ==========================================================
    // POST /api/v1/shorturl – Validation: userId is blank
    // ==========================================================

    @Test
    void shortUrl_withBlankUserId_shouldReturn400WithValidationError() throws Exception {
        UrlConversionRequest request = UrlConversionRequest.builder()
                .url("https://example.com")
                .userId("")       // blank – violates @NotBlank
                .build();

        mockMvc.perform(post("/api/v1/shorturl")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.errors", hasItem(containsString("userId"))));
    }

    @Test
    void shortUrl_withNullUserId_shouldReturn400WithValidationError() throws Exception {
        UrlConversionRequest request = UrlConversionRequest.builder()
                .url("https://example.com")
                .userId(null)     // null – violates @NotBlank
                .build();

        mockMvc.perform(post("/api/v1/shorturl")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.errors", hasItem(containsString("userId"))));
    }

    // ==========================================================
    // POST /api/v1/shorturl – Validation: both fields missing
    // ==========================================================

    @Test
    void shortUrl_withBothFieldsMissing_shouldReturn400WithTwoErrors() throws Exception {
        UrlConversionRequest request = UrlConversionRequest.builder()
                .url(null)
                .userId(null)
                .build();

        mockMvc.perform(post("/api/v1/shorturl")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.errors", hasSize(2)));
    }

    // ==========================================================
    // POST /api/v1/shorturl – Missing/malformed body
    // ==========================================================

    @Test
    void shortUrl_withMissingBody_shouldReturn400() throws Exception {
        mockMvc.perform(post("/api/v1/shorturl")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Request body is missing or malformed"));
    }

    @Test
    void shortUrl_withMalformedJson_shouldReturn400() throws Exception {
        mockMvc.perform(post("/api/v1/shorturl")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ this is not valid json }"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Request body is missing or malformed"));
    }

    // ==========================================================
    // POST /api/v1/shorturl – Error response structure
    // ==========================================================

    @Test
    void shortUrl_errorResponse_shouldContainTimestampStatusAndError() throws Exception {
        UrlConversionRequest request = UrlConversionRequest.builder()
                .url(null)
                .userId("user-1")
                .build();

        mockMvc.perform(post("/api/v1/shorturl")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").isNumber())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"));
    }

    // ==========================================================
    // GET /{hash} – Happy path (redirect)
    // ==========================================================

    @Test
    void redirect_withValidHash_shouldReturn302WithLocationHeader() throws Exception {
        Url url = new Url();
        url.setBase64Hash("abc1234");
        url.setOriginalUrl("https://example.com/original");
        url.setUserId("user-1");
        url.setCreatedAt(System.currentTimeMillis());
        url.setUpdatedAt(url.getCreatedAt());

        when(redisService.get("abc1234")).thenReturn(null); // cache miss → go to DB
        when(urlsRepository.findById("abc1234")).thenReturn(Optional.of(url));

        mockMvc.perform(get("/abc1234"))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", "https://example.com/original"));
    }

    // ==========================================================
    // GET /{hash} – Hash not found
    // ==========================================================

    @Test
    void redirect_withUnknownHash_shouldReturn404WithJsonError() throws Exception {
        when(redisService.get("unknown7")).thenReturn(null); // cache miss → go to DB
        when(urlsRepository.findById("unknown7")).thenReturn(Optional.empty());

        mockMvc.perform(get("/unknown7"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Short URL not found."));
    }

    @Test
    void redirect_notFound_shouldContainTimestamp() throws Exception {
        when(redisService.get("missing1")).thenReturn(null);
        when(urlsRepository.findById("missing1")).thenReturn(Optional.empty());

        mockMvc.perform(get("/missing1"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.timestamp").isNumber());
    }

    // ==========================================================
    // GET /{hash} – Invalid URI stored in DB (IllegalArgumentException)
    // ==========================================================

    @Test
    void redirect_withInvalidStoredUrl_shouldReturn400() throws Exception {
        Url url = new Url();
        url.setBase64Hash("bad1234");
        url.setOriginalUrl("not a valid uri %%%");  // URI.create() will throw
        url.setUserId("user-x");
        url.setCreatedAt(System.currentTimeMillis());
        url.setUpdatedAt(url.getCreatedAt());

        when(redisService.get("bad1234")).thenReturn(null);
        when(urlsRepository.findById("bad1234")).thenReturn(Optional.of(url));

        mockMvc.perform(get("/bad1234"))
                .andExpect(status().isBadRequest());
    }
}
