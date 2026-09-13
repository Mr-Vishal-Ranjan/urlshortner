package com.bitly.urlshortner.controller.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UrlConversionRequest {

    @NotBlank(message = "url must not be blank")
    @Size(max = 2048, message = "url must not exceed 2048 characters")
    @Pattern(
            regexp = "^https?://.*",
            message = "url must start with http:// or https://"
    )
    private String url;

    @NotBlank(message = "userId must not be blank")
    @Size(max = 255, message = "userId must not exceed 255 characters")
    private String userId;
}

