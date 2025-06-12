package com.multimedia.ms.dao;

import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.FindOneAndUpdateOptions;
import com.mongodb.client.model.ReturnDocument;
import com.mongodb.client.model.Updates;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.InsertOneResult;
import com.mongodb.client.result.UpdateResult;
import com.multimedia.ms.model.Database;
import com.multimedia.ms.model.MusicianProfileDto;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;

/**
 * Data access object for musician profiles
 */
public class MusicianProfileDao {
    private final Database database;
    private final MongoCollection<Document> collection;
    
    public MusicianProfileDao() {
        this.database = new Database();
        this.collection = database.getDatabase().getCollection("musician_profiles");
    }
    
    /**
     * Create a new musician profile
     * 
     * @param profile The profile to create
     * @return The created profile with ID
     * @throws RuntimeException if an error occurs
     */
    public MusicianProfileDto createProfile(MusicianProfileDto profile) {
        try {
            Document doc = profile.toDocument();
            
            // Initialize ratings if they're not set
            if (!doc.containsKey("averageRating")) {
                doc.append("averageRating", 0.0);
            }
            if (!doc.containsKey("totalRatings")) {
                doc.append("totalRatings", 0);
            }
            
            InsertOneResult result = collection.insertOne(doc);
            if (result.getInsertedId() != null) {
                profile.setId(result.getInsertedId().asObjectId().getValue().toString());
            }
            return profile;
        } catch (Exception e) {
            throw new RuntimeException("Error creating musician profile: " + e.getMessage(), e);
        }
    }
    
    /**
     * Get a musician profile by ID
     * 
     * @param id The profile ID
     * @return The musician profile or null if not found
     */
    public MusicianProfileDto getProfileById(String id) {
        try {
            Document doc = collection.find(Filters.eq("_id", new ObjectId(id))).first();
            return MusicianProfileDto.fromDocument(doc);
        } catch (Exception e) {
            throw new RuntimeException("Error retrieving musician profile: " + e.getMessage(), e);
        }
    }
    
    /**
     * Get a musician profile by user ID
     * 
     * @param userId The user ID
     * @return The musician profile or null if not found
     */
    public MusicianProfileDto getProfileByUserId(String userId) {
        try {
            Document doc = collection.find(Filters.eq("userId", userId)).first();
            return MusicianProfileDto.fromDocument(doc);
        } catch (Exception e) {
            throw new RuntimeException("Error retrieving musician profile: " + e.getMessage(), e);
        }
    }
    
    /**
     * Get all musician profiles
     * 
     * @return List of musician profiles
     */
    public List<MusicianProfileDto> getAllProfiles() {
        List<MusicianProfileDto> profiles = new ArrayList<>();
        try {
            FindIterable<Document> docs = collection.find();
            MongoCursor<Document> cursor = docs.iterator();
            
            while (cursor.hasNext()) {
                Document doc = cursor.next();
                MusicianProfileDto profile = MusicianProfileDto.fromDocument(doc);
                if (profile != null) {
                    profiles.add(profile);
                }
            }
            cursor.close();
        } catch (Exception e) {
            throw new RuntimeException("Error retrieving musician profiles: " + e.getMessage(), e);
        }
        return profiles;
    }
    
    /**
     * Update a musician profile
     * 
     * @param profile The profile to update
     * @return The updated profile
     */
    public MusicianProfileDto updateProfile(MusicianProfileDto profile) {
        try {
            Document doc = profile.toDocument();
            doc.remove("_id"); // Remove ID from update document
            
            // Always update the updatedAt field
            doc.put("updatedAt", new Date());
            
            // Create a find filter by ID
            Bson filter = Filters.eq("_id", new ObjectId(profile.getId()));
            
            FindOneAndUpdateOptions options = new FindOneAndUpdateOptions()
                    .returnDocument(ReturnDocument.AFTER);
            
            Document updatedDoc = collection.findOneAndUpdate(
                    filter,
                    new Document("$set", doc),
                    options
            );
            
            return MusicianProfileDto.fromDocument(updatedDoc);
        } catch (Exception e) {
            throw new RuntimeException("Error updating musician profile: " + e.getMessage(), e);
        }
    }
    
    /**
     * Delete a musician profile by ID
     * 
     * @param id The profile ID
     * @return true if deleted, false if not found
     */
    public boolean deleteProfile(String id) {
        try {
            DeleteResult result = collection.deleteOne(Filters.eq("_id", new ObjectId(id)));
            return result.getDeletedCount() > 0;
        } catch (Exception e) {
            throw new RuntimeException("Error deleting musician profile: " + e.getMessage(), e);
        }
    }
    
    /**
     * Update the rating statistics for a musician profile
     * 
     * @param musicianId The musician ID
     * @param newAverageRating The new average rating
     * @param totalRatings The new total ratings count
     * @return true if updated, false if not found
     */
    public boolean updateRatingStats(String musicianId, double newAverageRating, int totalRatings) {
        try {
            UpdateResult result = collection.updateOne(
                Filters.eq("_id", new ObjectId(musicianId)),
                Updates.combine(
                    Updates.set("averageRating", newAverageRating),
                    Updates.set("totalRatings", totalRatings)
                )
            );
            
            return result.getModifiedCount() > 0;
        } catch (Exception e) {
            throw new RuntimeException("Error updating rating statistics: " + e.getMessage(), e);
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
