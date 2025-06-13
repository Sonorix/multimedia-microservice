package com.multimedia.ms;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.gridfs.GridFSBucket;
import com.mongodb.client.gridfs.GridFSBuckets;
import com.mongodb.client.model.Filters;
import com.multimedia.ms.model.MusicianProfileDto;
import io.github.cdimascio.dotenv.Dotenv;
import org.bson.Document;
import org.bson.types.ObjectId;

import java.util.ArrayList;
import java.util.List;

/**
 * Tester class to directly verify MongoDB integration without deploying the full application
 */
public class MongoDBTester {

    private static MongoClient mongoClient;
    private static MongoDatabase database;
    private static GridFSBucket gridFSBucket;

    public static void main(String[] args) {
        try {
            // Initialize connection
            initMongoDB();
            
            // Run tests
            testMusicianProfiles();
            testRatings();
            testMultimediaFiles();
            
            System.out.println("\n✅ All tests completed successfully!");
        } catch (Exception e) {
            System.err.println("❌ Error during tests: " + e.getMessage());
            e.printStackTrace();
        } finally {
            // Close connection
            if (mongoClient != null) {
                mongoClient.close();
                System.out.println("\nMongoDB connection closed");
            }
        }
    }
    
    private static void initMongoDB() {
        System.out.println("Initializing MongoDB connection...");
        
        try {
            // Load environment variables
            Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();
            String connectionString = dotenv.get("MONGODB_CONNECTION_STRING", "mongodb://localhost:27017");
            String dbName = dotenv.get("MONGODB_DATABASE", "multimedia_db");
            
            // Create MongoDB client and database
            mongoClient = MongoClients.create(connectionString);
            database = mongoClient.getDatabase(dbName);
            
            // Create GridFS bucket
            gridFSBucket = GridFSBuckets.create(database);
            
            System.out.println("✅ MongoDB connection established!");
            System.out.println("   Connection: " + connectionString);
            System.out.println("   Database: " + dbName);
            
            // List all collections
            System.out.println("\nAvailable collections:");
            database.listCollectionNames().forEach(name -> System.out.println("   - " + name));
            
        } catch (Exception e) {
            System.err.println("❌ Error connecting to MongoDB: " + e.getMessage());
            throw e;
        }
    }
    
    private static void testMusicianProfiles() {
        System.out.println("\n--- Testing Musician Profiles ---");
        
        try {
            MongoCollection<Document> collection = database.getCollection("musicianProfiles");
            
            // Count documents
            long count = collection.countDocuments();
            System.out.println("Found " + count + " musician profiles");
            
            if (count > 0) {
                // Get one musician profile
                Document doc = collection.find().first();
                ObjectId id = doc.getObjectId("_id");
                MusicianProfileDto profile = MusicianProfileDto.fromDocument(doc);
                
                System.out.println("Sample musician:");
                System.out.println("   ID: " + id);
                System.out.println("   Name: " + profile.getName());
                System.out.println("   Biography: " + profile.getBiography());
                System.out.println("   Genres: " + String.join(", ", profile.getGenres()));
                System.out.println("   Instruments: " + String.join(", ", profile.getInstruments()));
                System.out.println("   Average Rating: " + profile.getAverageRating());
                
                // Query by ID to verify we can retrieve specific profiles
                Document found = collection.find(Filters.eq("_id", id)).first();
                if (found != null) {
                    System.out.println("✅ Successfully retrieved by ID");
                } else {
                    throw new RuntimeException("Failed to retrieve musician profile by ID");
                }
            }
            
            System.out.println("✅ Musician Profiles test passed!");
        } catch (Exception e) {
            System.err.println("❌ Error testing musician profiles: " + e.getMessage());
            throw e;
        }
    }
    
    private static void testRatings() {
        System.out.println("\n--- Testing Ratings ---");
        
        try {
            MongoCollection<Document> collection = database.getCollection("ratings");
            
            // Count documents
            long count = collection.countDocuments();
            System.out.println("Found " + count + " ratings");
            
            if (count > 0) {
                // Get one rating
                Document doc = collection.find().first();
                ObjectId id = doc.getObjectId("_id");
                String musicianId = doc.getString("musicianId");
                String userId = doc.getString("userId");
                double ratingValue = doc.getDouble("rating");
                String comment = doc.getString("comment");
                
                System.out.println("Sample rating:");
                System.out.println("   ID: " + id);
                System.out.println("   Musician ID: " + musicianId);
                System.out.println("   User ID: " + userId);
                System.out.println("   Rating: " + ratingValue);
                System.out.println("   Comment: " + comment);
                
                // Query by musician ID to verify we can retrieve ratings for a musician
                List<Document> found = collection.find(Filters.eq("musicianId", musicianId))
                    .into(new ArrayList<>());
                
                if (!found.isEmpty()) {
                    System.out.println("✅ Successfully retrieved " + found.size() + " ratings for musician");
                } else {
                    throw new RuntimeException("Failed to retrieve ratings by musician ID");
                }
            }
            
            System.out.println("✅ Ratings test passed!");
        } catch (Exception e) {
            System.err.println("❌ Error testing ratings: " + e.getMessage());
            throw e;
        }
    }
    
    private static void testMultimediaFiles() {
        System.out.println("\n--- Testing Multimedia Files ---");
        
        try {
            MongoCollection<Document> collection = database.getCollection("fs.files");
            
            // Count documents
            long count = collection.countDocuments();
            System.out.println("Found " + count + " multimedia files");
            
            if (count > 0) {
                // Get one file metadata
                Document doc = collection.find().first();
                ObjectId id = doc.getObjectId("_id");
                String filename = doc.getString("filename");
                String contentType = doc.getString("contentType");
                long length = doc.getLong("length");
                
                Document metadata = (Document) doc.get("metadata");
                String musicianId = metadata != null ? metadata.getString("musicianId") : null;
                String title = metadata != null ? metadata.getString("title") : null;
                
                System.out.println("Sample file:");
                System.out.println("   ID: " + id);
                System.out.println("   Filename: " + filename);
                System.out.println("   Content Type: " + contentType);
                System.out.println("   Size: " + length + " bytes");
                System.out.println("   Musician ID: " + musicianId);
                System.out.println("   Title: " + title);
                
                // Verify chunks collection
                long chunkCount = database.getCollection("fs.chunks").countDocuments();
                System.out.println("   Found " + chunkCount + " file chunks");
                
                System.out.println("✅ Multimedia Files test passed!");
            }
        } catch (Exception e) {
            System.err.println("❌ Error testing multimedia files: " + e.getMessage());
            throw e;
        }
    }
}
