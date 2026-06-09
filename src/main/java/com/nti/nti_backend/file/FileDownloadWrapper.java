package com.nti.nti_backend.file;
import org.springframework.core.io.Resource;

public record FileDownloadWrapper(Resource resource, String contentType, String contentDisposition) {}