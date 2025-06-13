package com.multimedia.ms.controller;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.multimedia.ms.model.Database;
import jakarta.json.Json;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObjectBuilder;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.bson.Document;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

/**
 * Servlet para depuración y pruebas de la conexión con MongoDB
 */
@WebServlet("/debug/mongodb")
public class MongoDebugServlet extends HttpServlet {
    
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();
        
        try {
            // Crear instancia de Database para obtener conexión a MongoDB
            Database database = new Database();
            MongoClient mongoClient = database.getMongoClient();
            
            // Listar bases de datos para confirmar conexión
            List<String> databases = new ArrayList<>();
            mongoClient.listDatabaseNames().forEach(name -> databases.add(name));
            
            // Obtener la base de datos del portafolio
            MongoDatabase db = mongoClient.getDatabase("musicianportfolio");
            
            // Contar documentos en colecciones
            MongoCollection<Document> profiles = db.getCollection("musicianProfiles");
            long profilesCount = profiles.countDocuments();
            
            MongoCollection<Document> ratings = db.getCollection("ratings");
            long ratingsCount = ratings.countDocuments();
            
            MongoCollection<Document> multimedia = db.getCollection("multimedia.files");
            long multimediaCount = multimedia.countDocuments();
            
            // Construir información de colecciones
            JsonArrayBuilder collectionsBuilder = Json.createArrayBuilder();
            db.listCollectionNames().forEach(name -> {
                collectionsBuilder.add(Json.createObjectBuilder()
                    .add("name", name)
                    .add("count", db.getCollection(name).countDocuments()));
            });
            
            // Construir respuesta
            JsonObjectBuilder builder = Json.createObjectBuilder()
                .add("status", "success")
                .add("connection", "active")
                .add("databases", databases.toString())
                .add("databaseName", "musicianportfolio")
                .add("collections", collectionsBuilder)
                .add("stats", Json.createObjectBuilder()
                    .add("profilesCount", profilesCount)
                    .add("ratingsCount", ratingsCount)
                    .add("multimediaCount", multimediaCount));
            
            out.println(builder.build().toString());
        } catch (Exception e) {
            // Responder con error en formato JSON
            JsonObjectBuilder builder = Json.createObjectBuilder()
                .add("status", "error")
                .add("message", e.getMessage())
                .add("stackTrace", e.getStackTrace().toString());
            
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.println(builder.build().toString());
            
            // Imprimir stack trace en el log del servidor
            e.printStackTrace();
        }
    }
}
