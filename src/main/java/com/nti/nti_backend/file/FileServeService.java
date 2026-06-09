package com.nti.nti_backend.file;

import com.nti.nti_backend.organization.exception.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import org.springframework.http.MediaTypeFactory;

@Service
public class FileServeService {

    @Value("${app.upload-dir:uploads}")
    private String uploadDir;

    private static final List<String> ALLOWED_IMAGE_TYPES = List.of(
            "image/jpeg", "image/png", "image/webp", "image/gif"
    );
    private static final long MAX_IMAGE_BYTES = 5L * 1024 * 1024;

    public Resource load(String filePath) {
        try {
            Path base = Paths.get(uploadDir).toAbsolutePath().normalize();

            // FIX: If an old database row contains "uploads/" or "uploads\", strip it out
            if (filePath.startsWith("uploads/") || filePath.startsWith("uploads\\")) {
                filePath = filePath.substring(8); // removes "uploads/" or "uploads\"
            }

            Path resolved = base.resolve(filePath).normalize();

            if (!resolved.startsWith(base)) {
                throw new ResourceNotFoundException(
                        "Access denied — path outside upload directory: " + filePath
                );
            }

            Resource resource = new UrlResource(resolved.toUri());
            if (resource.exists() && resource.isReadable()) {
                return resource;
            }
            throw new ResourceNotFoundException(
                    "Resource not found or not readable: " + filePath
            );
        } catch (MalformedURLException e) {
            throw new ResourceNotFoundException("File not found: " + filePath);
        }
    }

    public String detectContentType(String fileName) {
        // Fix: Use Spring's native utility to ensure modern formats like WebP are detected correctly
        return MediaTypeFactory.getMediaType(fileName)
                .map(org.springframework.http.MediaType::toString)
                .orElse("application/octet-stream");
    }

    // Fix 3: sanitize filename before embedding in header value
    // removes quotes and newlines which would break the header
    public String contentDisposition(boolean inline, String fileName) {
        String safeFileName = fileName
                .replace("\"", "")
                .replace("\n", "")
                .replace("\r", "");
        String disposition = inline ? "inline" : "attachment";
        return disposition + "; filename=\"" + safeFileName + "\"";
    }

    public void validateImage(MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("File cannot be empty");
        }
        if (file.getSize() > MAX_IMAGE_BYTES) {
            throw new IllegalArgumentException("Image must be at most 5 MB");
        }
        String contentType = file.getContentType();
        if (contentType == null
                || !ALLOWED_IMAGE_TYPES.contains(contentType.toLowerCase())) {
            throw new IllegalArgumentException(
                    "Only image files are allowed: JPEG, PNG, WebP, GIF"
            );
        }
    }

    public String saveImage(MultipartFile file, String subDir, String filename)
            throws IOException {
        String safeFilename = Paths.get(filename).getFileName().toString();

        Path base       = Paths.get(uploadDir).toAbsolutePath().normalize();
        Path uploadPath = base.resolve(subDir).normalize();

        if (!uploadPath.startsWith(base)) {
            throw new IllegalArgumentException(
                    "Invalid subdirectory: " + subDir
            );
        }

        Files.createDirectories(uploadPath);

        Path target = uploadPath.resolve(safeFilename);
        Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);

        // Final Fix: Force forward-slashes so Windows developers don't accidentally
        // save backslashes (\) into the database, which breaks on Linux servers.
        return base.relativize(target).toString().replace('\\', '/');
    }

    public void deleteFile(String relativeFilePath) {
        try {
            Path base = Paths.get(uploadDir).toAbsolutePath().normalize();
            Path targetFile = base.resolve(relativeFilePath).toAbsolutePath().normalize();

            if (targetFile.startsWith(base)) {
                Files.deleteIfExists(targetFile);
            }
        } catch (IOException e) {
            // Log the warning but don't crash the database transaction
            System.err.println("Failed to delete physical file: " + relativeFilePath);
        }
    }
}