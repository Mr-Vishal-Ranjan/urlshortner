package com.bitly.urlshortner.utils;

import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link Base64HashGenrator}.
 * No Spring context needed – plain instantiation.
 */
class Base64HashGenratorTest {

    private final Base64HashGenrator generator = new Base64HashGenrator();

    // Valid characters used by the generator
    private static final String VALID_CHARS =
            "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_";

    @Test
    void generate_shouldReturnSevenCharacterString() {
        String hash = generator.generate();
        assertThat(hash).hasSize(7);
    }

    @Test
    void generate_shouldNotReturnNull() {
        assertThat(generator.generate()).isNotNull();
    }

    @Test
    void generate_shouldOnlyContainValidCharacters() {
        String hash = generator.generate();
        for (char c : hash.toCharArray()) {
            assertThat(VALID_CHARS).contains(String.valueOf(c));
        }
    }

    @RepeatedTest(5)
    void generate_shouldReturnDifferentHashesOnRepeatedCalls() {
        // Collect 50 hashes and assert at least some are unique
        Set<String> hashes = new HashSet<>();
        for (int i = 0; i < 50; i++) {
            hashes.add(generator.generate());
        }
        // With 64^7 possibilities, 50 draws must all be unique
        assertThat(hashes).hasSize(50);
    }
}
