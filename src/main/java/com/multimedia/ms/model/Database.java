package com.multimedia.ms.model;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.gridfs.GridFSBucket;
import com.mongodb.client.gridfs.GridFSBuckets;
import io.github.cdimascio.dotenv.Dotenv;

/**
 * Database connection manager for MongoDB
 */
public class Database {
    private Dotenv dotenv;
    private MongoClient mongoClient;
    private MongoDatabase database;
    private GridFSBucket gridFSBucket;
    
    public Database() {
        try {
            this.dotenv = Dotenv.load();
            validateEnvVariables();
            initializeMongoConnection();
        } catch (Exception e) {
            throw new RuntimeException("Error initializing database connection: " + e.getMessage(), e);
        }
    }
    
    private void validateEnvVariables() {
        if (dotenv == null) {
            throw new RuntimeException("Failed to load .env file");
        }
        
        if (dotenv.get("MONGODB_CONNECTION_STRING") == null) {
            throw new RuntimeException("MongoDB connection string not found in environment variables");
        }
        
        if (dotenv.get("MONGODB_DATABASE") == null) {
            throw new RuntimeException("MongoDB database name not found in environment variables");
        }
    }
    
    private void initializeMongoConnection() {
        String connectionString = dotenv.get("MONGODB_CONNECTION_STRING");
        String databaseName = dotenv.get("MONGODB_DATABASE");
        
        this.mongoClient = MongoClients.create(connectionString);
        this.database = mongoClient.getDatabase(databaseName);
        this.gridFSBucket = GridFSBuckets.create(database, "files");
    }
    
    public MongoClient getMongoClient() {
        return this.mongoClient;
    }
    
    public MongoDatabase getDatabase() {
        return this.database;
    }
    
    public GridFSBucket getGridFSBucket() {
        return this.gridFSBucket;
    }
    
    public void close() {
        if (this.mongoClient != null) {
            this.mongoClient.close();
        }
    }
    
    // Helper method to get configuration values
    public String getConfigValue(String key, String defaultValue) {
        String value = dotenv.get(key);
        return (value != null) ? value : defaultValue;
    }
}
