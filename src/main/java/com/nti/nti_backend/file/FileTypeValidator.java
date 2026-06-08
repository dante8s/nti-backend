package com.nti.nti_backend.file;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;

/**
 * Validates actual file content via magic bytes — not Content-Type header,
 * which can be forged by the client.
 */
public final class FileTypeValidator {

    private FileTypeValidator() {}

    /** PDF: magic %PDF = 25 50 44 46 */
    public static boolean isPdf(MultipartFile file) throws IOException {
        String name = file.getOriginalFilename();
        if (name == null || !name.toLowerCase().endsWith(".pdf")) return false;
        return startsWithMagic(file, 0x25, 0x50, 0x44, 0x46);
    }

    /** DOCX is a ZIP archive: PK = 50 4B 03 04, plus .docx extension */
    public static boolean isDocx(MultipartFile file) throws IOException {
        String name = file.getOriginalFilename();
        if (name == null || !name.toLowerCase().endsWith(".docx")) return false;
        return startsWithMagic(file, 0x50, 0x4B, 0x03, 0x04);
    }

    /** JPEG: FF D8 FF */
    public static boolean isJpeg(MultipartFile file) throws IOException {
        String name = file.getOriginalFilename();
        if (name == null) return false;
        String lower = name.toLowerCase();
        if (!lower.endsWith(".jpg") && !lower.endsWith(".jpeg")) return false;
        return startsWithMagic(file, 0xFF, 0xD8, 0xFF);
    }

    /** PNG: 89 50 4E 47 0D 0A 1A 0A */
    public static boolean isPng(MultipartFile file) throws IOException {
        String name = file.getOriginalFilename();
        if (name == null || !name.toLowerCase().endsWith(".png")) return false;
        return startsWithMagic(file, 0x89, 0x50, 0x4E, 0x47);
    }

    /** WebP: bytes 0-3 = RIFF, bytes 8-11 = WEBP */
    public static boolean isWebp(MultipartFile file) throws IOException {
        String name = file.getOriginalFilename();
        if (name == null || !name.toLowerCase().endsWith(".webp")) return false;
        byte[] header = readBytes(file, 12);
        if (header.length < 12) return false;
        return (header[0] & 0xFF) == 0x52 && (header[1] & 0xFF) == 0x49
            && (header[2] & 0xFF) == 0x46 && (header[3] & 0xFF) == 0x46   // RIFF
            && (header[8] & 0xFF) == 0x57 && (header[9] & 0xFF) == 0x45
            && (header[10] & 0xFF) == 0x42 && (header[11] & 0xFF) == 0x50; // WEBP
    }

    /** Accepts JPEG, PNG, WebP — sufficient for profile photos */
    public static boolean isAllowedImage(MultipartFile file) throws IOException {
        return isJpeg(file) || isPng(file) || isWebp(file);
    }

    private static boolean startsWithMagic(MultipartFile file, int... magic) throws IOException {
        byte[] header = readBytes(file, magic.length);
        if (header.length < magic.length) return false;
        for (int i = 0; i < magic.length; i++) {
            if ((header[i] & 0xFF) != magic[i]) return false;
        }
        return true;
    }

    private static byte[] readBytes(MultipartFile file, int count) throws IOException {
        try (InputStream is = file.getInputStream()) {
            return is.readNBytes(count);
        }
    }
}
