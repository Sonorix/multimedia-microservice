package com.multimedia.ms.util;

import org.bson.types.ObjectId;
import java.util.Collection;
import java.util.regex.Pattern;

/**
 * Input validation utility for the application
 */
public class Validator {

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@(.+)$");
    private static final int MAX_TEXT_LENGTH = 1000; // Max length for text fields like bio
    private static final int MAX_SHORT_TEXT_LENGTH = 255; // Max length for shorter text fields
    
    /**
     * Check if a string is null or empty
     * 
     * @param value The string to check
     * @return true if the string is null or empty
     */
    public static boolean isEmpty(String value) {
        return value == null || value.trim().isEmpty();
    }
    
    /**
     * Validate that a required string is not null or empty
     * 
     * @param value The string to validate
     * @param fieldName The name of the field for error message
     * @throws IllegalArgumentException if validation fails
     */
    public static void validateRequired(String value, String fieldName) {
        if (isEmpty(value)) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
    }
    
    /**
     * Validate that a string doesn't exceed a maximum length
     * 
     * @param value The string to validate
     * @param maxLength The maximum allowed length
     * @param fieldName The name of the field for error message
     * @throws IllegalArgumentException if validation fails
     */
    public static void validateMaxLength(String value, int maxLength, String fieldName) {
        if (value != null && value.length() > maxLength) {
            throw new IllegalArgumentException(fieldName + " exceeds maximum length of " + maxLength + " characters");
        }
    }
    
    /**
     * Validate an email address format
     * 
     * @param email The email to validate
     * @param fieldName The name of the field for error message
     * @throws IllegalArgumentException if validation fails
     */
    public static void validateEmail(String email, String fieldName) {
        validateRequired(email, fieldName);
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            throw new IllegalArgumentException("Invalid email format");
        }
    }
    
    /**
     * Validate that a string is a valid ObjectId
     * 
     * @param id The ID to validate
     * @param fieldName The name of the field for error message
     * @throws IllegalArgumentException if validation fails
     */
    public static void validateObjectId(String id, String fieldName) {
        validateRequired(id, fieldName);
        if (!ObjectId.isValid(id)) {
            throw new IllegalArgumentException("Invalid " + fieldName + " format");
        }
    }
    
    /**
     * Validate a rating value (1-5)
     * 
     * @param rating The rating value
     * @throws IllegalArgumentException if validation fails
     */
    public static void validateRating(double rating) {
        if (rating < 1 || rating > 5) {
            throw new IllegalArgumentException("Rating must be between 1 and 5");
        }
    }
    
    /**
     * Validate that a collection is not null or empty
     * 
     * @param collection The collection to validate
     * @param fieldName The name of the field for error message
     * @throws IllegalArgumentException if validation fails
     */
    public static void validateNotEmpty(Collection<?> collection, String fieldName) {
        if (collection == null || collection.isEmpty()) {
            throw new IllegalArgumentException(fieldName + " cannot be empty");
        }
    }
    
    /**
     * Validate a JSON string for structure/format
     * 
     * @param json The JSON string to validate
     * @param fieldName The name of the field for error message
     * @throws IllegalArgumentException if validation fails
     */
    public static void validateJsonFormat(String json, String fieldName) {
        validateRequired(json, fieldName);
        
        // Simple check if it starts with { and ends with }
        if (!json.trim().startsWith("{") || !json.trim().endsWith("}")) {
            throw new IllegalArgumentException("Invalid JSON format for " + fieldName);
        }
    }
    
    /**
     * Validate general text input (bio, description, etc.)
     * 
     * @param text The text to validate
     * @param fieldName The name of the field for error message
     * @throws IllegalArgumentException if validation fails
     */
    public static void validateText(String text, String fieldName) {
        if (text != null) {
            validateMaxLength(text, MAX_TEXT_LENGTH, fieldName);
        }
    }
    
    /**
     * Validate short text input (name, title, etc.)
     * 
     * @param text The text to validate
     * @param fieldName The name of the field for error message
     * @throws IllegalArgumentException if validation fails
     */
    public static void validateShortText(String text, String fieldName) {
        validateRequired(text, fieldName);
        validateMaxLength(text, MAX_SHORT_TEXT_LENGTH, fieldName);
    }
    
    /**
     * Validate file size against maximum allowed size
     * 
     * @param fileSize The file size in bytes
     * @param maxFileSize The maximum allowed file size in bytes
     * @throws IllegalArgumentException if validation fails
     */
    public static void validateFileSize(long fileSize, long maxFileSize) {
        if (fileSize <= 0) {
            throw new IllegalArgumentException("File size must be positive");
        }
        
        if (fileSize > maxFileSize) {
            throw new IllegalArgumentException("File size exceeds maximum allowed size of " + 
                    (maxFileSize / (1024 * 1024)) + " MB");
        }
    }
    
    /**
     * Validate that a file extension is in the allowed list
     * 
     * @param extension The file extension (without dot)
     * @param allowedExtensions Collection of allowed extensions
     * @throws IllegalArgumentException if validation fails
     */
    public static void validateFileExtension(String extension, Collection<String> allowedExtensions) {
        validateRequired(extension, "File extension");
        
        if (!allowedExtensions.contains(extension.toLowerCase())) {
            throw new IllegalArgumentException("File type not allowed. Allowed types: " + 
                    String.join(", ", allowedExtensions));
        }
    }
}
