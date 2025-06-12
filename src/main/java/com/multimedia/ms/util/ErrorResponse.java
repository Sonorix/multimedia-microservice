package com.multimedia.ms.util;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Standardized error response structure
 */
public class ErrorResponse {
    private int status;
    private String message;
    private String path;
    private String timestamp;
    
    /**
     * Create a new error response
     * 
     * @param status HTTP status code
     * @param message Error message
     * @param path Request path that caused the error
     */
    public ErrorResponse(int status, String message, String path) {
        this.status = status;
        this.message = message;
        this.path = path;
        this.timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME);
    }
    
    /**
     * Convert this error response to a JSON string
     * 
     * @return JSON string representation of this error
     */
    public String toJson() {
        JsonObject json = Json.createObjectBuilder()
            .add("status", status)
            .add("message", message)
            .add("path", path)
            .add("timestamp", timestamp)
            .build();
        
        return json.toString();
    }
}
