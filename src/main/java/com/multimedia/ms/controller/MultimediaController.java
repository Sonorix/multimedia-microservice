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
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.Part;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.StreamingOutput;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.apache.commons.io.FilenameUtils;

/**
 * REST controller for multimedia files
 */
@Path("/multimedia")
public class MultimediaController {
    
    private final MultimediaDao multimediaDao;
    private final MusicianProfileDao profileDao;
    private final Set<String> allowedFileTypes;
    private final long maxFileSize;
    private final String uploadTempDir;
    
    public MultimediaController() {
        this.multimediaDao = new MultimediaDao();
        this.profileDao = new MusicianProfileDao();
        
        Dotenv dotenv = Database.getDotenv();
        this.allowedFileTypes = new HashSet<>(Arrays.asList(
                dotenv.get("ALLOWED_FILE_TYPES", "mp3,mp4,jpg,jpeg,png").toLowerCase().split(",")));
        this.maxFileSize = Long.parseLong(dotenv.get("MAX_FILE_SIZE", "10485760")); // Default 10MB
        this.uploadTempDir = dotenv.get("UPLOAD_TEMP_DIR", System.getProperty("java.io.tmpdir"));
        
        // Ensure temp directory exists
        File tempDir = new File(uploadTempDir);
        if (!tempDir.exists()) {
            tempDir.mkdirs();
        }
    }
    
    /**
     * Get all multimedia files for a musician
     * 
     * @param musicianId Musician ID
     * @param publicOnly Whether to return only public files
     * @return Response with all multimedia files
     */
    @GET
    @Path("/musician/{musicianId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getFilesByMusicianId(
            @PathParam("musicianId") String musicianId,
            @QueryParam("publicOnly") boolean publicOnly) {
        try {
            // First check if musician exists
            MusicianProfileDto musician = profileDao.getProfileById(musicianId);
            if (musician == null) {
                return Response
                    .status(Response.Status.NOT_FOUND)
                    .entity(Json.createObjectBuilder().add("message", "Musician not found").build().toString())
                    .build();
            }
            
            // Get files for this musician
            List<MultimediaDto> files;
            if (publicOnly) {
                files = multimediaDao.getPublicFilesByMusicianId(musicianId);
            } else {
                files = multimediaDao.getFilesByMusicianId(musicianId);
            }
            
            JsonArrayBuilder array = Json.createArrayBuilder();
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
                
                array.add(fileJson);
            }
            
            // Build response with musician info and files
            JsonObjectBuilder response = Json.createObjectBuilder()
                .add("musicianId", musicianId)
                .add("musicianName", musician.getName())
                .add("files", array);
            
            return Response.ok(response.build().toString()).build();
        } catch (Exception e) {
            return Response
                .status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(Json.createObjectBuilder().add("error", e.getMessage()).build().toString())
                .build();
        }
    }
    
    /**
     * Get metadata for a specific file
     * 
     * @param id File metadata ID
     * @return Response with file metadata
     */
    @GET
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getFileMetadata(@PathParam("id") String id) {
        try {
            MultimediaDto file = multimediaDao.getFileMetadata(id);
            
            if (file == null) {
                return Response
                    .status(Response.Status.NOT_FOUND)
                    .entity(Json.createObjectBuilder().add("message", "File not found").build().toString())
                    .build();
            }
            
            JsonObjectBuilder response = Json.createObjectBuilder()
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
            
            return Response.ok(response.build().toString()).build();
        } catch (Exception e) {
            return Response
                .status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(Json.createObjectBuilder().add("error", e.getMessage()).build().toString())
                .build();
        }
    }
    
    /**
     * Download a file
     * 
     * @param id File metadata ID
     * @return Response with file content
     */
    @GET
    @Path("/{id}/download")
    public Response downloadFile(@PathParam("id") String id) {
        try {
            MultimediaDto file = multimediaDao.getFileMetadata(id);
            
            if (file == null) {
                return Response
                    .status(Response.Status.NOT_FOUND)
                    .entity(Json.createObjectBuilder().add("message", "File not found").build().toString())
                    .build();
            }
            
            // Check if file exists in GridFS
            if (!multimediaDao.fileExists(file.getFileId())) {
                return Response
                    .status(Response.Status.NOT_FOUND)
                    .entity(Json.createObjectBuilder().add("message", "File content not found").build().toString())
                    .build();
            }
            
            // Stream the file content
            StreamingOutput fileStream = outputStream -> {
                try {
                    byte[] fileBytes = multimediaDao.downloadFile(file.getFileId());
                    outputStream.write(fileBytes);
                    outputStream.flush();
                } catch (Exception e) {
                    throw new RuntimeException("Error streaming file: " + e.getMessage(), e);
                }
            };
            
            return Response
                .ok(fileStream, file.getContentType())
                .header("Content-Disposition", "attachment; filename=\"" + file.getFilename() + "\"")
                .header("Content-Length", file.getFileSize())
                .build();
        } catch (Exception e) {
            return Response
                .status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(Json.createObjectBuilder().add("error", e.getMessage()).build().toString())
                .build();
        }
    }
    
    /**
     * Upload a new file
     * 
     * @param req HTTP request with multipart form data
     * @return Response with uploaded file metadata
     */
    @POST
    @Path("/upload")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    public Response uploadFile(@Context HttpServletRequest req) {
        String musicianId = null;
        String title = null;
        String description = null;
        boolean isPublic = false;
        Part filePart = null;
        
        try {
            // Get form fields
            Collection<Part> parts = req.getParts();
            
            for (Part part : parts) {
                String name = part.getName();
                
                if (name.equals("file")) {
                    filePart = part;
                } else {
                    // Handle other form fields
                    String value = new String(part.getInputStream().readAllBytes());
                    
                    if (name.equals("musicianId")) {
                        musicianId = value;
                    } else if (name.equals("title")) {
                        title = value;
                    } else if (name.equals("description")) {
                        description = value;
                    } else if (name.equals("isPublic")) {
                        isPublic = Boolean.parseBoolean(value);
                    }
                }
            }
            
            // Validate required fields
            if (filePart == null) {
                return Response
                    .status(Response.Status.BAD_REQUEST)
                    .entity(Json.createObjectBuilder().add("message", "No file uploaded").build().toString())
                    .build();
            }
            
            if (musicianId == null || title == null) {
                return Response
                    .status(Response.Status.BAD_REQUEST)
                    .entity(Json.createObjectBuilder().add("message", "Missing required fields: musicianId, title").build().toString())
                    .build();
            }
            
            // Check if musician exists
            MusicianProfileDto musician = profileDao.getProfileById(musicianId);
            if (musician == null) {
                return Response
                    .status(Response.Status.NOT_FOUND)
                    .entity(Json.createObjectBuilder().add("message", "Musician not found").build().toString())
                    .build();
            }
            
            // Validate file size
            if (filePart.getSize() > maxFileSize) {
                return Response
                    .status(Response.Status.BAD_REQUEST)
                    .entity(Json.createObjectBuilder()
                        .add("message", "File size exceeds maximum allowed size (" + maxFileSize / (1024 * 1024) + " MB)")
                        .build().toString())
                    .build();
            }
            
            // Validate file type
            String fileName = getSubmittedFileName(filePart);
            String extension = FilenameUtils.getExtension(fileName).toLowerCase();
            
            if (!allowedFileTypes.contains(extension)) {
                return Response
                    .status(Response.Status.BAD_REQUEST)
                    .entity(Json.createObjectBuilder()
                        .add("message", "File type not allowed. Allowed types: " + String.join(", ", allowedFileTypes))
                        .build().toString())
                    .build();
            }
            
            // Determine media type
            String contentType = filePart.getContentType();
            String mediaType = contentType.startsWith("audio/") ? "AUDIO" : 
                               contentType.startsWith("video/") ? "VIDEO" : "IMAGE";
            
            // Create multimedia metadata
            MultimediaDto multimedia = new MultimediaDto(
                null, // fileId will be set after upload
                fileName,
                contentType,
                musicianId,
                title,
                description,
                mediaType,
                filePart.getSize(),
                isPublic
            );
            
            // Upload file to GridFS
            MultimediaDto savedFile = multimediaDao.uploadFile(multimedia, filePart.getInputStream());
            
            // Build response
            JsonObjectBuilder response = Json.createObjectBuilder()
                .add("id", savedFile.getId())
                .add("fileId", savedFile.getFileId())
                .add("filename", savedFile.getFilename())
                .add("contentType", savedFile.getContentType())
                .add("musicianId", savedFile.getMusicianId())
                .add("title", savedFile.getTitle())
                .add("description", savedFile.getDescription() != null ? savedFile.getDescription() : "")
                .add("mediaType", savedFile.getMediaType())
                .add("fileSize", savedFile.getFileSize())
                .add("isPublic", savedFile.isIsPublic())
                .add("uploadDate", savedFile.getUploadDate().getTime());
            
            return Response
                .status(Response.Status.CREATED)
                .entity(response.build().toString())
                .build();
        } catch (ServletException | IOException e) {
            return Response
                .status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(Json.createObjectBuilder().add("error", e.getMessage()).build().toString())
                .build();
        }
    }
    
    /**
     * Update file metadata
     * 
     * @param id File metadata ID
     * @param metadataJson Metadata JSON
     * @return Response with updated metadata
     */
    @PUT
    @Path("/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateFileMetadata(@PathParam("id") String id, String metadataJson) {
        try {
            // Check if file exists
            MultimediaDto file = multimediaDao.getFileMetadata(id);
            if (file == null) {
                return Response
                    .status(Response.Status.NOT_FOUND)
                    .entity(Json.createObjectBuilder().add("message", "File not found").build().toString())
                    .build();
            }
            
            // Parse input JSON
            JsonReader jsonReader = Json.createReader(new StringReader(metadataJson));
            JsonObject json = jsonReader.readObject();
            jsonReader.close();
            
            // Update metadata fields
            if (json.containsKey("title")) {
                file.setTitle(json.getString("title"));
            }
            
            if (json.containsKey("description")) {
                file.setDescription(json.getString("description"));
            }
            
            if (json.containsKey("isPublic")) {
                file.setIsPublic(json.getBoolean("isPublic"));
            }
            
            // Save updates
            MultimediaDto updatedFile = multimediaDao.updateMetadata(file);
            
            // Build response
            JsonObjectBuilder response = Json.createObjectBuilder()
                .add("id", updatedFile.getId())
                .add("fileId", updatedFile.getFileId())
                .add("filename", updatedFile.getFilename())
                .add("contentType", updatedFile.getContentType())
                .add("musicianId", updatedFile.getMusicianId())
                .add("title", updatedFile.getTitle())
                .add("description", updatedFile.getDescription() != null ? updatedFile.getDescription() : "")
                .add("mediaType", updatedFile.getMediaType())
                .add("fileSize", updatedFile.getFileSize())
                .add("isPublic", updatedFile.isIsPublic())
                .add("uploadDate", updatedFile.getUploadDate().getTime());
            
            return Response.ok(response.build().toString()).build();
        } catch (Exception e) {
            return Response
                .status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(Json.createObjectBuilder().add("error", e.getMessage()).build().toString())
                .build();
        }
    }
    
    /**
     * Delete a file
     * 
     * @param id File metadata ID
     * @return Response with success or failure message
     */
    @DELETE
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteFile(@PathParam("id") String id) {
        try {
            boolean deleted = multimediaDao.deleteFile(id);
            
            if (deleted) {
                return Response
                    .ok(Json.createObjectBuilder().add("message", "File deleted successfully").build().toString())
                    .build();
            } else {
                return Response
                    .status(Response.Status.NOT_FOUND)
                    .entity(Json.createObjectBuilder().add("message", "File not found").build().toString())
                    .build();
            }
        } catch (Exception e) {
            return Response
                .status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(Json.createObjectBuilder().add("error", e.getMessage()).build().toString())
                .build();
        }
    }
    
    /**
     * Helper method to get the filename from a Part
     * This is needed because Part.getSubmittedFileName() is not always available in all servlet containers
     */
    private String getSubmittedFileName(Part part) {
        String contentDisposition = part.getHeader("content-disposition");
        String[] elements = contentDisposition.split(";");
        
        for (String element : elements) {
            if (element.trim().startsWith("filename")) {
                String filename = element.substring(element.indexOf('=') + 1).trim().replace("\"", "");
                if (filename.isEmpty()) {
                    // Generate a random filename if none provided
                    String extension = "bin";
                    if (part.getContentType() != null) {
                        if (part.getContentType().startsWith("image/")) {
                            extension = part.getContentType().replace("image/", "");
                        } else if (part.getContentType().startsWith("audio/")) {
                            extension = part.getContentType().replace("audio/", "");
                        } else if (part.getContentType().startsWith("video/")) {
                            extension = part.getContentType().replace("video/", "");
                        }
                    }
                    return UUID.randomUUID().toString() + "." + extension;
                }
                return filename;
            }
        }
        
        return "unknown";
    }
}
