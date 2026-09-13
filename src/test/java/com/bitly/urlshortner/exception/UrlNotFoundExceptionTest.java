package com.bitly.urlshortner.exception;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link UrlNotFoundException}.
 */
class UrlNotFoundExceptionTest {

    @Test
    void constructor_shouldIncludeHashInMessage() {
        UrlNotFoundException ex = new UrlNotFoundException("abc1234");
        assertThat(ex.getMessage()).contains("abc1234");
    }

    @Test
    void constructor_shouldBeRuntimeException() {
        assertThat(new UrlNotFoundException("xyz")).isInstanceOf(RuntimeException.class);
    }

    @Test
    void shouldBeThrownAndCaughtAsRuntimeException() {
        assertThatThrownBy(() -> {
            throw new UrlNotFoundException("test-hash");
        })
                .isInstanceOf(UrlNotFoundException.class)
                .hasMessageContaining("test-hash");
    }
}
