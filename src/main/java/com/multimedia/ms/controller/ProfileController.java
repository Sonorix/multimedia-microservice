package com.multimedia.ms.controller;

import com.multimedia.ms.dao.MusicianProfileDao;
import com.multimedia.ms.dao.MultimediaDao;
import com.multimedia.ms.model.MusicianProfileDto;
import com.multimedia.ms.model.MultimediaDto;
import jakarta.json.Json;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import jakarta.json.JsonReader;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.io.StringReader;
import java.util.List;

/**
 * REST controller for musician profiles
 */
@Path("/profiles")
public class ProfileController {
    
    private final MusicianProfileDao profileDao;
    private final MultimediaDao multimediaDao;
    
    public ProfileController() {
        this.profileDao = new MusicianProfileDao();
        this.multimediaDao = new MultimediaDao();
    }
    
    /**
     * Get all musician profiles
     * 
     * @return Response with all profiles
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAllProfiles() {
        try {
            List<MusicianProfileDto> profiles = profileDao.getAllProfiles();
            JsonArrayBuilder array = Json.createArrayBuilder();
            
            for (MusicianProfileDto profile : profiles) {
                JsonObjectBuilder profileJson = Json.createObjectBuilder()
                    .add("id", profile.getId())
                    .add("userId", profile.getUserId())
                    .add("name", profile.getName())
                    .add("biography", profile.getBiography() != null ? profile.getBiography() : "")
                    .add("averageRating", profile.getAverageRating())
                    .add("totalRatings", profile.getTotalRatings())
                    .add("createdAt", profile.getCreatedAt().getTime());
                
                JsonArrayBuilder genresArray = Json.createArrayBuilder();
                for (String genre : profile.getGenres()) {
                    genresArray.add(genre);
                }
                
                JsonArrayBuilder instrumentsArray = Json.createArrayBuilder();
                for (String instrument : profile.getInstruments()) {
                    instrumentsArray.add(instrument);
                }
                
                profileJson.add("genres", genresArray);
                profileJson.add("instruments", instrumentsArray);
                
                array.add(profileJson);
            }
            
            return Response.ok(array.build().toString()).build();
        } catch (Exception e) {
            return Response
                .status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(Json.createObjectBuilder().add("error", e.getMessage()).build().toString())
                .build();
        }
    }
    
    /**
     * Get a musician profile by ID
     * 
     * @param id Profile ID
     * @return Response with profile or 404
     */
    @GET
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getProfileById(@PathParam("id") String id) {
        try {
            MusicianProfileDto profile = profileDao.getProfileById(id);
            
            if (profile == null) {
                return Response
                    .status(Response.Status.NOT_FOUND)
                    .entity(Json.createObjectBuilder().add("message", "Profile not found").build().toString())
                    .build();
            }
            
            // Get public multimedia files for this musician
            List<MultimediaDto> files = multimediaDao.getPublicFilesByMusicianId(id);
            JsonArrayBuilder filesArray = Json.createArrayBuilder();
            
            for (MultimediaDto file : files) {
                JsonObjectBuilder fileJson = Json.createObjectBuilder()
                    .add("id", file.getId())
                    .add("title", file.getTitle())
                    .add("description", file.getDescription() != null ? file.getDescription() : "")
                    .add("mediaType", file.getMediaType())
                    .add("uploadDate", file.getUploadDate().getTime());
                
                filesArray.add(fileJson);
            }
            
            // Build response with profile and public files
            JsonObjectBuilder responseJson = Json.createObjectBuilder()
                .add("id", profile.getId())
                .add("userId", profile.getUserId())
                .add("name", profile.getName())
                .add("biography", profile.getBiography() != null ? profile.getBiography() : "")
                .add("averageRating", profile.getAverageRating())
                .add("totalRatings", profile.getTotalRatings())
                .add("createdAt", profile.getCreatedAt().getTime());
            
            JsonArrayBuilder genresArray = Json.createArrayBuilder();
            for (String genre : profile.getGenres()) {
                genresArray.add(genre);
            }
            
            JsonArrayBuilder instrumentsArray = Json.createArrayBuilder();
            for (String instrument : profile.getInstruments()) {
                instrumentsArray.add(instrument);
            }
            
            responseJson.add("genres", genresArray);
            responseJson.add("instruments", instrumentsArray);
            responseJson.add("multimedia", filesArray);
            
            return Response.ok(responseJson.build().toString()).build();
        } catch (Exception e) {
            return Response
                .status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(Json.createObjectBuilder().add("error", e.getMessage()).build().toString())
                .build();
        }
    }
    
    /**
     * Get a musician profile by user ID
     * 
     * @param userId User ID
     * @return Response with profile or 404
     */
    @GET
    @Path("/user/{userId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getProfileByUserId(@PathParam("userId") String userId) {
        try {
            MusicianProfileDto profile = profileDao.getProfileByUserId(userId);
            
            if (profile == null) {
                return Response
                    .status(Response.Status.NOT_FOUND)
                    .entity(Json.createObjectBuilder().add("message", "Profile not found").build().toString())
                    .build();
            }
            
            // Reuse the getProfileById logic since we have the profile ID now
            return getProfileById(profile.getId());
        } catch (Exception e) {
            return Response
                .status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(Json.createObjectBuilder().add("error", e.getMessage()).build().toString())
                .build();
        }
    }
    
    /**
     * Create a new musician profile
     * 
     * @param profileJson Profile JSON data
     * @return Response with created profile
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createProfile(String profileJson) {
        try {
            // Parse the input JSON
            JsonReader jsonReader = Json.createReader(new StringReader(profileJson));
            JsonObject json = jsonReader.readObject();
            jsonReader.close();
            
            // Validate required fields
            if (!json.containsKey("userId") || !json.containsKey("name")) {
                return Response
                    .status(Response.Status.BAD_REQUEST)
                    .entity(Json.createObjectBuilder().add("message", "Missing required fields: userId, name").build().toString())
                    .build();
            }
            
            // Create profile object
            MusicianProfileDto profile = new MusicianProfileDto(
                json.getString("userId"),
                json.getString("name"),
                json.containsKey("biography") ? json.getString("biography") : ""
            );
            
            // Process optional arrays
            if (json.containsKey("genres") && !json.isNull("genres")) {
                json.getJsonArray("genres").forEach(genre -> 
                    profile.getGenres().add(genre.toString().replace("\"", ""))
                );
            }
            
            if (json.containsKey("instruments") && !json.isNull("instruments")) {
                json.getJsonArray("instruments").forEach(instrument -> 
                    profile.getInstruments().add(instrument.toString().replace("\"", ""))
                );
            }
            
            // Save to database
            MusicianProfileDto savedProfile = profileDao.createProfile(profile);
            
            // Build response
            JsonObjectBuilder response = Json.createObjectBuilder()
                .add("id", savedProfile.getId())
                .add("userId", savedProfile.getUserId())
                .add("name", savedProfile.getName())
                .add("biography", savedProfile.getBiography() != null ? savedProfile.getBiography() : "")
                .add("averageRating", savedProfile.getAverageRating())
                .add("totalRatings", savedProfile.getTotalRatings())
                .add("createdAt", savedProfile.getCreatedAt().getTime());
            
            JsonArrayBuilder genresArray = Json.createArrayBuilder();
            for (String genre : savedProfile.getGenres()) {
                genresArray.add(genre);
            }
            
            JsonArrayBuilder instrumentsArray = Json.createArrayBuilder();
            for (String instrument : savedProfile.getInstruments()) {
                instrumentsArray.add(instrument);
            }
            
            response.add("genres", genresArray);
            response.add("instruments", instrumentsArray);
            
            return Response
                .status(Response.Status.CREATED)
                .entity(response.build().toString())
                .build();
        } catch (Exception e) {
            return Response
                .status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(Json.createObjectBuilder().add("error", e.getMessage()).build().toString())
                .build();
        }
    }
    
    /**
     * Update a musician profile
     * 
     * @param id Profile ID
     * @param profileJson Profile JSON data
     * @return Response with updated profile
     */
    @PUT
    @Path("/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateProfile(@PathParam("id") String id, String profileJson) {
        try {
            // Check if profile exists
            MusicianProfileDto existingProfile = profileDao.getProfileById(id);
            if (existingProfile == null) {
                return Response
                    .status(Response.Status.NOT_FOUND)
                    .entity(Json.createObjectBuilder().add("message", "Profile not found").build().toString())
                    .build();
            }
            
            // Parse the input JSON
            JsonReader jsonReader = Json.createReader(new StringReader(profileJson));
            JsonObject json = jsonReader.readObject();
            jsonReader.close();
            
            // Update profile fields
            if (json.containsKey("name")) {
                existingProfile.setName(json.getString("name"));
            }
            
            if (json.containsKey("biography")) {
                existingProfile.setBiography(json.getString("biography"));
            }
            
            // Handle arrays - replace completely if present
            if (json.containsKey("genres") && !json.isNull("genres")) {
                existingProfile.getGenres().clear();
                json.getJsonArray("genres").forEach(genre -> 
                    existingProfile.getGenres().add(genre.toString().replace("\"", ""))
                );
            }
            
            if (json.containsKey("instruments") && !json.isNull("instruments")) {
                existingProfile.getInstruments().clear();
                json.getJsonArray("instruments").forEach(instrument -> 
                    existingProfile.getInstruments().add(instrument.toString().replace("\"", ""))
                );
            }
            
            // Save updates
            MusicianProfileDto updatedProfile = profileDao.updateProfile(existingProfile);
            
            // Build response
            JsonObjectBuilder response = Json.createObjectBuilder()
                .add("id", updatedProfile.getId())
                .add("userId", updatedProfile.getUserId())
                .add("name", updatedProfile.getName())
                .add("biography", updatedProfile.getBiography() != null ? updatedProfile.getBiography() : "")
                .add("averageRating", updatedProfile.getAverageRating())
                .add("totalRatings", updatedProfile.getTotalRatings())
                .add("createdAt", updatedProfile.getCreatedAt().getTime())
                .add("updatedAt", updatedProfile.getUpdatedAt().getTime());
            
            JsonArrayBuilder genresArray = Json.createArrayBuilder();
            for (String genre : updatedProfile.getGenres()) {
                genresArray.add(genre);
            }
            
            JsonArrayBuilder instrumentsArray = Json.createArrayBuilder();
            for (String instrument : updatedProfile.getInstruments()) {
                instrumentsArray.add(instrument);
            }
            
            response.add("genres", genresArray);
            response.add("instruments", instrumentsArray);
            
            return Response.ok(response.build().toString()).build();
        } catch (Exception e) {
            return Response
                .status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(Json.createObjectBuilder().add("error", e.getMessage()).build().toString())
                .build();
        }
    }
    
    /**
     * Delete a musician profile
     * 
     * @param id Profile ID
     * @return Response with success or failure message
     */
    @DELETE
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteProfile(@PathParam("id") String id) {
        try {
            boolean deleted = profileDao.deleteProfile(id);
            
            if (deleted) {
                return Response
                    .ok(Json.createObjectBuilder().add("message", "Profile deleted successfully").build().toString())
                    .build();
            } else {
                return Response
                    .status(Response.Status.NOT_FOUND)
                    .entity(Json.createObjectBuilder().add("message", "Profile not found").build().toString())
                    .build();
            }
        } catch (Exception e) {
            return Response
                .status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(Json.createObjectBuilder().add("error", e.getMessage()).build().toString())
                .build();
        }
    }
}
