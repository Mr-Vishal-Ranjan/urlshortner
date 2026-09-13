package com.bitly.urlshortner.service;

import com.bitly.urlshortner.controller.dto.UrlConversionRequest;
import com.bitly.urlshortner.controller.dto.UrlConversionResponse;
import com.bitly.urlshortner.dao.model.Url;
import com.bitly.urlshortner.dao.postgres.UrlsRepository;
import com.bitly.urlshortner.service.UrlConversion.UrlConversionService;
import com.bitly.urlshortner.utils.Base64HashGenrator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link UrlConversionService}.
 * Repository and hash generator are mocked – no DB or Spring context needed.
 */
@ExtendWith(MockitoExtension.class)
class UrlConversionServiceTest {

    @Mock
    private UrlsRepository urlsRepository;

    @Mock
    private Base64HashGenrator base64HashGenrator;

    @InjectMocks
    private UrlConversionService urlConversionService;

    private static final String HASH_A   = "aaaaaaa";
    private static final String HASH_B   = "bbbbbbb";
    private static final String BASE_URL = "https://bitly.com/";

    private UrlConversionRequest validRequest() {
        return UrlConversionRequest.builder()
                .url("https://example.com/very/long/url")
                .userId("user-42")
                .build();
    }

    private Url buildSavedUrl(String hash, UrlConversionRequest req) {
        Url saved = new Url();
        saved.setOriginalUrl(req.getUrl());
        saved.setBase64Hash(hash);
        saved.setUserId(req.getUserId());
        saved.setCreatedAt(System.currentTimeMillis());
        saved.setUpdatedAt(saved.getCreatedAt());
        return saved;
    }

    // ------------------------------------------------------------------
    // shortUrl – happy path (hash is unique on first attempt)
    // ------------------------------------------------------------------

    @Test
    void shortUrl_withUniqueHashOnFirstAttempt_shouldReturnCorrectResponse() {
        UrlConversionRequest request = validRequest();
        Url saved = buildSavedUrl(HASH_A, request);

        when(base64HashGenrator.generate()).thenReturn(HASH_A);
        when(urlsRepository.existsById(HASH_A)).thenReturn(false);   // unique
        when(urlsRepository.save(any(Url.class))).thenReturn(saved);

        UrlConversionResponse response = urlConversionService.shortUrl(request);

        assertThat(response.getShortUrl()).isEqualTo(BASE_URL + HASH_A);
        assertThat(response.getOriginalUrl()).isEqualTo(request.getUrl());
        assertThat(response.getUserId()).isEqualTo(request.getUserId());
        assertThat(response.getCreatedAt()).isNotNull();
    }

    @Test
    void shortUrl_shouldCallExistsByIdBeforeSaving() {
        UrlConversionRequest request = validRequest();
        Url saved = buildSavedUrl(HASH_A, request);

        when(base64HashGenrator.generate()).thenReturn(HASH_A);
        when(urlsRepository.existsById(HASH_A)).thenReturn(false);
        when(urlsRepository.save(any(Url.class))).thenReturn(saved);

        urlConversionService.shortUrl(request);

        // existsById must be called before save
        var inOrder = inOrder(urlsRepository);
        inOrder.verify(urlsRepository).existsById(HASH_A);
        inOrder.verify(urlsRepository).save(any(Url.class));
    }

    @Test
    void shortUrl_shouldCallHashGeneratorOnce_whenFirstHashIsUnique() {
        UrlConversionRequest request = validRequest();
        Url saved = buildSavedUrl(HASH_A, request);

        when(base64HashGenrator.generate()).thenReturn(HASH_A);
        when(urlsRepository.existsById(HASH_A)).thenReturn(false);
        when(urlsRepository.save(any(Url.class))).thenReturn(saved);

        urlConversionService.shortUrl(request);

        verify(base64HashGenrator, times(1)).generate();
    }

    // ------------------------------------------------------------------
    // shortUrl – collision retry: first hash collides, second is unique
    // ------------------------------------------------------------------

    @Test
    void shortUrl_withCollisionOnFirstHash_shouldRetryAndUseSecondHash() {
        UrlConversionRequest request = validRequest();
        Url saved = buildSavedUrl(HASH_B, request);

        // First call returns colliding hash, second returns unique hash
        when(base64HashGenrator.generate())
                .thenReturn(HASH_A)   // attempt 1 – collides
                .thenReturn(HASH_B);  // attempt 2 – unique

        when(urlsRepository.existsById(HASH_A)).thenReturn(true);   // collision
        when(urlsRepository.existsById(HASH_B)).thenReturn(false);  // free
        when(urlsRepository.save(any(Url.class))).thenReturn(saved);

        UrlConversionResponse response = urlConversionService.shortUrl(request);

        assertThat(response.getShortUrl()).isEqualTo(BASE_URL + HASH_B);
        verify(base64HashGenrator, times(2)).generate();
    }

    @Test
    void shortUrl_withCollision_savedEntityShouldUseUniqueHash() {
        UrlConversionRequest request = validRequest();
        Url saved = buildSavedUrl(HASH_B, request);

        when(base64HashGenrator.generate())
                .thenReturn(HASH_A)
                .thenReturn(HASH_B);
        when(urlsRepository.existsById(HASH_A)).thenReturn(true);
        when(urlsRepository.existsById(HASH_B)).thenReturn(false);
        when(urlsRepository.save(any(Url.class))).thenAnswer(invocation -> {
            Url urlArg = invocation.getArgument(0);
            // Must persist the non-colliding hash
            assertThat(urlArg.getBase64Hash()).isEqualTo(HASH_B);
            return saved;
        });

        urlConversionService.shortUrl(request);
    }

    // ------------------------------------------------------------------
    // shortUrl – all retries exhausted (5 consecutive collisions)
    // ------------------------------------------------------------------

    @Test
    void shortUrl_whenAllRetriesExhausted_shouldThrowIllegalStateException() {
        UrlConversionRequest request = validRequest();

        // Every generated hash already exists
        when(base64HashGenrator.generate()).thenReturn(HASH_A);
        when(urlsRepository.existsById(HASH_A)).thenReturn(true);

        assertThatThrownBy(() -> urlConversionService.shortUrl(request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Could not generate a unique short URL hash");
    }

    @Test
    void shortUrl_whenAllRetriesExhausted_shouldNeverCallSave() {
        UrlConversionRequest request = validRequest();

        when(base64HashGenrator.generate()).thenReturn(HASH_A);
        when(urlsRepository.existsById(anyString())).thenReturn(true);

        try {
            urlConversionService.shortUrl(request);
        } catch (IllegalStateException ignored) { /* expected */ }

        verify(urlsRepository, never()).save(any());
    }

    @Test
    void shortUrl_whenAllRetriesExhausted_shouldCallGenerateMaxFiveTimes() {
        UrlConversionRequest request = validRequest();

        when(base64HashGenrator.generate()).thenReturn(HASH_A);
        when(urlsRepository.existsById(anyString())).thenReturn(true);

        try {
            urlConversionService.shortUrl(request);
        } catch (IllegalStateException ignored) { /* expected */ }

        verify(base64HashGenrator, times(5)).generate();
    }

    // ------------------------------------------------------------------
    // shortUrl – saved entity field verification
    // ------------------------------------------------------------------

    @Test
    void shortUrl_savedEntityShouldHaveAllFieldsPopulated() {
        UrlConversionRequest request = validRequest();
        Url saved = buildSavedUrl(HASH_A, request);

        when(base64HashGenrator.generate()).thenReturn(HASH_A);
        when(urlsRepository.existsById(HASH_A)).thenReturn(false);
        when(urlsRepository.save(any(Url.class))).thenAnswer(invocation -> {
            Url urlArg = invocation.getArgument(0);
            assertThat(urlArg.getOriginalUrl()).isEqualTo(request.getUrl());
            assertThat(urlArg.getUserId()).isEqualTo(request.getUserId());
            assertThat(urlArg.getBase64Hash()).isEqualTo(HASH_A);
            assertThat(urlArg.getCreatedAt()).isNotNull();
            assertThat(urlArg.getUpdatedAt()).isNotNull();
            return saved;
        });

        urlConversionService.shortUrl(request);
    }
}
