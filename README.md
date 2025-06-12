# Multimedia Microservice

A Java/JakartaEE microservice for managing musician profiles, ratings, and multimedia content with MongoDB integration.

## Overview

This microservice provides APIs for:
- Managing musician profiles with biographical information, genres, and instruments
- Handling ratings and reviews for musicians
- Uploading, storing, and retrieving multimedia files (audio, video, images)

## Technology Stack

- Java 17
- Jakarta EE 11
- MongoDB (with GridFS for file storage)
- Maven

## Requirements

- JDK 17 or higher
- MongoDB server
- Maven 3.6+

## Configuration

Before running the application, create a `.env` file in the project root based on the provided `.env.template`. This file should contain:

```
MONGODB_CONNECTION_STRING=mongodb://localhost:27017
MONGODB_DATABASE=multimedia_db
MAX_FILE_SIZE=10485760
ALLOWED_FILE_TYPES=mp3,mp4,jpg,jpeg,png
UPLOAD_TEMP_DIR=/path/to/temp/dir
```

Adjust the values as needed for your environment.

## Build and Deployment

To build the project:

```bash
mvn clean package
```

This creates a WAR file in the `target` directory that can be deployed to any Jakarta EE compatible server (e.g., Payara, Glassfish, TomEE).

## API Documentation

### Base URL

All API endpoints are accessible under:

```
/Multimedia-microservice/resources/
```

### Musician Profile API

#### Get All Profiles

```
GET /profiles
```

Returns a list of all musician profiles.

#### Get Profile by ID

```
GET /profiles/{id}
```

Returns a single musician profile with the specified ID.

#### Get Profile by User ID

```
GET /profiles/user/{userId}
```

Returns the musician profile associated with the specified user ID.

#### Create Profile

```
POST /profiles
```

Create a new musician profile.

**Request Body**:
```json
{
  "userId": "user123",
  "name": "John Smith",
  "biography": "Jazz guitarist with 10 years of experience",
  "genres": ["Jazz", "Blues"],
  "instruments": ["Guitar", "Piano"]
}
```

#### Update Profile

```
PUT /profiles/{id}
```

Update an existing musician profile.

**Request Body**:
```json
{
  "name": "John A. Smith",
  "biography": "Updated biography",
  "genres": ["Jazz", "Blues", "Fusion"],
  "instruments": ["Guitar", "Piano", "Bass"]
}
```

#### Delete Profile

```
DELETE /profiles/{id}
```

Delete a musician profile.

### Ratings API

#### Get All Ratings for a Musician

```
GET /ratings/musician/{musicianId}
```

Returns all ratings for the specified musician.

#### Get Rating by ID

```
GET /ratings/{id}
```

Returns a single rating with the specified ID.

#### Get Rating by User and Musician

```
GET /ratings/musician/{musicianId}/user/{userId}
```

Returns the rating submitted by a specific user for a specific musician.

#### Add Rating

```
POST /ratings
```

Add a new rating for a musician.

**Request Body**:
```json
{
  "musicianId": "musician123",
  "userId": "user456",
  "rating": 4.5,
  "comment": "Great performance, very talented!"
}
```

#### Delete Rating

```
DELETE /ratings/{id}
```

Delete a rating.

### Multimedia API

#### Get All Files for a Musician

```
GET /multimedia/musician/{musicianId}?publicOnly=true|false
```

Returns all multimedia files for the specified musician. Use the `publicOnly` query parameter to filter for public files only.

#### Get File Metadata

```
GET /multimedia/{id}
```

Returns metadata for a specific file.

#### Download File

```
GET /multimedia/{id}/download
```

Downloads the file with the specified ID.

#### Upload File

```
POST /multimedia/upload
```

Upload a new file. Must be sent as `multipart/form-data`.

**Form Fields**:
- `file`: The file to upload
- `musicianId`: ID of the musician who owns this file
- `title`: Title of the file
- `description`: Optional description
- `isPublic`: Whether the file should be publicly accessible (`true` or `false`)

#### Update File Metadata

```
PUT /multimedia/{id}
```

Update metadata for an existing file.

**Request Body**:
```json
{
  "title": "Updated title",
  "description": "Updated description",
  "isPublic": true
}
```

#### Delete File

```
DELETE /multimedia/{id}
```

Delete a file.

## Error Handling

All API endpoints return standardized error responses in the following format:

```json
{
  "status": 400,
  "message": "Error message description",
  "path": "/profiles/123",
  "timestamp": "2025-06-11T21:51:24.123"
}
```

Common HTTP status codes:
- 200: Success
- 201: Created successfully
- 400: Bad request (invalid input)
- 404: Resource not found
- 409: Conflict
- 500: Server error

## Security Considerations

This microservice does not handle authentication directly. It's designed to work within a larger microservices architecture where authentication is handled by a separate service.

For production deployment, consider:
- Implementing API gateway authentication
- Setting up CORS configuration
- Using HTTPS
- Implementing rate limiting

## Integration with Users-microservice

This microservice is designed to complement the Users-microservice project. User IDs referenced in this service should correspond to valid users in the Users-microservice.
