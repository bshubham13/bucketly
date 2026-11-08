package com.bucketly.api.service;

import com.bucketly.api.exception.InvalidPhotoException;
import com.bucketly.api.exception.PhotoStorageException;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;

public interface PhotoStorage {

    /**
     * Maximum allowed image size in bytes (5 MB).
     */
    long MAX_FILE_SIZE = 5 * 1024 * 1024;

    /**
     * Stores an image stream, validating its actual JPEG/PNG format and 5MB size limit.
     * Generates an opaque server-side UUID filename. Never uses original client filename in storage path.
     *
     * @param inputStream data stream containing the image bytes
     * @param contentLength declared size of the stream in bytes (or negative if unknown)
     * @param originalFilename optional client-provided filename used only as metadata
     * @return opaque storage key (e.g., "550e8400-e29b-41d4-a716-446655440000.jpg")
     */
    String store(InputStream inputStream, long contentLength, String originalFilename);

    /**
     * Stores a Spring MultipartFile.
     *
     * @param file uploaded multipart file
     * @return opaque storage key
     */
    default String store(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new InvalidPhotoException("Photo file cannot be empty");
        }
        try {
            return store(file.getInputStream(), file.getSize(), file.getOriginalFilename());
        } catch (IOException e) {
            throw new PhotoStorageException("Failed to read photo upload stream", e);
        }
    }

    /**
     * Loads the photo as a Spring Resource given its opaque storage key.
     *
     * @param storageKey the opaque storage key
     * @return readable Spring Resource pointing to the photo
     */
    Resource loadAsResource(String storageKey);

    /**
     * Deletes the photo with the given storage key if it exists.
     *
     * @param storageKey the opaque storage key
     */
    void delete(String storageKey);

    /**
     * Resolves the MIME content type (e.g. "image/jpeg", "image/png") for the given storage key.
     *
     * @param storageKey the opaque storage key
     * @return MIME content type string
     */
    String getContentType(String storageKey);
}

