package com.multimedia.ms.dao;

import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.model.Filters;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.InsertOneResult;
import com.multimedia.ms.model.Database;
import com.multimedia.ms.model.RatingDto;
import java.util.ArrayList;
import java.util.List;
import org.bson.Document;
import org.bson.types.ObjectId;

/**
 * Data access object for musician ratings
 */
public class RatingDao {
    private final Database database;
    private final MongoCollection<Document> collection;
    private final MusicianProfileDao musicianProfileDao;
    
    public RatingDao() {
        this.database = new Database();
        this.collection = database.getDatabase().getCollection("ratings");
        this.musicianProfileDao = new MusicianProfileDao();
    }
    
    /**
     * Add a new rating for a musician
     * 
     * @param rating The rating to add
     * @return The created rating with ID
     * @throws RuntimeException if an error occurs
     */
    public RatingDto addRating(RatingDto rating) {
        try {
            Document doc = rating.toDocument();
            InsertOneResult result = collection.insertOne(doc);
            
            if (result.getInsertedId() != null) {
                rating.setId(result.getInsertedId().asObjectId().getValue().toString());
            }
            
            // Update the musician's average rating
            updateMusicianAverageRating(rating.getMusicianId());
            
            return rating;
        } catch (Exception e) {
            throw new RuntimeException("Error adding rating: " + e.getMessage(), e);
        }
    }
    
    /**
     * Get all ratings
     * 
     * @return List of all ratings
     */
    public List<RatingDto> getAllRatings() {
        List<RatingDto> ratings = new ArrayList<>();
        try {
            // Verificar si la colección existe
            if (collection == null) {
                System.err.println("Warning: Collection 'ratings' is null");
                return ratings;
            }
            
            try {
                FindIterable<Document> docs = collection.find();
                if (docs == null) {
                    System.err.println("Warning: Find operation returned null");
                    return ratings;
                }
                
                MongoCursor<Document> cursor = docs.iterator();
                while (cursor.hasNext()) {
                    Document doc = cursor.next();
                    if (doc != null) {
                        RatingDto rating = RatingDto.fromDocument(doc);
                        if (rating != null) {
                            ratings.add(rating);
                        }
                    }
                }
                cursor.close();
            } catch (Exception e) {
                System.err.println("Error during cursor iteration: " + e.toString());
                e.printStackTrace();
            }
        } catch (Exception e) {
            System.err.println("Critical error in getAllRatings: " + e.toString());
            e.printStackTrace();
            // No relanzar la excepción para evitar errores 500
        }
        return ratings;
    }

    /**
     * Get a rating by ID
     * 
     * @param id The rating ID
     * @return The rating or null if not found
     */
    public RatingDto getRatingById(String id) {
        try {
            Document doc = collection.find(Filters.eq("_id", new ObjectId(id))).first();
            return RatingDto.fromDocument(doc);
        } catch (Exception e) {
            throw new RuntimeException("Error retrieving rating: " + e.getMessage(), e);
        }
    }
    
    /**
     * Get all ratings for a musician
     * 
     * @param musicianId The musician ID
     * @return List of ratings for the musician
     */
    public List<RatingDto> getRatingsByMusicianId(String musicianId) {
        List<RatingDto> ratings = new ArrayList<>();
        try {
            FindIterable<Document> docs = collection.find(Filters.eq("musicianId", musicianId));
            MongoCursor<Document> cursor = docs.iterator();
            
            while (cursor.hasNext()) {
                Document doc = cursor.next();
                RatingDto rating = RatingDto.fromDocument(doc);
                if (rating != null) {
                    ratings.add(rating);
                }
            }
            cursor.close();
        } catch (Exception e) {
            throw new RuntimeException("Error retrieving ratings: " + e.getMessage(), e);
        }
        return ratings;
    }
    
    /**
     * Get rating submitted by a specific user for a specific musician
     * 
     * @param musicianId The musician ID
     * @param userId The user ID who submitted the rating
     * @return The rating or null if not found
     */
    public RatingDto getRatingByUserAndMusician(String musicianId, String userId) {
        try {
            Document doc = collection.find(
                Filters.and(
                    Filters.eq("musicianId", musicianId),
                    Filters.eq("userId", userId)
                )
            ).first();
            return RatingDto.fromDocument(doc);
        } catch (Exception e) {
            throw new RuntimeException("Error retrieving rating: " + e.getMessage(), e);
        }
    }
    
    /**
     * Delete a rating by ID
     * 
     * @param id The rating ID
     * @return true if deleted, false if not found
     */
    public boolean deleteRating(String id) {
        try {
            // First get the rating to know which musician to update after deletion
            RatingDto rating = getRatingById(id);
            if (rating == null) {
                return false;
            }
            
            DeleteResult result = collection.deleteOne(Filters.eq("_id", new ObjectId(id)));
            
            if (result.getDeletedCount() > 0) {
                // Update the musician's average rating after deletion
                updateMusicianAverageRating(rating.getMusicianId());
                return true;
            }
            
            return false;
        } catch (Exception e) {
            throw new RuntimeException("Error deleting rating: " + e.getMessage(), e);
        }
    }
    
    /**
     * Calculate and update the average rating for a musician
     * 
     * @param musicianId The musician ID
     */
    private void updateMusicianAverageRating(String musicianId) {
        try {
            List<RatingDto> ratings = getRatingsByMusicianId(musicianId);
            
            // Calculate new average
            double sum = 0;
            for (RatingDto rating : ratings) {
                sum += rating.getRating();
            }
            
            double averageRating = ratings.isEmpty() ? 0 : sum / ratings.size();
            int totalRatings = ratings.size();
            
            // Update the musician profile with new rating stats
            musicianProfileDao.updateRatingStats(musicianId, averageRating, totalRatings);
        } catch (Exception e) {
            throw new RuntimeException("Error updating musician average rating: " + e.getMessage(), e);
        }
    }
    
    /**
     * Close the database connection
     */
    public void close() {
        if (database != null) {
            database.close();
        }
    }
}
