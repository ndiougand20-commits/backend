package com.rezo.backend.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class UserMediaStorageService {

    private static final Set<String> ALLOWED_IMAGE_MIME_TYPES = Set.of(
            "image/jpeg", "image/png", "image/webp", "image/gif"
    );
    private static final long MAX_IMAGE_BYTES = 8L * 1024L * 1024L;
    private static final long MAX_PDF_BYTES = 12L * 1024L * 1024L;

    private final Path uploadRoot;

    public UserMediaStorageService(@Value("${rezo.media.upload-dir:uploads}") String uploadDir) {
        this.uploadRoot = Paths.get(uploadDir).toAbsolutePath().normalize();
    }

    public StoredMedia storePhoto(UUID userId, MultipartFile file) {
        validateNotEmpty(file);
        validateSize(file, MAX_IMAGE_BYTES, "L'image depasse la taille maximale autorisee (8MB)");

        String contentType = normalizedContentType(file);
        if (!ALLOWED_IMAGE_MIME_TYPES.contains(contentType)) {
            throw new MediaValidationException("Format image non supporte (jpeg, png, webp, gif)");
        }

        String extension = extensionFromFilename(file.getOriginalFilename());
        if (extension == null || extension.isBlank()) {
            extension = contentTypeToExtension(contentType);
        }
        return writeFile(userId, "photos", file, extension, "PHOTO");
    }

    public StoredMedia storeJustificatifPdf(UUID userId, MultipartFile file) {
        validateNotEmpty(file);
        validateSize(file, MAX_PDF_BYTES, "Le PDF depasse la taille maximale autorisee (12MB)");

        String contentType = normalizedContentType(file);
        String extension = extensionFromFilename(file.getOriginalFilename());
        boolean pdfMime = "application/pdf".equals(contentType);
        boolean pdfExt = "pdf".equalsIgnoreCase(extension);
        if (!pdfMime && !pdfExt) {
            throw new MediaValidationException("Seuls les fichiers PDF sont autorises pour les justificatifs");
        }

        return writeFile(userId, "justificatifs", file, "pdf", "JUSTIFICATIF_PDF");
    }

    private StoredMedia writeFile(UUID userId, String folder, MultipartFile file, String extension, String category) {
        try {
            Files.createDirectories(uploadRoot.resolve("users").resolve(userId.toString()).resolve(folder));

            String safeExt = extension == null ? "bin" : extension.toLowerCase(Locale.ROOT);
            String generatedName = UUID.randomUUID() + "." + safeExt;

            Path userFolder = uploadRoot.resolve("users").resolve(userId.toString()).resolve(folder);
            Path target = userFolder.resolve(generatedName).normalize();
            if (!target.startsWith(userFolder)) {
                throw new MediaValidationException("Chemin fichier invalide");
            }

            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
            String fileUrl = "/uploads/users/" + userId + "/" + folder + "/" + generatedName;
            return new StoredMedia(category, file.getOriginalFilename(), normalizedContentType(file), fileUrl);
        } catch (IOException exception) {
            throw new MediaStorageException("Erreur lors de l'enregistrement du fichier", exception);
        }
    }

    private String normalizedContentType(MultipartFile file) {
        return file.getContentType() == null ? "application/octet-stream" : file.getContentType().toLowerCase(Locale.ROOT);
    }

    private String extensionFromFilename(String originalFilename) {
        if (originalFilename == null) {
            return null;
        }
        String trimmed = originalFilename.trim();
        int dot = trimmed.lastIndexOf('.');
        if (dot < 0 || dot == trimmed.length() - 1) {
            return null;
        }
        return trimmed.substring(dot + 1).replaceAll("[^A-Za-z0-9]", "").toLowerCase(Locale.ROOT);
    }

    private String contentTypeToExtension(String contentType) {
        return switch (contentType) {
            case "image/jpeg" -> "jpg";
            case "image/png" -> "png";
            case "image/webp" -> "webp";
            case "image/gif" -> "gif";
            default -> "bin";
        };
    }

    private void validateNotEmpty(MultipartFile file) {
        if (file.isEmpty()) {
            throw new MediaValidationException("Aucun fichier recu");
        }
    }

    private void validateSize(MultipartFile file, long maxSize, String message) {
        if (file.getSize() > maxSize) {
            throw new MediaValidationException(message);
        }
    }

    public record StoredMedia(String category, String originalFileName, String contentType, String fileUrl) {
    }

    public static class MediaValidationException extends RuntimeException {
        public MediaValidationException(String message) {
            super(message);
        }
    }

    public static class MediaStorageException extends RuntimeException {
        public MediaStorageException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
