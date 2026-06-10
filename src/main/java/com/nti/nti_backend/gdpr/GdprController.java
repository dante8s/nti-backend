package com.nti.nti_backend.gdpr;

import com.nti.nti_backend.user.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Map;

@RestController
@RequestMapping("/api/gdpr")
@RequiredArgsConstructor
public class GdprController {

    private final GdprService    gdprService;
    private final ObjectMapper   objectMapper;

    @GetMapping("/export")
    public ResponseEntity<byte[]> exportData(
            @AuthenticationPrincipal User user) throws Exception {

        Map<String, Object> data = gdprService.exportData(user);
        byte[] json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(data);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"my-data.json\"")
                .contentType(MediaType.APPLICATION_JSON)
                .body(json);
    }

    @DeleteMapping("/account")
    public ResponseEntity<String> deleteAccount(
            @Valid @RequestBody DeleteAccountRequest request,
            @AuthenticationPrincipal User user) {

        gdprService.anonymize(user, request.password());
        return ResponseEntity.ok("Personal data deleted. Account deactivated.");
    }
}
