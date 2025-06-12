package com.multimedia.ms;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import io.github.cdimascio.dotenv.Dotenv;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

/**
 * Helper class for manual integration testing
 * This is not for automated testing but to help verify endpoints
 * during development and manual testing
 */
public class IntegrationTestHelper {
    
    private static final String BASE_URL = "http://localhost:8080/Multimedia-microservice/resources";
    private static MongoClient mongoClient;
    private static MongoDatabase database;
    
    /**
     * Initialize MongoDB connection using env vars
     */
    public static void initMongoDB() {
        try {
            Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();
            String connectionString = dotenv.get("MONGODB_CONNECTION_STRING", "mongodb://localhost:27017");
            String dbName = dotenv.get("MONGODB_DATABASE", "multimedia_db");
            
            mongoClient = MongoClients.create(connectionString);
            database = mongoClient.getDatabase(dbName);
            
            System.out.println("MongoDB connected successfully to: " + connectionString);
            System.out.println("Using database: " + dbName);
            
            // Print collection names to verify connection
            System.out.println("Available collections:");
            database.listCollectionNames().forEach(name -> System.out.println("- " + name));
        } catch (Exception e) {
            System.err.println("Failed to connect to MongoDB: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Close MongoDB connection
     */
    public static void closeMongoDB() {
        if (mongoClient != null) {
            mongoClient.close();
            System.out.println("MongoDB connection closed");
        }
    }
    
    /**
     * Test a GET endpoint
     * 
     * @param endpoint The endpoint path (without base URL)
     * @return Response as string
     */
    public static String testGetEndpoint(String endpoint) {
        try {
            URL url = new URL(BASE_URL + endpoint);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Accept", "application/json");
            
            int responseCode = connection.getResponseCode();
            System.out.println("\nTesting GET " + endpoint);
            System.out.println("Response Code: " + responseCode);
            
            InputStream responseStream = (responseCode >= 400) 
                    ? connection.getErrorStream() 
                    : connection.getInputStream();
                    
            String response = readResponseStream(responseStream);
            System.out.println("Response Body: " + response);
            
            return response;
        } catch (Exception e) {
            System.err.println("Error testing endpoint: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * Test a POST endpoint
     * 
     * @param endpoint The endpoint path (without base URL)
     * @param jsonPayload The JSON request body
     * @return Response as string
     */
    public static String testPostEndpoint(String endpoint, String jsonPayload) {
        try {
            URL url = new URL(BASE_URL + endpoint);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("Accept", "application/json");
            connection.setDoOutput(true);
            
            // Send request body
            connection.getOutputStream().write(jsonPayload.getBytes(StandardCharsets.UTF_8));
            
            int responseCode = connection.getResponseCode();
            System.out.println("\nTesting POST " + endpoint);
            System.out.println("Payload: " + jsonPayload);
            System.out.println("Response Code: " + responseCode);
            
            InputStream responseStream = (responseCode >= 400) 
                    ? connection.getErrorStream() 
                    : connection.getInputStream();
                    
            String response = readResponseStream(responseStream);
            System.out.println("Response Body: " + response);
            
            return response;
        } catch (Exception e) {
            System.err.println("Error testing endpoint: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * Helper method to read response stream to string
     */
    private static String readResponseStream(InputStream inputStream) throws IOException {
        try (Scanner scanner = new Scanner(inputStream, StandardCharsets.UTF_8.name())) {
            return scanner.useDelimiter("\\A").next();
        }
    }
    
    /**
     * Main method for manual testing
     */
    public static void main(String[] args) {
        // Initialize MongoDB connection
        initMongoDB();
        
        try {
            // Test endpoints
            System.out.println("==== Testing Endpoints ====");
            
            // Test profiles endpoint
            testGetEndpoint("/profiles");
            
            // Test creating a new profile
            String profileJson = "{"
                    + "\"userId\": \"user123\","
                    + "\"name\": \"Test Musician\","
                    + "\"biography\": \"This is a test musician for integration testing\","
                    + "\"genres\": [\"Rock\", \"Jazz\"],"
                    + "\"instruments\": [\"Guitar\", \"Piano\"]"
                    + "}";
            String createProfileResponse = testPostEndpoint("/profiles", profileJson);
            
            // Extract profile ID from response (simplified parsing)
            String profileId = null;
            if (createProfileResponse != null && createProfileResponse.contains("\"id\":")) {
                profileId = createProfileResponse.split("\"id\":")[1].split(",")[0].trim().replace("\"", "");
                System.out.println("Created profile ID: " + profileId);
                
                // Test getting the created profile
                testGetEndpoint("/profiles/" + profileId);
            }
            
        } finally {
            // Close MongoDB connection
            closeMongoDB();
        }
    }
}
