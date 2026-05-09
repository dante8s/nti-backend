package com.nti.nti_backend.file;

import com.nti.nti_backend.organization.exception.ResourceNotFoundException;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;

import java.net.MalformedURLException;
import java.net.URLConnection;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
public class FileServeService {
    public Resource load(String filePath) {
        try {
            Path path = Paths.get(filePath);
            Resource resource = new UrlResource(path.toUri());
            if (resource.exists() && resource.isReadable()) {
                return resource;
            }
            throw new ResourceNotFoundException("Resource not found or not readable: " + filePath);
        } catch (MalformedURLException e) {
            throw new ResourceNotFoundException(
                    "File not found: " + filePath
            );
        }
    }

    public String detectContentType(String fileName) {
        String type = URLConnection.guessContentTypeFromName(fileName);
        return type != null ? type : "application/octet-stream";
    }

    // open inline PDFs and images, download attachments
    public String contentDisposition(boolean inline, String fileName) {
        String disposition = inline ? "inline" : "attachment";
        return disposition + "; filename=\"" + fileName + "\"";
    }
}
