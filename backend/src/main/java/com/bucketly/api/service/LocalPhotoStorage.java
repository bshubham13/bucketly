package com.bucketly.api.service;

import com.bucketly.api.exception.InvalidPhotoException;
import com.bucketly.api.exception.PhotoStorageException;
import com.bucketly.api.exception.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Iterator;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
public class LocalPhotoStorage implements PhotoStorage {

    private static final Pattern SAFE_KEY_PATTERN = Pattern.compile("^[a-zA-Z0-9_-]+\\.(jpg|jpeg|png)$", Pattern.CASE_INSENSITIVE);

    private final Path rootLocation;

    @Autowired
    public LocalPhotoStorage(@Value("${bucketly.photo.upload-dir:uploads/memories}") String uploadDir) {
        this(Paths.get(uploadDir));
    }

    public LocalPhotoStorage(Path rootLocation) {
        this.rootLocation = rootLocation.toAbsolutePath().normalize();
        init();
    }

    private void init() {
        try {
            Files.createDirectories(this.rootLocation);
        } catch (IOException e) {
            throw new PhotoStorageException("Could not initialize photo storage directory: " + this.rootLocation, e);
        }
    }

    public Path getRootLocation() {
        return this.rootLocation;
    }

    @Override
    public String store(InputStream inputStream, long contentLength, String originalFilename) {
        if (inputStream == null) {
            throw new InvalidPhotoException("Photo input stream cannot be null");
        }

        // Bounded read enforcing 5 MB maximum
        byte[] bytes = readBoundedStream(inputStream, contentLength);

        // Validate actual image content and determine extension strictly from content
        String formatExtension = validateAndDetectFormat(bytes);

        // Generate opaque server-side UUID storage key (never use originalFilename for path)
        String storageKey = UUID.randomUUID().toString() + "." + formatExtension;
        Path targetPath = resolvePath(storageKey);

        try {
            writeBytes(targetPath, bytes);
            return storageKey;
        } catch (IOException e) {
            if (!(e instanceof FileAlreadyExistsException)) {
                try {
                    Files.deleteIfExists(targetPath);
                } catch (IOException cleanupException) {
                    e.addSuppressed(cleanupException);
                }
            }
            throw new PhotoStorageException("Failed to store photo file: " + storageKey, e);
        }
    }

    void writeBytes(Path targetPath, byte[] bytes) throws IOException {
        Files.write(targetPath, bytes, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
    }

    @Override
    public Resource loadAsResource(String storageKey) {
        Path path = resolvePath(storageKey);
        if (!Files.exists(path) || !Files.isRegularFile(path) || !Files.isReadable(path)) {
            throw new ResourceNotFoundException("Photo not found: " + storageKey);
        }
        try {
            return new UrlResource(path.toUri());
        } catch (MalformedURLException e) {
            throw new InvalidPhotoException("Malformed photo resource URL for key: " + storageKey, e);
        }
    }

    @Override
    public void delete(String storageKey) {
        if (storageKey == null || storageKey.isBlank()) {
            return;
        }
        Path path = resolvePath(storageKey);
        try {
            Files.deleteIfExists(path);
        } catch (IOException e) {
            throw new PhotoStorageException("Failed to delete photo file: " + storageKey, e);
        }
    }

    @Override
    public String getContentType(String storageKey) {
        Path path = resolvePath(storageKey);
        String lower = storageKey.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) {
            return "image/jpeg";
        }
        if (lower.endsWith(".png")) {
            return "image/png";
        }
        try {
            String probed = Files.probeContentType(path);
            return probed != null ? probed : "application/octet-stream";
        } catch (IOException e) {
            return "application/octet-stream";
        }
    }

    private Path resolvePath(String storageKey) {
        if (storageKey == null || storageKey.isBlank()) {
            throw new InvalidPhotoException("Storage key cannot be blank");
        }

        // Immediate check against path traversal characters
        if (storageKey.contains("..") || storageKey.contains("/") || storageKey.contains("\\")
                || storageKey.contains(":") || storageKey.contains("\0")) {
            throw new InvalidPhotoException("Path traversal attempt or invalid key: " + storageKey);
        }

        if (!SAFE_KEY_PATTERN.matcher(storageKey).matches()) {
            throw new InvalidPhotoException("Invalid photo storage key format: " + storageKey);
        }

        Path resolved = this.rootLocation.resolve(storageKey).normalize();
        if (!resolved.startsWith(this.rootLocation) || resolved.equals(this.rootLocation)) {
            throw new InvalidPhotoException("Resolved path escaped storage root: " + storageKey);
        }

        return resolved;
    }

    private byte[] readBoundedStream(InputStream inputStream, long declaredSize) {
        if (declaredSize > MAX_FILE_SIZE) {
            throw new InvalidPhotoException("Photo file size exceeds maximum limit of 5 MB");
        }

        try {
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            byte[] chunk = new byte[8192];
            long totalRead = 0;
            int bytesRead;

            while ((bytesRead = inputStream.read(chunk)) != -1) {
                totalRead += bytesRead;
                if (totalRead > MAX_FILE_SIZE) {
                    throw new InvalidPhotoException("Photo file size exceeds maximum limit of 5 MB");
                }
                buffer.write(chunk, 0, bytesRead);
            }

            if (totalRead == 0) {
                throw new InvalidPhotoException("Photo file cannot be empty");
            }

            return buffer.toByteArray();
        } catch (InvalidPhotoException e) {
            throw e;
        } catch (IOException e) {
            throw new PhotoStorageException("Failed to read photo input stream", e);
        }
    }

    private String validateAndDetectFormat(byte[] bytes) {
        if (bytes == null || bytes.length < 8) {
            throw new InvalidPhotoException("Invalid or incomplete image content");
        }

        // Magic byte verification
        boolean isJpeg = (bytes[0] == (byte) 0xFF && bytes[1] == (byte) 0xD8 && bytes[2] == (byte) 0xFF);
        boolean isPng = (bytes[0] == (byte) 0x89 && bytes[1] == (byte) 0x50 && bytes[2] == (byte) 0x4E && bytes[3] == (byte) 0x47
                && bytes[4] == (byte) 0x0D && bytes[5] == (byte) 0x0A && bytes[6] == (byte) 0x1A && bytes[7] == (byte) 0x0A);

        if (!isJpeg && !isPng) {
            throw new InvalidPhotoException("Unsupported image format. Only JPEG and PNG images are supported.");
        }

        // Full raster decoding verification using ImageIO to verify that the byte content
        // is a valid, decodable JPEG or PNG raster image with non-zero dimensions.
        // Note: This verifies image decodeability and format integrity, but does not re-encode
        // or strip trailing or embedded non-image payloads (e.g. metadata or polyglots).
        try (ImageInputStream iis = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
            if (iis == null) {
                throw new InvalidPhotoException("Unable to read image data stream");
            }

            Iterator<ImageReader> readers = ImageIO.getImageReaders(iis);
            if (!readers.hasNext()) {
                throw new InvalidPhotoException("Unsupported or corrupt image format");
            }

            ImageReader reader = readers.next();
            try {
                reader.setInput(iis, true, true);
                String formatName = reader.getFormatName();
                if (formatName == null) {
                    throw new InvalidPhotoException("Unable to determine image format");
                }

                String upperFormat = formatName.toUpperCase(Locale.ROOT);
                if (!"JPEG".equals(upperFormat) && !"JPG".equals(upperFormat) && !"PNG".equals(upperFormat)) {
                    throw new InvalidPhotoException("Unsupported image format: " + formatName);
                }

                int width = reader.getWidth(0);
                int height = reader.getHeight(0);
                if (width <= 0 || height <= 0) {
                    throw new InvalidPhotoException("Invalid image dimensions: " + width + "x" + height);
                }

                // Verify the image can actually be raster-decoded
                BufferedImage decodedImage = reader.read(0);
                if (decodedImage == null) {
                    throw new InvalidPhotoException("Corrupted image content could not be decoded");
                }

                return "PNG".equals(upperFormat) ? "png" : "jpg";
            } finally {
                reader.dispose();
            }
        } catch (InvalidPhotoException e) {
            throw e;
        } catch (Exception e) {
            throw new InvalidPhotoException("Malformed or unreadable image content: " + e.getMessage(), e);
        }
    }
}
