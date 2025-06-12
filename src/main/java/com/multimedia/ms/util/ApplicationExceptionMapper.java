package com.multimedia.ms.util;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Global exception mapper that handles uncaught exceptions in the application
 * and returns standardized error responses.
 */
@Provider
public class ApplicationExceptionMapper implements ExceptionMapper<Throwable> {

    private static final Logger LOGGER = Logger.getLogger(ApplicationExceptionMapper.class.getName());
    
    @Context
    private UriInfo uriInfo;
    
    @Override
    public Response toResponse(Throwable exception) {
        // Log the exception
        LOGGER.log(Level.SEVERE, "Uncaught exception", exception);
        
        int status = Response.Status.INTERNAL_SERVER_ERROR.getStatusCode();
        String message = "An unexpected error occurred";
        
        // Handle WebApplicationException specially to preserve the status code
        if (exception instanceof WebApplicationException) {
            status = ((WebApplicationException) exception).getResponse().getStatus();
            
            // Use the exception message if available, or a default message based on status
            message = exception.getMessage();
            if (message == null || message.isEmpty()) {
                message = getDefaultMessageForStatus(status);
            }
        } else if (exception instanceof IllegalArgumentException) {
            // For validation errors
            status = Response.Status.BAD_REQUEST.getStatusCode();
            message = exception.getMessage();
        } else if (exception instanceof SecurityException) {
            // For security-related errors
            status = Response.Status.FORBIDDEN.getStatusCode();
            message = "Access denied";
        }
        
        String path = uriInfo != null ? uriInfo.getPath() : "unknown";
        ErrorResponse errorResponse = new ErrorResponse(status, message, path);
        
        return Response
                .status(status)
                .entity(errorResponse.toJson())
                .type(MediaType.APPLICATION_JSON)
                .build();
    }
    
    /**
     * Get a default message for common HTTP status codes
     * 
     * @param status HTTP status code
     * @return Default message for the status
     */
    private String getDefaultMessageForStatus(int status) {
        switch (status) {
            case 400:
                return "Bad request";
            case 401:
                return "Authentication required";
            case 403:
                return "Access denied";
            case 404:
                return "Resource not found";
            case 405:
                return "Method not allowed";
            case 406:
                return "Not acceptable";
            case 409:
                return "Conflict with current state";
            case 415:
                return "Unsupported media type";
            case 422:
                return "Validation error";
            case 429:
                return "Too many requests";
            case 500:
                return "Internal server error";
            case 501:
                return "Not implemented";
            case 503:
                return "Service unavailable";
            default:
                return "An error occurred";
        }
    }
}
