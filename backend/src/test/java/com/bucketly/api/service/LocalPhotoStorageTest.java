package com.bucketly.api.service;

import com.bucketly.api.exception.InvalidPhotoException;
import com.bucketly.api.exception.PhotoStorageException;
import com.bucketly.api.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.Resource;
import org.springframework.mock.web.MockMultipartFile;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LocalPhotoStorageTest {

    @TempDir
    Path tempDir;

    private LocalPhotoStorage photoStorage;

    @BeforeEach
    void setUp() {
        photoStorage = new LocalPhotoStorage(tempDir);
    }

    private byte[] createTestImage(String format, int width, int height) throws IOException {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setColor(Color.CYAN);
        g.fillRect(0, 0, width, height);
        g.dispose();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, format, baos);
        return baos.toByteArray();
    }

    @Test
    @DisplayName("Successfully stores valid JPEG and returns UUID key ending in .jpg")
    void store_ValidJpeg_Success() throws IOException {
        byte[] jpegBytes = createTestImage("jpg", 100, 100);

        String key = photoStorage.store(new ByteArrayInputStream(jpegBytes), jpegBytes.length, "my-vacation.jpeg");

        assertThat(key).endsWith(".jpg");
        assertThat(key).doesNotContain("my-vacation");

        // Verify key is a valid UUID + .jpg
        String uuidPart = key.substring(0, key.lastIndexOf('.'));
        assertThat(UUID.fromString(uuidPart)).isNotNull();

        // Verify file exists on disk in tempDir
        Path storedFile = tempDir.resolve(key);
        assertThat(Files.exists(storedFile)).isTrue();
        assertThat(Files.size(storedFile)).isEqualTo(jpegBytes.length);
    }

    @Test
    @DisplayName("Successfully stores valid PNG and returns UUID key ending in .png")
    void store_ValidPng_Success() throws IOException {
        byte[] pngBytes = createTestImage("png", 80, 80);

        String key = photoStorage.store(new ByteArrayInputStream(pngBytes), pngBytes.length, "avatar.png");

        assertThat(key).endsWith(".png");
        assertThat(key).doesNotContain("avatar");

        Path storedFile = tempDir.resolve(key);
        assertThat(Files.exists(storedFile)).isTrue();
        assertThat(Files.size(storedFile)).isEqualTo(pngBytes.length);
    }

    @Test
    @DisplayName("Never uses client originalFilename as filesystem path")
    void store_DoesNotUseOriginalFilenameAsStoragePath() throws IOException {
        byte[] jpegBytes = createTestImage("jpg", 50, 50);
        String untrustedFilename = "../../malicious/path/hack.exe";

        String key = photoStorage.store(new ByteArrayInputStream(jpegBytes), jpegBytes.length, untrustedFilename);

        assertThat(key).doesNotContain("malicious");
        assertThat(key).doesNotContain("hack");
        assertThat(key).doesNotContain("..");
        assertThat(key).doesNotContain("/");
        assertThat(key).doesNotContain("\\");
        assertThat(key).endsWith(".jpg");

        Path storedFile = tempDir.resolve(key);
        assertThat(Files.exists(storedFile)).isTrue();
        assertThat(storedFile.startsWith(tempDir)).isTrue();
    }

    @Test
    @DisplayName("Rejects null or empty input stream")
    void store_RejectsEmptyOrNullInputStream() {
        assertThatThrownBy(() -> photoStorage.store(null, 100, "test.jpg"))
                .isInstanceOf(InvalidPhotoException.class)
                .hasMessageContaining("cannot be null");

        assertThatThrownBy(() -> photoStorage.store(new ByteArrayInputStream(new byte[0]), 0, "test.jpg"))
                .isInstanceOf(InvalidPhotoException.class)
                .hasMessageContaining("empty");
    }

    @Test
    @DisplayName("Rejects upload when declared size exceeds 5 MB")
    void store_RejectsDeclaredSizeExceedingLimit() {
        long oversized = (5 * 1024 * 1024) + 1;
        ByteArrayInputStream is = new ByteArrayInputStream(new byte[10]);

        assertThatThrownBy(() -> photoStorage.store(is, oversized, "huge.jpg"))
                .isInstanceOf(InvalidPhotoException.class)
                .hasMessageContaining("exceeds maximum limit of 5 MB");
    }

    @Test
    @DisplayName("Rejects upload when actual stream exceeds 5 MB regardless of declared size")
    void store_RejectsActualStreamExceedingLimit() {
        byte[] chunk = new byte[1024];
        // Stream that produces slightly over 5 MB
        InputStream streaming5MbPlus = new InputStream() {
            private long count = 0;
            private final long limit = (5 * 1024 * 1024) + 2048;

            @Override
            public int read() {
                if (count >= limit) return -1;
                count++;
                return 0;
            }

            @Override
            public int read(byte[] b, int off, int len) {
                if (count >= limit) return -1;
                int toRead = (int) Math.min(len, limit - count);
                count += toRead;
                return toRead;
            }
        };

        assertThatThrownBy(() -> photoStorage.store(streaming5MbPlus, -1, "stream.jpg"))
                .isInstanceOf(InvalidPhotoException.class)
                .hasMessageContaining("exceeds maximum limit of 5 MB");
    }

    @Test
    @DisplayName("Rejects arbitrary non-image text bytes even with .jpg extension")
    void store_RejectsNonImageArbitraryContent() {
        byte[] textBytes = "This is a plain text file, not an image!".getBytes(StandardCharsets.UTF_8);

        assertThatThrownBy(() -> photoStorage.store(new ByteArrayInputStream(textBytes), textBytes.length, "fake.jpg"))
                .isInstanceOf(InvalidPhotoException.class)
                .hasMessageContaining("Unsupported image format");
    }

    @Test
    @DisplayName("Rejects unsupported image format such as GIF")
    void store_RejectsUnsupportedFormat_Gif() throws IOException {
        byte[] gifBytes = createTestImage("gif", 50, 50);

        assertThatThrownBy(() -> photoStorage.store(new ByteArrayInputStream(gifBytes), gifBytes.length, "animated.gif"))
                .isInstanceOf(InvalidPhotoException.class)
                .hasMessageContaining("Only JPEG and PNG");
    }

    @Test
    @DisplayName("Rejects unsupported image format such as BMP")
    void store_RejectsUnsupportedFormat_Bmp() throws IOException {
        byte[] bmpBytes = createTestImage("bmp", 50, 50);

        assertThatThrownBy(() -> photoStorage.store(new ByteArrayInputStream(bmpBytes), bmpBytes.length, "graphic.bmp"))
                .isInstanceOf(InvalidPhotoException.class)
                .hasMessageContaining("Only JPEG and PNG");
    }

    @Test
    @DisplayName("Rejects file with PNG magic bytes but corrupted/malformed body")
    void store_RejectsCorruptedImageWithValidMagicBytes() {
        byte[] fakePng = new byte[] {
                (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, // PNG header
                0x00, 0x00, 0x00, 0x0D, 0x49, 0x48, 0x44, 0x52, // corrupt IHDR chunk
                0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00 // truncated/invalid
        };

        assertThatThrownBy(() -> photoStorage.store(new ByteArrayInputStream(fakePng), fakePng.length, "broken.png"))
                .isInstanceOf(InvalidPhotoException.class);
    }

    @Test
    @DisplayName("Loads existing stored photo as Resource")
    void loadAsResource_Success() throws IOException {
        byte[] jpegBytes = createTestImage("jpg", 60, 60);
        String key = photoStorage.store(new ByteArrayInputStream(jpegBytes), jpegBytes.length, "photo.jpg");

        Resource resource = photoStorage.loadAsResource(key);

        assertThat(resource).isNotNull();
        assertThat(resource.exists()).isTrue();
        assertThat(resource.isReadable()).isTrue();
        assertThat(resource.contentLength()).isEqualTo(jpegBytes.length);
    }

    @Test
    @DisplayName("Throws ResourceNotFoundException when photo file does not exist")
    void loadAsResource_NotFound() {
        String nonExistentKey = UUID.randomUUID().toString() + ".jpg";

        assertThatThrownBy(() -> photoStorage.loadAsResource(nonExistentKey))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Photo not found");
    }

    @Test
    @DisplayName("Deletes existing photo file and subsequent load throws ResourceNotFoundException")
    void delete_DeletesExistingPhoto() throws IOException {
        byte[] pngBytes = createTestImage("png", 40, 40);
        String key = photoStorage.store(new ByteArrayInputStream(pngBytes), pngBytes.length, "test.png");

        assertThat(Files.exists(tempDir.resolve(key))).isTrue();

        photoStorage.delete(key);

        assertThat(Files.exists(tempDir.resolve(key))).isFalse();
        assertThatThrownBy(() -> photoStorage.loadAsResource(key))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Delete is idempotent and gracefully handles non-existent or null key")
    void delete_IdempotentOnNonExistentKey() {
        photoStorage.delete(null);
        photoStorage.delete("");
        photoStorage.delete("   ");
        photoStorage.delete(UUID.randomUUID().toString() + ".jpg");
    }

    @Test
    @DisplayName("Returns correct MIME Content-Type based on storage key")
    void getContentType_ResolvesCorrectMime() {
        assertThat(photoStorage.getContentType("photo.jpg")).isEqualTo("image/jpeg");
        assertThat(photoStorage.getContentType("photo.jpeg")).isEqualTo("image/jpeg");
        assertThat(photoStorage.getContentType("photo.png")).isEqualTo("image/png");
        assertThat(photoStorage.getContentType("PHOTO.PNG")).isEqualTo("image/png");
    }

    @Test
    @DisplayName("Protects against path traversal in resolvePath for load, delete, and getContentType")
    void pathTraversalProtection() {
        String[] traversalKeys = {
                "../secret.jpg",
                "..\\secret.jpg",
                "../../etc/passwd",
                "sub/photo.jpg",
                "/root/photo.jpg",
                "C:\\Windows\\System32\\cmd.exe",
                "photo.exe",
                "photo.php",
                "valid-name.jpg/../secret.jpg",
                "photo.jpg\0.png"
        };

        for (String maliciousKey : traversalKeys) {
            assertThatThrownBy(() -> photoStorage.loadAsResource(maliciousKey))
                    .isInstanceOf(InvalidPhotoException.class);

            assertThatThrownBy(() -> photoStorage.delete(maliciousKey))
                    .isInstanceOf(InvalidPhotoException.class);

            assertThatThrownBy(() -> photoStorage.getContentType(maliciousKey))
                    .isInstanceOf(InvalidPhotoException.class);
        }
    }

    @Test
    @DisplayName("Stores photo via Spring MultipartFile convenience method")
    void store_MultipartFile_Success() throws IOException {
        byte[] pngBytes = createTestImage("png", 50, 50);
        MockMultipartFile file = new MockMultipartFile("photo", "original.png", "image/png", pngBytes);

        String key = photoStorage.store(file);

        assertThat(key).endsWith(".png");
        assertThat(Files.exists(tempDir.resolve(key))).isTrue();
    }

    @Test
    @DisplayName("Rejects empty Spring MultipartFile")
    void store_EmptyMultipartFile_ThrowsInvalidPhotoException() {
        MockMultipartFile emptyFile = new MockMultipartFile("photo", "empty.png", "image/png", new byte[0]);

        assertThatThrownBy(() -> photoStorage.store(emptyFile))
                .isInstanceOf(InvalidPhotoException.class)
                .hasMessageContaining("empty");
    }

    @Test
    @DisplayName("Cleans up partially created target file when disk write fails")
    void store_CleansUpPartialFileWhenWriteFails() throws IOException {
        byte[] jpegBytes = createTestImage("jpg", 30, 30);
        Path[] createdPartialPath = new Path[1];

        LocalPhotoStorage storageWithFailingWrite = new LocalPhotoStorage(tempDir) {
            @Override
            void writeBytes(Path targetPath, byte[] bytes) throws IOException {
                createdPartialPath[0] = targetPath;
                // Simulate partial bytes written before failure
                Files.write(targetPath, new byte[] {1, 2, 3});
                throw new IOException("Simulated disk error during write");
            }
        };

        assertThatThrownBy(() -> storageWithFailingWrite.store(new ByteArrayInputStream(jpegBytes), jpegBytes.length, "test.jpg"))
                .isInstanceOf(PhotoStorageException.class)
                .hasCauseInstanceOf(IOException.class)
                .hasMessageContaining("Failed to store photo file");

        // Verify partial file was cleaned up and does not linger on disk
        assertThat(createdPartialPath[0]).isNotNull();
        assertThat(Files.exists(createdPartialPath[0])).isFalse();
    }

    @Test
    @DisplayName("Does not delete pre-existing file when FileAlreadyExistsException occurs")
    void store_DoesNotDeletePreExistingFileWhenFileAlreadyExists() throws IOException {
        byte[] jpegBytes = createTestImage("jpg", 30, 30);
        Path preExistingFile = tempDir.resolve("pre-existing.jpg");
        Files.write(preExistingFile, new byte[] {42, 42, 42});

        LocalPhotoStorage storageWithCollision = new LocalPhotoStorage(tempDir) {
            @Override
            void writeBytes(Path targetPath, byte[] bytes) throws IOException {
                throw new FileAlreadyExistsException(preExistingFile.toString());
            }
        };

        assertThatThrownBy(() -> storageWithCollision.store(new ByteArrayInputStream(jpegBytes), jpegBytes.length, "test.jpg"))
                .isInstanceOf(PhotoStorageException.class)
                .hasCauseInstanceOf(FileAlreadyExistsException.class);

        // Verify pre-existing file was NOT deleted
        assertThat(Files.exists(preExistingFile)).isTrue();
        assertThat(Files.readAllBytes(preExistingFile)).containsExactly(42, 42, 42);
    }
}

