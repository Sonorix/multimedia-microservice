package com.multimedia.ms.dao;

import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.gridfs.GridFSBucket;
import com.mongodb.client.gridfs.model.GridFSFile;
import com.mongodb.client.gridfs.model.GridFSUploadOptions;
import com.mongodb.client.model.Filters;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.InsertOneResult;
import com.multimedia.ms.model.Database;
import com.multimedia.ms.model.MultimediaDto;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.bson.BsonValue;
import org.bson.Document;
import org.bson.types.ObjectId;

/**
 * Data access object for multimedia files
 */
public class MultimediaDao {
    private final Database database;
    private final MongoCollection<Document> collection;
    private final GridFSBucket gridFSBucket;
    
    public MultimediaDao() {
        this.database = new Database();
        this.collection = database.getDatabase().getCollection("fs.files");
        this.gridFSBucket = database.getGridFSBucket();
    }
    
    /**
     * Upload a new multimedia file
     * 
     * @param multimedia The multimedia metadata
     * @param inputStream The file content input stream
     * @return The created multimedia record with ID
     * @throws RuntimeException if an error occurs
     */
    public MultimediaDto uploadFile(MultimediaDto multimedia, InputStream inputStream) {
        try {
            // Upload file to GridFS
            Document metadata = new Document()
                    .append("musicianId", multimedia.getMusicianId())
                    .append("title", multimedia.getTitle())
                    .append("description", multimedia.getDescription())
                    .append("uploadDate", new Date())
                    .append("isPublic", multimedia.isIsPublic());
            
            GridFSUploadOptions options = new GridFSUploadOptions()
                    .metadata(metadata)
                    .chunkSizeBytes(1024 * 1024); // 1MB chunk size
            
            ObjectId fileId = gridFSBucket.uploadFromStream(
                    multimedia.getFilename(),
                    inputStream,
                    options);
            
            // Store metadata in multimedia collection
            multimedia.setFileId(fileId.toString());
            Document doc = multimedia.toDocument();
            
            InsertOneResult result = collection.insertOne(doc);
            if (result.getInsertedId() != null) {
                multimedia.setId(result.getInsertedId().asObjectId().getValue().toString());
            }
            
            return multimedia;
        } catch (Exception e) {
            throw new RuntimeException("Error uploading file: " + e.getMessage(), e);
        }
    }
    
    /**
     * Get metadata for a file by ID
     * 
     * @param id The multimedia metadata ID
     * @return The multimedia metadata or null if not found
     */
    public MultimediaDto getFileMetadata(String id) {
        try {
            Document doc = collection.find(Filters.eq("_id", new ObjectId(id))).first();
            return MultimediaDto.fromDocument(doc);
        } catch (Exception e) {
            throw new RuntimeException("Error retrieving file metadata: " + e.getMessage(), e);
        }
    }
    
    /**
     * Get all multimedia files for a musician
     * 
     * @param musicianId The musician ID
     * @return List of multimedia metadata for the musician
     */
    public List<MultimediaDto> getFilesByMusicianId(String musicianId) {
        List<MultimediaDto> files = new ArrayList<>();
        try {
            FindIterable<Document> docs = collection.find(Filters.eq("musicianId", musicianId));
            MongoCursor<Document> cursor = docs.iterator();
            
            while (cursor.hasNext()) {
                Document doc = cursor.next();
                MultimediaDto file = MultimediaDto.fromDocument(doc);
                if (file != null) {
                    files.add(file);
                }
            }
            cursor.close();
        } catch (Exception e) {
            throw new RuntimeException("Error retrieving multimedia files: " + e.getMessage(), e);
        }
        return files;
    }
    
    /**
     * Get all public multimedia files for a musician
     * 
     * @param musicianId The musician ID
     * @return List of public multimedia metadata for the musician
     */
    public List<MultimediaDto> getPublicFilesByMusicianId(String musicianId) {
        List<MultimediaDto> files = new ArrayList<>();
        try {
            FindIterable<Document> docs = collection.find(
                Filters.and(
                    Filters.eq("musicianId", musicianId),
                    Filters.eq("isPublic", true)
                )
            );
            
            MongoCursor<Document> cursor = docs.iterator();
            while (cursor.hasNext()) {
                Document doc = cursor.next();
                MultimediaDto file = MultimediaDto.fromDocument(doc);
                if (file != null) {
                    files.add(file);
                }
            }
            cursor.close();
        } catch (Exception e) {
            throw new RuntimeException("Error retrieving public multimedia files: " + e.getMessage(), e);
        }
        return files;
    }
    
    /**
     * Download a file's content
     * 
     * @param fileId The GridFS file ID
     * @return The file content as bytes
     * @throws RuntimeException if an error occurs
     */
    public byte[] downloadFile(String fileId) {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            gridFSBucket.downloadToStream(new ObjectId(fileId), outputStream);
            return outputStream.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Error downloading file: " + e.getMessage(), e);
        }
    }
    
    /**
     * Delete a multimedia file and its metadata
     * 
     * @param id The multimedia metadata ID
     * @return true if deleted, false if not found
     * @throws RuntimeException if an error occurs
     */
    public boolean deleteFile(String id) {
        try {
            // First get the metadata to get the GridFS fileId
            MultimediaDto multimedia = getFileMetadata(id);
            if (multimedia == null) {
                return false;
            }
            
            // Delete the GridFS file
            gridFSBucket.delete(new ObjectId(multimedia.getFileId()));
            
            // Delete the metadata
            DeleteResult result = collection.deleteOne(Filters.eq("_id", new ObjectId(id)));
            return result.getDeletedCount() > 0;
        } catch (Exception e) {
            throw new RuntimeException("Error deleting file: " + e.getMessage(), e);
        }
    }
    
    /**
     * Check if a file exists by its GridFS ID
     * 
     * @param fileId The GridFS file ID
     * @return true if the file exists, false otherwise
     */
    public boolean fileExists(String fileId) {
        try {
            GridFSFile file = gridFSBucket.find(Filters.eq("_id", new ObjectId(fileId))).first();
            return file != null;
        } catch (Exception e) {
            throw new RuntimeException("Error checking file existence: " + e.getMessage(), e);
        }
    }
    
    /**
     * Update multimedia file metadata
     * 
     * @param multimedia The multimedia metadata to update
     * @return The updated metadata
     * @throws RuntimeException if an error occurs
     */
    public MultimediaDto updateMetadata(MultimediaDto multimedia) {
        try {
            Document update = new Document()
                    .append("title", multimedia.getTitle())
                    .append("description", multimedia.getDescription())
                    .append("isPublic", multimedia.isIsPublic());
            
            collection.updateOne(
                    Filters.eq("_id", new ObjectId(multimedia.getId())),
                    new Document("$set", update)
            );
            
            // Also update GridFS metadata
            Document gridFsUpdate = new Document()
                    .append("metadata.title", multimedia.getTitle())
                    .append("metadata.description", multimedia.getDescription())
                    .append("metadata.isPublic", multimedia.isIsPublic());
            
            database.getDatabase().getCollection("fs.files").updateOne(
                    Filters.eq("_id", new ObjectId(multimedia.getFileId())),
                    new Document("$set", gridFsUpdate)
            );
            
            return getFileMetadata(multimedia.getId());
        } catch (Exception e) {
            throw new RuntimeException("Error updating file metadata: " + e.getMessage(), e);
        }
    }
    
    /**
     * Close the database connection
     */
    public void close() {
        if (database != null) {
            database.close();
        }
    }
}
