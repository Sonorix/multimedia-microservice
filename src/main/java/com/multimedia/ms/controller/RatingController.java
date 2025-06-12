package com.multimedia.ms.controller;

import com.multimedia.ms.dao.MusicianProfileDao;
import com.multimedia.ms.dao.RatingDao;
import com.multimedia.ms.model.MusicianProfileDto;
import com.multimedia.ms.model.RatingDto;
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
 * REST controller for musician ratings
 */
@Path("/ratings")
public class RatingController {
    
    private final RatingDao ratingDao;
    private final MusicianProfileDao profileDao;
    
    public RatingController() {
        this.ratingDao = new RatingDao();
        this.profileDao = new MusicianProfileDao();
    }
    
    /**
     * Get all ratings for a musician
     * 
     * @param musicianId Musician ID
     * @return Response with all ratings
     */
    @GET
    @Path("/musician/{musicianId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getRatingsByMusicianId(@PathParam("musicianId") String musicianId) {
        try {
            // First check if musician exists
            MusicianProfileDto musician = profileDao.getProfileById(musicianId);
            if (musician == null) {
                return Response
                    .status(Response.Status.NOT_FOUND)
                    .entity(Json.createObjectBuilder().add("message", "Musician not found").build().toString())
                    .build();
            }
            
            // Get ratings for this musician
            List<RatingDto> ratings = ratingDao.getRatingsByMusicianId(musicianId);
            JsonArrayBuilder array = Json.createArrayBuilder();
            
            for (RatingDto rating : ratings) {
                JsonObjectBuilder ratingJson = Json.createObjectBuilder()
                    .add("id", rating.getId())
                    .add("musicianId", rating.getMusicianId())
                    .add("userId", rating.getUserId())
                    .add("rating", rating.getRating())
                    .add("comment", rating.getComment() != null ? rating.getComment() : "")
                    .add("createdAt", rating.getCreatedAt().getTime());
                
                array.add(ratingJson);
            }
            
            // Add musician summary info
            JsonObjectBuilder response = Json.createObjectBuilder()
                .add("musicianId", musicianId)
                .add("musicianName", musician.getName())
                .add("averageRating", musician.getAverageRating())
                .add("totalRatings", musician.getTotalRatings())
                .add("ratings", array);
            
            return Response.ok(response.build().toString()).build();
        } catch (Exception e) {
            return Response
                .status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(Json.createObjectBuilder().add("error", e.getMessage()).build().toString())
                .build();
        }
    }
    
    /**
     * Get a specific rating by ID
     * 
     * @param id Rating ID
     * @return Response with rating or 404
     */
    @GET
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getRatingById(@PathParam("id") String id) {
        try {
            RatingDto rating = ratingDao.getRatingById(id);
            
            if (rating == null) {
                return Response
                    .status(Response.Status.NOT_FOUND)
                    .entity(Json.createObjectBuilder().add("message", "Rating not found").build().toString())
                    .build();
            }
            
            JsonObjectBuilder response = Json.createObjectBuilder()
                .add("id", rating.getId())
                .add("musicianId", rating.getMusicianId())
                .add("userId", rating.getUserId())
                .add("rating", rating.getRating())
                .add("comment", rating.getComment() != null ? rating.getComment() : "")
                .add("createdAt", rating.getCreatedAt().getTime());
            
            return Response.ok(response.build().toString()).build();
        } catch (Exception e) {
            return Response
                .status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(Json.createObjectBuilder().add("error", e.getMessage()).build().toString())
                .build();
        }
    }
    
    /**
     * Get rating from a specific user for a musician
     * 
     * @param musicianId Musician ID
     * @param userId User ID
     * @return Response with rating or 404
     */
    @GET
    @Path("/musician/{musicianId}/user/{userId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getRatingByUserAndMusician(
            @PathParam("musicianId") String musicianId,
            @PathParam("userId") String userId) {
        try {
            RatingDto rating = ratingDao.getRatingByUserAndMusician(musicianId, userId);
            
            if (rating == null) {
                return Response
                    .status(Response.Status.NOT_FOUND)
                    .entity(Json.createObjectBuilder().add("message", "Rating not found").build().toString())
                    .build();
            }
            
            JsonObjectBuilder response = Json.createObjectBuilder()
                .add("id", rating.getId())
                .add("musicianId", rating.getMusicianId())
                .add("userId", rating.getUserId())
                .add("rating", rating.getRating())
                .add("comment", rating.getComment() != null ? rating.getComment() : "")
                .add("createdAt", rating.getCreatedAt().getTime());
            
            return Response.ok(response.build().toString()).build();
        } catch (Exception e) {
            return Response
                .status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(Json.createObjectBuilder().add("error", e.getMessage()).build().toString())
                .build();
        }
    }
    
    /**
     * Add a new rating for a musician
     * 
     * @param ratingJson Rating JSON data
     * @return Response with created rating
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response addRating(String ratingJson) {
        try {
            // Parse the input JSON
            JsonReader jsonReader = Json.createReader(new StringReader(ratingJson));
            JsonObject json = jsonReader.readObject();
            jsonReader.close();
            
            // Validate required fields
            if (!json.containsKey("musicianId") || !json.containsKey("userId") || !json.containsKey("rating")) {
                return Response
                    .status(Response.Status.BAD_REQUEST)
                    .entity(Json.createObjectBuilder().add("message", "Missing required fields: musicianId, userId, rating").build().toString())
                    .build();
            }
            
            // Check if musician exists
            String musicianId = json.getString("musicianId");
            MusicianProfileDto musician = profileDao.getProfileById(musicianId);
            if (musician == null) {
                return Response
                    .status(Response.Status.NOT_FOUND)
                    .entity(Json.createObjectBuilder().add("message", "Musician not found").build().toString())
                    .build();
            }
            
            // Check if rating is valid (1-5)
            double ratingValue = Double.parseDouble(json.get("rating").toString());
            if (ratingValue < 1 || ratingValue > 5) {
                return Response
                    .status(Response.Status.BAD_REQUEST)
                    .entity(Json.createObjectBuilder().add("message", "Rating must be between 1 and 5").build().toString())
                    .build();
            }
            
            // Check if user already rated this musician
            String userId = json.getString("userId");
            RatingDto existingRating = ratingDao.getRatingByUserAndMusician(musicianId, userId);
            if (existingRating != null) {
                return Response
                    .status(Response.Status.CONFLICT)
                    .entity(Json.createObjectBuilder().add("message", "User already rated this musician").build().toString())
                    .build();
            }
            
            // Create rating object
            RatingDto rating = new RatingDto(
                musicianId,
                userId,
                ratingValue,
                json.containsKey("comment") ? json.getString("comment") : ""
            );
            
            // Save to database
            RatingDto savedRating = ratingDao.addRating(rating);
            
            // Build response
            JsonObjectBuilder response = Json.createObjectBuilder()
                .add("id", savedRating.getId())
                .add("musicianId", savedRating.getMusicianId())
                .add("userId", savedRating.getUserId())
                .add("rating", savedRating.getRating())
                .add("comment", savedRating.getComment() != null ? savedRating.getComment() : "")
                .add("createdAt", savedRating.getCreatedAt().getTime());
            
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
     * Delete a rating
     * 
     * @param id Rating ID
     * @return Response with success or failure message
     */
    @DELETE
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteRating(@PathParam("id") String id) {
        try {
            boolean deleted = ratingDao.deleteRating(id);
            
            if (deleted) {
                return Response
                    .ok(Json.createObjectBuilder().add("message", "Rating deleted successfully").build().toString())
                    .build();
            } else {
                return Response
                    .status(Response.Status.NOT_FOUND)
                    .entity(Json.createObjectBuilder().add("message", "Rating not found").build().toString())
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
