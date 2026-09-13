package com.bitly.urlshortner.controller;

import com.bitly.urlshortner.controller.dto.UrlConversionRequest;
import com.bitly.urlshortner.controller.dto.UrlConversionResponse;
import com.bitly.urlshortner.dao.model.Url;
import com.bitly.urlshortner.dao.postgres.UrlsRepository;
import com.bitly.urlshortner.exception.UrlNotFoundException;
import com.bitly.urlshortner.service.UrlConversion.UrlConversionService;
import com.bitly.urlshortner.utils.RedisService;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@RequestMapping
public class UrlconversionController {

    @Resource
    private UrlConversionService urlConversionService;

    @Resource
    private UrlsRepository urlsRepository;

    @Autowired
    private RedisService redisService;

    /**
     * Shortens a long URL.
     * Responds 200 with the shortened URL on success.
     * Responds 400 if url or userId is missing/blank/invalid (handled by GlobalExceptionHandler).
     */
    @PostMapping("/api/v1/shorturl")
    public ResponseEntity<UrlConversionResponse> shortUrl(
            @Valid @RequestBody UrlConversionRequest urlConversionRequest) {

        UrlConversionResponse response = urlConversionService.shortUrl(urlConversionRequest);
        return ResponseEntity.ok(response);
    }

    /**
     * Redirects a short URL hash to its original URL.
     * Responds 302 Found with Location header on success.
     * Responds 400 if the stored URL has an unsafe scheme (fix #5).
     * Responds 404 if the hash does not exist (handled by GlobalExceptionHandler).
     */
    @GetMapping("/{hash}")
    public ResponseEntity<Void> redirect(@PathVariable String hash) {

        String originalUrl = redisService.get(hash);

        if (originalUrl == null) {
            Url url = urlsRepository.findById(hash)
                    .orElseThrow(() -> new UrlNotFoundException(hash));

            originalUrl = url.getOriginalUrl();
            redisService.save(hash, originalUrl);
        }

        return ResponseEntity
                .status(HttpStatus.FOUND)
                .location(toSafeUri(originalUrl))
                .build();
    }

    /**
     * Validates that the URL scheme is http or https before building a URI.
     * Prevents open redirects via javascript:, file://, data:, etc. that may
     * have been stored before input validation was in place (fix for issue #5).
     *
     * @throws IllegalArgumentException if the scheme is not http or https
     */
    private URI toSafeUri(String url) {
        if (url == null || (!url.startsWith("http://") && !url.startsWith("https://"))) {
            throw new IllegalArgumentException(
                    "Stored URL has an unsafe or unsupported scheme and cannot be redirected."
            );
        }
        return URI.create(url);
    }
}

