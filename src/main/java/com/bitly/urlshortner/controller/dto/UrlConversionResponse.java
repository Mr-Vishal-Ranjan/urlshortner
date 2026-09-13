package com.bitly.urlshortner.controller.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UrlConversionResponse {
    private String originalUrl;
    private String shortUrl;
    private String userId;
    private Long createdAt;
}
