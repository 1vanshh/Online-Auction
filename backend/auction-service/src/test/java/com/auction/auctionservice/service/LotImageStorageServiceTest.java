package com.auction.auctionservice.service;

import com.auction.auctionservice.dto.response.ImageUploadResponse;
import com.auction.auctionservice.exception.BadRequestException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class LotImageStorageServiceTest {
    @TempDir
    Path tempDir;

    @Test
    void shouldStoreAllowedImageAndReturnPublicUrl() throws Exception {
        LotImageStorageService service = new LotImageStorageService(tempDir.toString(), 1024 * 1024);
        MockMultipartFile file = new MockMultipartFile("image", "photo.png", "image/png", new byte[]{1, 2, 3});

        ImageUploadResponse response = service.store(file);

        assertTrue(response.imageUrl().startsWith("/api/auction/uploads/lots/"));
        assertTrue(response.fileName().endsWith(".png"));
        assertEquals(3, response.sizeBytes());
        assertEquals("image/png", response.contentType());
        assertTrue(Files.exists(tempDir.resolve(response.fileName())));
    }

    @Test
    void shouldRejectUnsupportedImageFormat() {
        LotImageStorageService service = new LotImageStorageService(tempDir.toString(), 1024 * 1024);
        MockMultipartFile file = new MockMultipartFile("image", "document.pdf", "application/pdf", new byte[]{1});

        BadRequestException ex = assertThrows(BadRequestException.class, () -> service.store(file));

        assertEquals("Допустимые форматы фото: JPG, PNG, WEBP, GIF", ex.getMessage());
    }

    @Test
    void shouldRejectTooLargeImage() {
        LotImageStorageService service = new LotImageStorageService(tempDir.toString(), 2);
        MockMultipartFile file = new MockMultipartFile("image", "photo.jpg", "image/jpeg", new byte[]{1, 2, 3});

        BadRequestException ex = assertThrows(BadRequestException.class, () -> service.store(file));

        assertEquals("Фото должно быть не больше 1 МБ", ex.getMessage());
    }
}
