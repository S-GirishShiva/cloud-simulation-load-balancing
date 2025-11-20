package com.cloudsimulation.utils;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Provides security validation for file paths to prevent directory traversal attacks.
 * All file path inputs from external sources should be sanitized using this validator.
 */
public class FileSecurityValidator {

    /**
     * Sanitizes a file path by removing directory traversal sequences and normalizing.
     * Prevents attacks like "../../../etc/passwd" or similar path traversal attempts.
     *
     * @param input The input path string to sanitize
     * @return Sanitized path string safe for file operations
     * @throws IllegalArgumentException if the input path is null or contains only traversal sequences
     */
    public static String sanitizePath(String input) {
        if (input == null || input.trim().isEmpty()) {
            throw new IllegalArgumentException("Path cannot be null or empty");
        }

        // Remove leading/trailing whitespace
        String sanitized = input.trim();

        // Normalize the path to resolve ".." and "." sequences
        Path path = Paths.get(sanitized).normalize();

        // Convert back to string and remove any remaining "../" sequences
        sanitized = path.toString().replace("..", "");

        // Remove leading slashes to prevent absolute path attacks
        while (sanitized.startsWith("/") || sanitized.startsWith("\\")) {
            sanitized = sanitized.substring(1);
        }

        // If sanitization removed everything, path was malicious
        if (sanitized.isEmpty()) {
            throw new IllegalArgumentException("Invalid path: contains only traversal sequences");
        }

        return sanitized;
    }
}
