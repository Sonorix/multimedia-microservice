package com.multimedia.ms.controller;

import com.multimedia.ms.dao.RatingDao;
import com.multimedia.ms.model.RatingDto;
import jakarta.json.Json;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import jakarta.json.JsonReader;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

/**
 * Servlet controller for ratings
 */
@WebServlet(name = "RatingServlet", urlPatterns = {"/ratings/*"})
public class RatingServlet extends HttpServlet {
    
    private final RatingDao ratingDao;
    
    public RatingServlet() {
        this.ratingDao = new RatingDao();
    }

    /**
     * Handles the HTTP GET method for getting ratings
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        String pathInfo = request.getPathInfo();
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        
        try {
            if (pathInfo == null || pathInfo.equals("/")) {
                // Get ratings for musician or all ratings if musicianId is not provided
                String musicianId = request.getParameter("musicianId");
                List<RatingDto> ratings;
                JsonArrayBuilder ratingsArray = Json.createArrayBuilder();
                double avgRating = 0;
                
                if (musicianId != null && !musicianId.isEmpty()) {
                    // Si se proporciona musicianId, filtrar por este
                    ratings = ratingDao.getRatingsByMusicianId(musicianId);
                } else {
                    // Si no se proporciona musicianId, obtener todas las valoraciones
                    try {
                        ratings = ratingDao.getAllRatings();
                    } catch (Exception e) {
                        handleError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error retrieving all ratings: " + e.toString());
                        e.printStackTrace();
                        return;
                    }
                }
                
                if (!ratings.isEmpty()) {
                    double totalRating = 0;
                    for (RatingDto rating : ratings) {
                        JsonObjectBuilder ratingJson = buildRatingJson(rating);
                        ratingsArray.add(ratingJson);
                        totalRating += rating.getRating();
                    }
                    avgRating = totalRating / ratings.size();
                }
                
                JsonObjectBuilder resultBuilder = Json.createObjectBuilder()
                    .add("ratings", ratingsArray)
                    .add("averageRating", avgRating)
                    .add("count", ratings.size());
                    
                if (musicianId != null && !musicianId.isEmpty()) {
                    resultBuilder.add("musicianId", musicianId);
                }
                
                JsonObject result = resultBuilder.build();
                
                try (PrintWriter out = response.getWriter()) {
                    out.print(result.toString());
                    out.flush();
                }
                
            } else if (pathInfo.matches("^/[^/]+$")) {
                // Get rating by ID: /ratings/{id}
                String id = pathInfo.substring(1);
                RatingDto rating = ratingDao.getRatingById(id);
                
                if (rating == null) {
                    handleError(response, HttpServletResponse.SC_NOT_FOUND, "Rating not found");
                    return;
                }
                
                JsonObjectBuilder ratingJson = buildRatingJson(rating);
                
                try (PrintWriter out = response.getWriter()) {
                    out.print(ratingJson.build().toString());
                    out.flush();
                }
                
            } else {
                handleError(response, HttpServletResponse.SC_NOT_FOUND, "Resource not found");
            }
        } catch (Exception e) {
            handleError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    /**
     * Handles the HTTP POST method for creating new ratings
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        
        try {
            // Read JSON data
            JsonObject data;
            try (JsonReader jsonReader = Json.createReader(request.getInputStream())) {
                data = jsonReader.readObject();
            }
            
            // Validate required fields
            String[] requiredFields = {"musicianId", "userId", "rating"};
            validateFields(data, requiredFields);
            
            String musicianId = data.getString("musicianId");
            String userId = data.getString("userId");
            int ratingValue = data.getInt("rating");
            
            // Validate rating value
            if (ratingValue < 1 || ratingValue > 5) {
                handleError(response, HttpServletResponse.SC_BAD_REQUEST, "Rating must be between 1 and 5");
                return;
            }
            
            // Check if user already rated this musician
            RatingDto existingRating = ratingDao.getRatingByUserAndMusician(musicianId, userId);
            String comment = data.containsKey("comment") ? data.getString("comment") : null;
            
            String ratingId;
            String message;
            
            if (existingRating != null) {
                // Update existing rating
                existingRating.setRating(ratingValue);
                existingRating.setComment(comment);
                // Since there's no explicit updateRating method, we'll use the delete and add pattern
                ratingDao.deleteRating(existingRating.getId());
                ratingDao.addRating(existingRating);
                ratingId = existingRating.getId();
                message = "Rating updated successfully";
            } else {
                // Create new rating
                RatingDto newRating = new RatingDto(musicianId, userId, ratingValue, comment);
                
                RatingDto createdRating = ratingDao.addRating(newRating);
                ratingId = createdRating.getId();
                message = "Rating created successfully";
            }
            
            JsonObject result = Json.createObjectBuilder()
                .add("id", ratingId)
                .add("message", message)
                .build();
            
            try (PrintWriter out = response.getWriter()) {
                out.print(result.toString());
                out.flush();
            }
            
        } catch (IllegalArgumentException e) {
            handleError(response, HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
        } catch (Exception e) {
            handleError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }
    
    /**
     * Handles the HTTP PUT method for updating ratings
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        String pathInfo = request.getPathInfo();
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        
        if (pathInfo == null || !pathInfo.matches("^/[^/]+$")) {
            handleError(response, HttpServletResponse.SC_NOT_FOUND, "Resource not found");
            return;
        }
        
        String id = pathInfo.substring(1);
        
        try {
            // Get existing rating
            RatingDto rating = ratingDao.getRatingById(id);
            if (rating == null) {
                handleError(response, HttpServletResponse.SC_NOT_FOUND, "Rating not found");
                return;
            }
            
            // Read JSON data
            JsonObject data;
            try (JsonReader jsonReader = Json.createReader(request.getInputStream())) {
                data = jsonReader.readObject();
            }
            
            // Update fields if present
            if (data.containsKey("rating")) {
                int ratingValue = data.getInt("rating");
                if (ratingValue < 1 || ratingValue > 5) {
                    handleError(response, HttpServletResponse.SC_BAD_REQUEST, "Rating must be between 1 and 5");
                    return;
                }
                rating.setRating(ratingValue);
            }
            
            if (data.containsKey("comment")) {
                rating.setComment(data.getString("comment"));
            }
            
            // Update rating by deleting and re-adding
            ratingDao.deleteRating(rating.getId());
            ratingDao.addRating(rating);
            
            JsonObject result = Json.createObjectBuilder()
                .add("id", rating.getId())
                .add("message", "Rating updated successfully")
                .build();
            
            try (PrintWriter out = response.getWriter()) {
                out.print(result.toString());
                out.flush();
            }
            
        } catch (Exception e) {
            handleError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }
    
    /**
     * Handles the HTTP DELETE method for deleting ratings
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        String pathInfo = request.getPathInfo();
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        
        if (pathInfo == null || !pathInfo.matches("^/[^/]+$")) {
            handleError(response, HttpServletResponse.SC_NOT_FOUND, "Resource not found");
            return;
        }
        
        String id = pathInfo.substring(1);
        
        try {
            // Check rating exists
            RatingDto rating = ratingDao.getRatingById(id);
            if (rating == null) {
                handleError(response, HttpServletResponse.SC_NOT_FOUND, "Rating not found");
                return;
            }
            
            // Delete rating
            ratingDao.deleteRating(id);
            
            JsonObject result = Json.createObjectBuilder()
                .add("message", "Rating deleted successfully")
                .build();
            
            try (PrintWriter out = response.getWriter()) {
                out.print(result.toString());
                out.flush();
            }
            
        } catch (Exception e) {
            handleError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }
    
    /**
     * Build a JSON object from a rating
     * 
     * @param rating Rating DTO
     * @return JsonObjectBuilder with rating data
     */
    private JsonObjectBuilder buildRatingJson(RatingDto rating) {
        JsonObjectBuilder builder = Json.createObjectBuilder()
            .add("id", rating.getId())
            .add("musicianId", rating.getMusicianId())
            .add("userId", rating.getUserId())
            .add("rating", rating.getRating());
        
        if (rating.getComment() != null) {
            builder.add("comment", rating.getComment());
        } else {
            builder.addNull("comment");
        }
        
        if (rating.getCreatedAt() != null) {
            builder.add("createdAt", rating.getCreatedAt().getTime());
        }
        
        return builder;
    }
    
    /**
     * Validate required fields in JSON data
     * 
     * @param data JSON data
     * @param requiredFields Array of required field names
     * @throws IllegalArgumentException if any required field is missing
     */
    private void validateFields(JsonObject data, String[] requiredFields) {
        for (String field : requiredFields) {
            if (!data.containsKey(field)) {
                throw new IllegalArgumentException("Field '" + field + "' is required");
            }
        }
    }
    
    /**
     * Handles error response
     * 
     * @param response HttpServletResponse
     * @param statusCode HTTP status code
     * @param message Error message
     * @throws IOException If an I/O error occurs
     */
    private void handleError(HttpServletResponse response, int statusCode, String message) throws IOException {
        response.setStatus(statusCode);
        
        JsonObject errorResponse = Json.createObjectBuilder()
                .add("error", message)
                .build();
        
        try (PrintWriter out = response.getWriter()) {
            out.print(errorResponse.toString());
            out.flush();
        }
    }
}
