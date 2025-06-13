package com.multimedia.ms.controller;

import com.multimedia.ms.dao.MusicianProfileDao;
import com.multimedia.ms.dao.MultimediaDao;
import com.multimedia.ms.model.Database;
import com.multimedia.ms.model.MusicianProfileDto;
import com.multimedia.ms.model.MultimediaDto;
import io.github.cdimascio.dotenv.Dotenv;
import jakarta.json.Json;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import jakarta.json.JsonReader;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.apache.commons.io.FilenameUtils;

/**
 * Servlet controller for multimedia files
 */
@WebServlet(name = "MultimediaServlet", urlPatterns = {"/multimedia/*"})
@MultipartConfig(
    fileSizeThreshold = 1024 * 1024,      // 1 MB
    maxFileSize = 1024 * 1024 * 10,       // 10 MB
    maxRequestSize = 1024 * 1024 * 100    // 100 MB
)
public class MultimediaServlet extends HttpServlet {
    
    private final MultimediaDao multimediaDao;
    private final MusicianProfileDao profileDao;
    private final Set<String> allowedFileTypes;
    private final long maxFileSize;
    private final String uploadTempDir;
    
    public MultimediaServlet() {
        this.multimediaDao = new MultimediaDao();
        this.profileDao = new MusicianProfileDao();
        
        // Crear instancia de Database para acceder a las variables de entorno
        Database database = new Database();
        this.allowedFileTypes = new HashSet<>(Arrays.asList(
                database.getConfigValue("ALLOWED_FILE_TYPES", "mp3,mp4,jpg,jpeg,png").toLowerCase().split(",")));
        this.maxFileSize = Long.parseLong(database.getConfigValue("MAX_FILE_SIZE", "10485760")); // Default 10MB
        this.uploadTempDir = database.getConfigValue("UPLOAD_TEMP_DIR", System.getProperty("java.io.tmpdir"));
        
        // Ensure temp directory exists
        File tempDir = new File(uploadTempDir);
        if (!tempDir.exists()) {
            tempDir.mkdirs();
        }
    }

    /**
     * Handles the HTTP GET method for:
     * - Getting all multimedia files for a musician
     * - Getting file metadata by ID
     * - Downloading a file
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        String pathInfo = request.getPathInfo();
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        
        try {
            if (pathInfo == null || pathInfo.equals("/")) {
                // List files route: /multimedia?musicianId=xxx&publicOnly=true
                String musicianId = request.getParameter("musicianId");
                boolean publicOnly = Boolean.parseBoolean(request.getParameter("publicOnly"));
                
                if (musicianId == null || musicianId.isEmpty()) {
                    handleError(response, HttpServletResponse.SC_BAD_REQUEST, "Musician ID is required");
                    return;
                }
                
                List<MultimediaDto> files = multimediaDao.getFilesByMusician(musicianId, publicOnly);
                JsonArrayBuilder filesArray = Json.createArrayBuilder();
                
                for (MultimediaDto file : files) {
                    JsonObjectBuilder fileJson = Json.createObjectBuilder()
                        .add("id", file.getId())
                        .add("fileId", file.getFileId())
                        .add("filename", file.getFilename())
                        .add("contentType", file.getContentType())
                        .add("musicianId", file.getMusicianId())
                        .add("title", file.getTitle())
                        .add("description", file.getDescription() != null ? file.getDescription() : "")
                        .add("mediaType", file.getMediaType())
                        .add("fileSize", file.getFileSize())
                        .add("isPublic", file.isIsPublic())
                        .add("uploadDate", file.getUploadDate().getTime());
                    filesArray.add(fileJson);
                }
                
                JsonObject result = Json.createObjectBuilder()
                    .add("files", filesArray)
                    .build();
                
                try (PrintWriter out = response.getWriter()) {
                    out.print(result.toString());
                    out.flush();
                }
                
            } else if (pathInfo.matches("^/[^/]+$")) {
                // Get file metadata: /multimedia/{id}
                String id = pathInfo.substring(1);
                MultimediaDto file = multimediaDao.getFileById(id);
                
                if (file == null) {
                    handleError(response, HttpServletResponse.SC_NOT_FOUND, "File not found");
                    return;
                }
                
                JsonObject result = Json.createObjectBuilder()
                    .add("id", file.getId())
                    .add("fileId", file.getFileId())
                    .add("filename", file.getFilename())
                    .add("contentType", file.getContentType())
                    .add("musicianId", file.getMusicianId())
                    .add("title", file.getTitle())
                    .add("description", file.getDescription() != null ? file.getDescription() : "")
                    .add("mediaType", file.getMediaType())
                    .add("fileSize", file.getFileSize())
                    .add("isPublic", file.isIsPublic())
                    .add("uploadDate", file.getUploadDate().getTime())
                    .build();
                
                try (PrintWriter out = response.getWriter()) {
                    out.print(result.toString());
                    out.flush();
                }
                
            } else if (pathInfo.matches("^/[^/]+/download$")) {
                // Download file: /multimedia/{id}/download
                String id = pathInfo.substring(1, pathInfo.lastIndexOf("/"));
                MultimediaDto file = multimediaDao.getFileById(id);
                
                if (file == null) {
                    handleError(response, HttpServletResponse.SC_NOT_FOUND, "File not found");
                    return;
                }
                
                response.setContentType(file.getContentType());
                response.setHeader("Content-Disposition", "attachment; filename=\"" + file.getFilename() + "\"");
                response.setContentLength((int) file.getFileSize());
                
                try (InputStream fileStream = multimediaDao.getFileContent(file.getFileId())) {
                    fileStream.transferTo(response.getOutputStream());
                }
            } else {
                handleError(response, HttpServletResponse.SC_NOT_FOUND, "Resource not found");
            }
        } catch (Exception e) {
            handleError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    /**
     * Handles the HTTP POST method for uploading a new file
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        
        try {
            // Validate musician exists
            String musicianId = request.getParameter("musicianId");
            if (musicianId == null || musicianId.isEmpty()) {
                handleError(response, HttpServletResponse.SC_BAD_REQUEST, "Musician ID is required");
                return;
            }
            
            MusicianProfileDto musician = profileDao.getProfileById(musicianId);
            if (musician == null) {
                handleError(response, HttpServletResponse.SC_NOT_FOUND, "Musician profile not found");
                return;
            }
            
            // Get form data parameters
            String title = request.getParameter("title");
            String description = request.getParameter("description");
            String isPublicStr = request.getParameter("isPublic");
            boolean isPublic = isPublicStr != null && Boolean.parseBoolean(isPublicStr);
            
            // Validate required fields
            if (title == null || title.isEmpty()) {
                handleError(response, HttpServletResponse.SC_BAD_REQUEST, "Title is required");
                return;
            }
            
            // Get file part
            Part filePart = request.getPart("file");
            if (filePart == null) {
                handleError(response, HttpServletResponse.SC_BAD_REQUEST, "No file uploaded");
                return;
            }
            
            // Validate file
            long fileSize = filePart.getSize();
            if (fileSize > maxFileSize) {
                handleError(response, HttpServletResponse.SC_BAD_REQUEST, 
                        "File size exceeds maximum allowed (" + (maxFileSize / 1024 / 1024) + "MB)");
                return;
            }
            
            String fileName = filePart.getSubmittedFileName();
            String extension = FilenameUtils.getExtension(fileName).toLowerCase();
            
            if (!allowedFileTypes.contains(extension)) {
                handleError(response, HttpServletResponse.SC_BAD_REQUEST, 
                        "File type not allowed. Allowed types: " + String.join(", ", allowedFileTypes));
                return;
            }
            
            // Create temporary file
            String fileId = UUID.randomUUID().toString();
            String contentType = filePart.getContentType();
            String mediaType = determineMediaType(contentType);
            
            // Save to MongoDB
            try (InputStream inputStream = filePart.getInputStream()) {
                MultimediaDto multimediaDto = new MultimediaDto();
                multimediaDto.setFileId(fileId);
                multimediaDto.setFilename(fileName);
                multimediaDto.setContentType(contentType);
                multimediaDto.setMusicianId(musicianId);
                multimediaDto.setTitle(title);
                multimediaDto.setDescription(description);
                multimediaDto.setMediaType(mediaType);
                multimediaDto.setFileSize(fileSize);
                multimediaDto.setIsPublic(isPublic);
                
                String savedId = multimediaDao.saveFile(multimediaDto, inputStream);
                
                JsonObject result = Json.createObjectBuilder()
                    .add("id", savedId)
                    .add("message", "File uploaded successfully")
                    .build();
                
                try (PrintWriter out = response.getWriter()) {
                    out.print(result.toString());
                    out.flush();
                }
            }
            
        } catch (Exception e) {
            handleError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }
    
    /**
     * Handles the HTTP PUT method for updating file metadata
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        String pathInfo = request.getPathInfo();
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        
        if (pathInfo == null || !pathInfo.matches("^/[^/]+$")) {
            handleError(response, HttpServletResponse.SC_NOT_FOUND, "Resource not found");
            return;
        }
        
        String id = pathInfo.substring(1);
        
        try {
            // Read JSON from request
            JsonObject data;
            try (JsonReader jsonReader = Json.createReader(request.getInputStream())) {
                data = jsonReader.readObject();
            }
            
            MultimediaDto file = multimediaDao.getFileById(id);
            if (file == null) {
                handleError(response, HttpServletResponse.SC_NOT_FOUND, "File not found");
                return;
            }
            
            // Update fields if present
            if (data.containsKey("title")) {
                String title = data.getString("title");
                if (title == null || title.isEmpty()) {
                    handleError(response, HttpServletResponse.SC_BAD_REQUEST, "Title cannot be empty");
                    return;
                }
                file.setTitle(title);
            }
            
            if (data.containsKey("description")) {
                file.setDescription(data.getString("description"));
            }
            
            if (data.containsKey("isPublic")) {
                file.setIsPublic(data.getBoolean("isPublic"));
            }
            
            // Save updated metadata
            multimediaDao.updateFile(file);
            
            JsonObject result = Json.createObjectBuilder()
                .add("id", file.getId())
                .add("message", "File metadata updated successfully")
                .build();
            
            try (PrintWriter out = response.getWriter()) {
                out.print(result.toString());
                out.flush();
            }
            
        } catch (Exception e) {
            handleError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }
    
    /**
     * Handles the HTTP DELETE method for deleting a file
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        String pathInfo = request.getPathInfo();
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        
        if (pathInfo == null || !pathInfo.matches("^/[^/]+$")) {
            handleError(response, HttpServletResponse.SC_NOT_FOUND, "Resource not found");
            return;
        }
        
        String id = pathInfo.substring(1);
        
        try {
            MultimediaDto file = multimediaDao.getFileById(id);
            if (file == null) {
                handleError(response, HttpServletResponse.SC_NOT_FOUND, "File not found");
                return;
            }
            
            // Delete file from MongoDB
            multimediaDao.deleteFile(id);
            
            JsonObject result = Json.createObjectBuilder()
                .add("message", "File deleted successfully")
                .build();
            
            try (PrintWriter out = response.getWriter()) {
                out.print(result.toString());
                out.flush();
            }
            
        } catch (Exception e) {
            handleError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    /**
     * Determines the media type based on content type
     * 
     * @param contentType MIME type of the file
     * @return Media type (audio, video, image)
     */
    private String determineMediaType(String contentType) {
        if (contentType != null) {
            if (contentType.startsWith("audio/")) {
                return "audio";
            } else if (contentType.startsWith("video/")) {
                return "video";
            } else if (contentType.startsWith("image/")) {
                return "image";
            }
        }
        return "other";
    }
    
    /**
     * Handles error response
     * 
     * @param response HttpServletResponse
     * @param statusCode HTTP status code
     * @param message Error message
     * @throws IOException If an I/O error occurs
     */
    private void handleError(HttpServletResponse response, int statusCode, String message) throws IOException {
        response.setStatus(statusCode);
        
        JsonObject errorResponse = Json.createObjectBuilder()
                .add("error", message)
                .build();
        
        try (PrintWriter out = response.getWriter()) {
            out.print(errorResponse.toString());
            out.flush();
        }
    }
}
