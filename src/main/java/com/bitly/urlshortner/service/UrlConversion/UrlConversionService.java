package com.bitly.urlshortner.service.UrlConversion;

import com.bitly.urlshortner.controller.dto.UrlConversionRequest;
import com.bitly.urlshortner.controller.dto.UrlConversionResponse;
import com.bitly.urlshortner.dao.model.Url;
import com.bitly.urlshortner.dao.postgres.UrlsRepository;
import com.bitly.urlshortner.utils.Base64HashGenrator;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class UrlConversionService {

    @Value("${app.base-url}")
    private String baseUrl;

    private static final int MAX_RETRIES = 5;

    @Resource
    private UrlsRepository urlsRepository;

    @Resource
    private Base64HashGenrator base64HashGenrator;

    public UrlConversionResponse shortUrl(UrlConversionRequest urlConversionRequest) {

        // Generate a unique hash – retry if there is a collision in the DB
        String hash = generateUniqueHash();

        Long currentTime = System.currentTimeMillis();

        Url url = new Url();
        url.setOriginalUrl(urlConversionRequest.getUrl());
        url.setCreatedAt(currentTime);
        url.setUserId(urlConversionRequest.getUserId());
        url.setBase64Hash(hash);
        url.setUpdatedAt(currentTime);

        Url savedUrl = urlsRepository.save(url);

        return UrlConversionResponse.builder()
                .originalUrl(savedUrl.getOriginalUrl())
                .shortUrl(baseUrl + savedUrl.getBase64Hash())
                .userId(savedUrl.getUserId())
                .createdAt(savedUrl.getCreatedAt())
                .build();
    }

    /**
     * Generates a hash that does not already exist in the database.
     * Retries up to {@value MAX_RETRIES} times before giving up.
     *
     * @return a unique 7-character hash
     * @throws IllegalStateException if all retry attempts produce collisions
     */
    private String generateUniqueHash() {
        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            String hash = base64HashGenrator.generate();

            if (!urlsRepository.existsById(hash)) {
                return hash;   // hash is free – proceed with this one
            }
            // hash already used – try again
        }
        throw new IllegalStateException(
                "Could not generate a unique short URL hash after " + MAX_RETRIES + " attempts."
        );
    }
}

