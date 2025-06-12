package com.multimedia.ms.model;

import java.util.Date;
import org.bson.Document;
import org.bson.types.ObjectId;

/**
 * Data transfer object for multimedia file metadata
 */
public class MultimediaDto {
    private String id;             // MongoDB ObjectId
    private String fileId;         // GridFS file ID
    private String filename;       // Original filename
    private String contentType;    // MIME type
    private String musicianId;     // Associated musician ID
    private String title;          // Display title
    private String description;    // File description
    private String mediaType;      // Type of media (e.g., audio, image, video)
    private long fileSize;         // Size in bytes
    private Date uploadDate;       // When the file was uploaded
    private boolean isPublic;      // Whether the file is publicly accessible
    
    public MultimediaDto() {
        this.uploadDate = new Date();
        this.isPublic = true;
    }
    
    public MultimediaDto(String fileId, String filename, String contentType, String musicianId, 
                         String title, String description, String mediaType, long fileSize) {
        this();
        this.fileId = fileId;
        this.filename = filename;
        this.contentType = contentType;
        this.musicianId = musicianId;
        this.title = title;
        this.description = description;
        this.mediaType = mediaType;
        this.fileSize = fileSize;
    }
    
    public MultimediaDto(String id, String fileId, String filename, String contentType, String musicianId,
                         String title, String description, String mediaType, long fileSize, 
                         Date uploadDate, boolean isPublic) {
        this.id = id;
        this.fileId = fileId;
        this.filename = filename;
        this.contentType = contentType;
        this.musicianId = musicianId;
        this.title = title;
        this.description = description;
        this.mediaType = mediaType;
        this.fileSize = fileSize;
        this.uploadDate = uploadDate;
        this.isPublic = isPublic;
    }
    
    // Factory method to create from MongoDB Document
    public static MultimediaDto fromDocument(Document doc) {
        if (doc == null) return null;
        
        ObjectId objectId = doc.getObjectId("_id");
        String id = objectId != null ? objectId.toString() : null;
        
        return new MultimediaDto(
            id,
            doc.getString("fileId"),
            doc.getString("filename"),
            doc.getString("contentType"),
            doc.getString("musicianId"),
            doc.getString("title"),
            doc.getString("description"),
            doc.getString("mediaType"),
            doc.getLong("fileSize"),
            doc.getDate("uploadDate"),
            doc.getBoolean("isPublic", true)
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
            .append("fileId", fileId)
            .append("filename", filename)
            .append("contentType", contentType)
            .append("musicianId", musicianId)
            .append("title", title)
            .append("description", description)
            .append("mediaType", mediaType)
            .append("fileSize", fileSize)
            .append("uploadDate", uploadDate)
            .append("isPublic", isPublic);
    }
    
    // Determine media type from content type
    public static String determineMediaType(String contentType) {
        if (contentType == null || contentType.isEmpty()) {
            return "unknown";
        }
        
        contentType = contentType.toLowerCase();
        
        if (contentType.startsWith("image/")) {
            return "image";
        } else if (contentType.startsWith("audio/")) {
            return "audio";
        } else if (contentType.startsWith("video/")) {
            return "video";
        } else if (contentType.equals("application/pdf")) {
            return "document";
        } else {
            return "other";
        }
    }

    // Getters and setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getFileId() {
        return fileId;
    }

    public void setFileId(String fileId) {
        this.fileId = fileId;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
        // Auto-update media type when content type changes
        this.mediaType = determineMediaType(contentType);
    }

    public String getMusicianId() {
        return musicianId;
    }

    public void setMusicianId(String musicianId) {
        this.musicianId = musicianId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getMediaType() {
        return mediaType;
    }

    public void setMediaType(String mediaType) {
        this.mediaType = mediaType;
    }

    public long getFileSize() {
        return fileSize;
    }

    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }

    public Date getUploadDate() {
        return uploadDate;
    }

    public void setUploadDate(Date uploadDate) {
        this.uploadDate = uploadDate;
    }

    public boolean isIsPublic() {
        return isPublic;
    }

    public void setIsPublic(boolean isPublic) {
        this.isPublic = isPublic;
    }
}
