package com.multimedia.ms.model;

import java.util.Date;
import org.bson.Document;
import org.bson.types.ObjectId;

/**
 * Data transfer object for musician ratings
 */
public class RatingDto {
    private String id;
    private String musicianId;
    private String userId;
    private int rating;           // Rating value (1-5)
    private String comment;
    private Date createdAt;
    
    public RatingDto() {
        this.createdAt = new Date();
    }
    
    public RatingDto(String musicianId, String userId, int rating, String comment) {
        this();
        this.musicianId = musicianId;
        this.userId = userId;
        this.rating = validateRating(rating);
        this.comment = comment;
    }
    
    public RatingDto(String id, String musicianId, String userId, int rating, String comment, Date createdAt) {
        this.id = id;
        this.musicianId = musicianId;
        this.userId = userId;
        this.rating = validateRating(rating);
        this.comment = comment;
        this.createdAt = createdAt;
    }
    
    // Factory method to create from MongoDB Document
    public static RatingDto fromDocument(Document doc) {
        if (doc == null) return null;
        
        ObjectId objectId = doc.getObjectId("_id");
        String id = objectId != null ? objectId.toString() : null;
        
        return new RatingDto(
            id,
            doc.getString("musicianId"),
            doc.getString("userId"),
            doc.getInteger("rating", 0),
            doc.getString("comment"),
            doc.getDate("createdAt")
        );
    }
    
    // Convert to MongoDB Document
    public Document toDocument() {
        Document doc = new Document();
        
        if (id != null && !id.isEmpty()) {
            try {
                doc.append("_id", new ObjectId(id));
            } catch (IllegalArgumentException e) {
                // Keep the ID as is if it's not a valid ObjectId
            }
        }
        
        return doc
            .append("musicianId", musicianId)
            .append("userId", userId)
            .append("rating", rating)
            .append("comment", comment)
            .append("createdAt", createdAt);
    }
    
    // Validate rating is between 1 and 5
    private int validateRating(int ratingValue) {
        if (ratingValue < 1) return 1;
        if (ratingValue > 5) return 5;
        return ratingValue;
    }

    // Getters and setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getMusicianId() {
        return musicianId;
    }

    public void setMusicianId(String musicianId) {
        this.musicianId = musicianId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public int getRating() {
        return rating;
    }

    public void setRating(int rating) {
        this.rating = validateRating(rating);
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }
}
