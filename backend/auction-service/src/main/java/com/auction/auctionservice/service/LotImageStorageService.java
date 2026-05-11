package com.auction.auctionservice.service;

import com.auction.auctionservice.dto.response.ImageUploadResponse;
import com.auction.auctionservice.exception.BadRequestException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class LotImageStorageService {
    private static final Map<String, String> CONTENT_TYPE_TO_EXTENSION = Map.of(
            "image/jpeg", "jpg",
            "image/png", "png",
            "image/webp", "webp",
            "image/gif", "gif"
    );

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp", "gif");

    private final Path uploadDir;
    private final long maxImageSizeBytes;

    public LotImageStorageService(
            @Value("${auction.upload-dir:uploads/lots}") String uploadDir,
            @Value("${auction.max-image-size-bytes:5242880}") long maxImageSizeBytes
    ) {
        this.uploadDir = Path.of(uploadDir).toAbsolutePath().normalize();
        this.maxImageSizeBytes = maxImageSizeBytes;
    }

    public ImageUploadResponse store(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Выберите изображение лота");
        }

        if (file.getSize() > maxImageSizeBytes) {
            throw new BadRequestException("Фото должно быть не больше " + maxImageSizeMegabytes() + " МБ");
        }

        String contentType = normalizeContentType(file.getContentType());
        String extension = resolveExtension(file.getOriginalFilename(), contentType);

        if (!isAllowed(contentType, extension)) {
            throw new BadRequestException("Допустимые форматы фото: JPG, PNG, WEBP, GIF");
        }

        String fileName = UUID.randomUUID() + "." + normalizeExtension(extension);
        Path target = uploadDir.resolve(fileName).normalize();

        if (!target.startsWith(uploadDir)) {
            throw new BadRequestException("Некорректное имя файла");
        }

        try {
            Files.createDirectories(uploadDir);
            file.transferTo(target);
        } catch (IOException ex) {
            throw new BadRequestException("Не удалось сохранить фото лота");
        }

        String publicUrl = "/api/auction/uploads/lots/" + fileName;
        return new ImageUploadResponse(publicUrl, fileName, file.getSize(), contentType);
    }

    private long maxImageSizeMegabytes() {
        return Math.max(1, (maxImageSizeBytes + 1024 * 1024 - 1) / 1024 / 1024);
    }

    private boolean isAllowed(String contentType, String extension) {
        String normalizedExtension = normalizeExtension(extension);
        return CONTENT_TYPE_TO_EXTENSION.containsKey(contentType) && ALLOWED_EXTENSIONS.contains(normalizedExtension);
    }

    private String resolveExtension(String originalFilename, String contentType) {
        String extension = extensionFromFileName(originalFilename);
        if (extension != null) {
            return extension;
        }
        return CONTENT_TYPE_TO_EXTENSION.getOrDefault(contentType, "jpg");
    }

    private String extensionFromFileName(String originalFilename) {
        if (originalFilename == null || originalFilename.isBlank()) {
            return null;
        }

        int dotIndex = originalFilename.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == originalFilename.length() - 1) {
            return null;
        }

        return originalFilename.substring(dotIndex + 1);
    }

    private String normalizeContentType(String contentType) {
        if (contentType == null || contentType.isBlank()) {
            return "";
        }
        return contentType.toLowerCase(Locale.ROOT).trim();
    }

    private String normalizeExtension(String extension) {
        String normalized = extension == null ? "" : extension.toLowerCase(Locale.ROOT).trim();
        return normalized.equals("jpeg") ? "jpg" : normalized;
    }
}
