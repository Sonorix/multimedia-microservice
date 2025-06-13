package com.multimedia.ms.controller;

import com.multimedia.ms.dao.MusicianProfileDao;
import com.multimedia.ms.dao.MultimediaDao;
import com.multimedia.ms.model.Database;
import com.multimedia.ms.model.MusicianProfileDto;
import com.multimedia.ms.model.MultimediaDto;
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
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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
        
        // Definir tipos de archivos permitidos con valores predeterminados seguros
        String defaultTypes = "mp3,mp4,jpg,jpeg,png";
        String configuredTypes = database.getConfigValue("ALLOWED_FILE_TYPES", defaultTypes);
        this.allowedFileTypes = new HashSet<>(Arrays.asList(configuredTypes.toLowerCase().split(",")));
        
        // Configurar tamaño máximo de archivo con valor predeterminado de 10MB
        String defaultMaxSize = "10485760";
        this.maxFileSize = Long.parseLong(database.getConfigValue("MAX_FILE_SIZE", defaultMaxSize));
        
        // Configurar directorio temporal
        String defaultTempDir = System.getProperty("java.io.tmpdir");
        this.uploadTempDir = database.getConfigValue("UPLOAD_TEMP_DIR", defaultTempDir);
        
        // Asegurar que el directorio temporal existe y tiene permisos de escritura
        try {
            File tempDir = new File(uploadTempDir);
            if (!tempDir.exists()) {
                boolean created = tempDir.mkdirs();
                if (!created) {
                    System.err.println("Warning: Could not create upload temp directory: " + uploadTempDir);
                }
            }
            
            if (!tempDir.canWrite()) {
                System.err.println("Warning: Upload temp directory is not writable: " + uploadTempDir);
            }
        } catch (SecurityException e) {
            System.err.println("Security exception creating temp directory: " + e.getMessage());
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
                String publicOnlyStr = request.getParameter("publicOnly"); 
                boolean publicOnly = publicOnlyStr != null && (publicOnlyStr.equalsIgnoreCase("true") || publicOnlyStr.equals("1"));
                
                List<MultimediaDto> files;
                JsonObjectBuilder resultBuilder = Json.createObjectBuilder();
                
                if (musicianId != null && !musicianId.isEmpty()) {
                    // Si se proporciona musicianId, verificar que existe el músico
                    MusicianProfileDto musician = profileDao.getProfileById(musicianId);
                    if (musician == null) {
                        handleError(response, HttpServletResponse.SC_NOT_FOUND, "Musician not found with ID: " + musicianId);
                        return;
                    }
                    
                    // Get files for specific musician
                    if (publicOnly) {
                        files = multimediaDao.getPublicFilesByMusicianId(musicianId);
                    } else {
                        files = multimediaDao.getFilesByMusicianId(musicianId);
                    }
                    
                    // Añadir musicianId al resultado
                    resultBuilder.add("musicianId", musicianId);
                } else {
                    // Si no se proporciona musicianId, obtener todos los archivos
                    try {
                        files = multimediaDao.getAllFiles();
                    } catch (Exception e) {
                        handleError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error retrieving all files: " + e.toString());
                        e.printStackTrace();
                        return;
                    }
                }
                
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
                MultimediaDto file = multimediaDao.getFileMetadata(id);
                
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
                MultimediaDto file = multimediaDao.getFileMetadata(id);
                
                if (file == null) {
                    handleError(response, HttpServletResponse.SC_NOT_FOUND, "File not found");
                    return;
                }
                
                response.setContentType(file.getContentType());
                response.setHeader("Content-Disposition", "attachment; filename=\"" + file.getFilename() + "\"");
                response.setContentLength((int) file.getFileSize());
                
                // Get file content
                byte[] fileContent = multimediaDao.downloadFile(file.getFileId());
                response.getOutputStream().write(fileContent);
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
            String contentType = filePart.getContentType();
            String mediaType = determineMediaType(contentType);
            
            // Save to MongoDB
            try (InputStream fileInputStream = filePart.getInputStream()) {
                MultimediaDto multimedia = new MultimediaDto();
                multimedia.setFilename(fileName);
                multimedia.setContentType(contentType);
                multimedia.setMediaType(mediaType);
                multimedia.setMusicianId(musicianId);
                multimedia.setTitle(title);
                multimedia.setDescription(description);
                multimedia.setFileSize(fileSize);
                multimedia.setIsPublic(isPublic);
                
                // Save file to MongoDB usando uploadFile (no saveFile)
                MultimediaDto savedFile = multimediaDao.uploadFile(multimedia, fileInputStream);
                
                JsonObject result = Json.createObjectBuilder()
                    .add("id", savedFile.getId())
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
            
            MultimediaDto file = multimediaDao.getFileMetadata(id);
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
            
            // Actualizar los metadatos usando el método implementado en MultimediaDao
            boolean updated = multimediaDao.updateFile(file);
            
            if (!updated) {
                handleError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to update file metadata");
                return;
            }
            
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
            MultimediaDto file = multimediaDao.getFileMetadata(id);
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
