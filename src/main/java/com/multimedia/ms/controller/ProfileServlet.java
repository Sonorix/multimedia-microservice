package com.multimedia.ms.controller;

import com.multimedia.ms.dao.MusicianProfileDao;
import com.multimedia.ms.model.MusicianProfileDto;
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
import java.util.ArrayList;
import java.util.List;

/**
 * Servlet controller for musician profiles
 */
@WebServlet(name = "ProfileServlet", urlPatterns = {"/profiles/*"})
public class ProfileServlet extends HttpServlet {
    
    private final MusicianProfileDao profileDao;
    
    public ProfileServlet() {
        this.profileDao = new MusicianProfileDao();
    }

    /**
     * Handles the HTTP GET method for getting musician profiles
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
                // Get all profiles
                List<MusicianProfileDto> profiles = profileDao.getAllProfiles();
                JsonArrayBuilder profilesArray = Json.createArrayBuilder();
                
                for (MusicianProfileDto profile : profiles) {
                    JsonObjectBuilder profileJson = buildProfileJson(profile);
                    profilesArray.add(profileJson);
                }
                
                JsonObject result = Json.createObjectBuilder()
                    .add("profiles", profilesArray)
                    .build();
                
                try (PrintWriter out = response.getWriter()) {
                    out.print(result.toString());
                    out.flush();
                }
                
            } else if (pathInfo.matches("^/[^/]+$")) {
                // Get profile by ID
                String id = pathInfo.substring(1);
                MusicianProfileDto profile = profileDao.getProfileById(id);
                
                if (profile == null) {
                    handleError(response, HttpServletResponse.SC_NOT_FOUND, "Musician profile not found");
                    return;
                }
                
                JsonObjectBuilder profileJson = buildProfileJson(profile);
                
                try (PrintWriter out = response.getWriter()) {
                    out.print(profileJson.build().toString());
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
     * Handles the HTTP POST method for creating new musician profiles
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
            String[] requiredFields = {"userId", "artisticName", "genre"};
            validateFields(data, requiredFields);
            
            // Create profile DTO
            MusicianProfileDto newProfile = new MusicianProfileDto();
            newProfile.setUserId(data.getString("userId"));
            newProfile.setName(data.getString("artisticName"));
            
            if (data.containsKey("imageUrl") && !data.isNull("imageUrl")) {
                newProfile.setImageUrl(data.getString("imageUrl"));
            }
            
            if (data.containsKey("bio") && !data.isNull("bio")) {
                newProfile.setBiography(data.getString("bio"));
            }
            
            // Set genres
            if (data.containsKey("genre")) {
                List<String> genres = new ArrayList<>();
                genres.add(data.getString("genre"));
                newProfile.setGenres(genres);
            }
            
            // Save profile
            MusicianProfileDto savedProfile = profileDao.createProfile(newProfile);
            
            if (savedProfile != null) {
                JsonObject jsonResult = Json.createObjectBuilder()
                    .add("id", savedProfile.getId())
                    .add("message", "Musician profile created successfully")
                    .build();
                
                try (PrintWriter out = response.getWriter()) {
                    out.print(jsonResult.toString());
                    out.flush();
                }
            } else {
                handleError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to create profile");
            }
            
        } catch (IllegalArgumentException e) {
            handleError(response, HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
        } catch (Exception e) {
            handleError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }
    
    /**
     * Handles the HTTP PUT method for updating musician profiles
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
            // Get existing profile
            MusicianProfileDto profile = profileDao.getProfileById(id);
            if (profile == null) {
                handleError(response, HttpServletResponse.SC_NOT_FOUND, "Musician profile not found");
                return;
            }
            
            // Read JSON data
            JsonObject data;
            try (JsonReader jsonReader = Json.createReader(request.getInputStream())) {
                data = jsonReader.readObject();
            }
            
            // Update fields if present
            if (data.containsKey("artisticName")) {
                String artisticName = data.getString("artisticName");
                if (artisticName == null || artisticName.isEmpty()) {
                    handleError(response, HttpServletResponse.SC_BAD_REQUEST, "Artistic name cannot be empty");
                    return;
                }
                profile.setName(artisticName);
            }
            
            if (data.containsKey("genre")) {
                String genre = data.getString("genre");
                if (genre == null || genre.isEmpty()) {
                    handleError(response, HttpServletResponse.SC_BAD_REQUEST, "Genre cannot be empty");
                    return;
                }
                
                List<String> genres = new ArrayList<>();
                genres.add(genre);
                profile.setGenres(genres);
            }
            
            if (data.containsKey("bio")) {
                profile.setBiography(data.getString("bio"));
            }
            
            // Update profile
            profileDao.updateProfile(profile);
            
            JsonObject result = Json.createObjectBuilder()
                .add("id", profile.getId())
                .add("message", "Musician profile updated successfully")
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
     * Handles the HTTP DELETE method for deleting musician profiles
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
            // Check profile exists
            MusicianProfileDto profile = profileDao.getProfileById(id);
            if (profile == null) {
                handleError(response, HttpServletResponse.SC_NOT_FOUND, "Musician profile not found");
                return;
            }
            
            // Delete profile
            profileDao.deleteProfile(id);
            
            JsonObject result = Json.createObjectBuilder()
                .add("message", "Musician profile deleted successfully")
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
     * Build a JSON object from a musician profile
     * 
     * @param profile Musician profile DTO
     * @return JsonObjectBuilder with profile data
     */
    private JsonObjectBuilder buildProfileJson(MusicianProfileDto profile) {
        JsonObjectBuilder builder = Json.createObjectBuilder()
            .add("id", profile.getId())
            .add("userId", profile.getUserId())
            .add("artisticName", profile.getName());
            
        // Añadir imageUrl si está disponible
        if (profile.getImageUrl() != null && !profile.getImageUrl().isEmpty()) {
            builder.add("imageUrl", profile.getImageUrl());
        } else {
            builder.addNull("imageUrl");
        }
        
        // Add genres
        if (profile.getGenres() != null && !profile.getGenres().isEmpty()) {
            builder.add("genre", profile.getGenres().get(0)); // Use first genre for compatibility
            
            JsonArrayBuilder genresArray = Json.createArrayBuilder();
            for (String genre : profile.getGenres()) {
                genresArray.add(genre);
            }
            builder.add("genres", genresArray);
        } else {
            builder.addNull("genre");
            builder.add("genres", Json.createArrayBuilder());
        }
        
        // Add instruments if available
        if (profile.getInstruments() != null && !profile.getInstruments().isEmpty()) {
            JsonArrayBuilder instrumentsArray = Json.createArrayBuilder();
            for (String instrument : profile.getInstruments()) {
                instrumentsArray.add(instrument);
            }
            builder.add("instruments", instrumentsArray);
        } else {
            builder.add("instruments", Json.createArrayBuilder());
        }
        
        if (profile.getBiography() != null) {
            builder.add("bio", profile.getBiography());
        } else {
            builder.addNull("bio");
        }
        
        // Add ratings information
        builder.add("averageRating", profile.getAverageRating());
        builder.add("totalRatings", profile.getTotalRatings());
        
        if (profile.getCreatedAt() != null) {
            builder.add("createdAt", profile.getCreatedAt().getTime());
        }
        
        return builder;
    }
    
    /**
     * Validate required fields in JSON data
     * 
     * @param data JSON data
     * @param requiredFields Array of required field names
     * @throws IllegalArgumentException if any required field is missing or empty
     */
    private void validateFields(JsonObject data, String[] requiredFields) {
        for (String field : requiredFields) {
            if (!data.containsKey(field) || data.isNull(field) || data.getString(field).isEmpty()) {
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
