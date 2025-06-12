package com.multimedia.ms.model;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.bson.Document;
import org.bson.types.ObjectId;

/**
 * Data transfer object for musician profile information
 */
public class MusicianProfileDto {
    private String id;
    private String userId;
    private String name;
    private String biography;
    private List<String> genres;
    private List<String> instruments;
    private Date createdAt;
    private Date updatedAt;
    private double averageRating;
    private int totalRatings;
    
    public MusicianProfileDto() {
        this.genres = new ArrayList<>();
        this.instruments = new ArrayList<>();
        this.createdAt = new Date();
        this.updatedAt = new Date();
    }
    
    public MusicianProfileDto(String userId, String name, String biography) {
        this();
        this.userId = userId;
        this.name = name;
        this.biography = biography;
    }
    
    public MusicianProfileDto(String id, String userId, String name, String biography, 
                              List<String> genres, List<String> instruments, 
                              Date createdAt, Date updatedAt, double averageRating, int totalRatings) {
        this.id = id;
        this.userId = userId;
        this.name = name;
        this.biography = biography;
        this.genres = genres;
        this.instruments = instruments;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.averageRating = averageRating;
        this.totalRatings = totalRatings;
    }
    
    // Factory method to create from MongoDB Document
    public static MusicianProfileDto fromDocument(Document doc) {
        if (doc == null) return null;
        
        ObjectId objectId = doc.getObjectId("_id");
        String id = objectId != null ? objectId.toString() : null;
        
        List<String> genres = new ArrayList<>();
        List<String> instruments = new ArrayList<>();
        
        @SuppressWarnings("unchecked")
        List<String> docGenres = (List<String>) doc.get("genres");
        if (docGenres != null) {
            genres.addAll(docGenres);
        }
        
        @SuppressWarnings("unchecked")
        List<String> docInstruments = (List<String>) doc.get("instruments");
        if (docInstruments != null) {
            instruments.addAll(docInstruments);
        }
        
        return new MusicianProfileDto(
            id,
            doc.getString("userId"),
            doc.getString("name"),
            doc.getString("biography"),
            genres,
            instruments,
            doc.getDate("createdAt"),
            doc.getDate("updatedAt"),
            doc.getDouble("averageRating"),
            doc.getInteger("totalRatings", 0)
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
            .append("userId", userId)
            .append("name", name)
            .append("biography", biography)
            .append("genres", genres)
            .append("instruments", instruments)
            .append("createdAt", createdAt)
            .append("updatedAt", new Date())
            .append("averageRating", averageRating)
            .append("totalRatings", totalRatings);
    }

    // Getters and setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getBiography() {
        return biography;
    }

    public void setBiography(String biography) {
        this.biography = biography;
    }

    public List<String> getGenres() {
        return genres;
    }

    public void setGenres(List<String> genres) {
        this.genres = genres;
    }

    public List<String> getInstruments() {
        return instruments;
    }

    public void setInstruments(List<String> instruments) {
        this.instruments = instruments;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public Date getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Date updatedAt) {
        this.updatedAt = updatedAt;
    }

    public double getAverageRating() {
        return averageRating;
    }

    public void setAverageRating(double averageRating) {
        this.averageRating = averageRating;
    }

    public int getTotalRatings() {
        return totalRatings;
    }

    public void setTotalRatings(int totalRatings) {
        this.totalRatings = totalRatings;
    }
}
