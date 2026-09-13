package com.bitly.urlshortner.exception;

/**
 * Thrown when a short URL hash cannot be found in the database.
 * Results in a 404 response via GlobalExceptionHandler.
 */
public class UrlNotFoundException extends RuntimeException {

    public UrlNotFoundException(String hash) {
        super("Short URL not found.");
    }
}
