package com.nti.nti_backend.bulk;

import com.nti.nti_backend.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/bulk-message")
@PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
@RequiredArgsConstructor
public class BulkMessageController {

    private final BulkMessageService service;

    /** Preview: returns recipient count without sending. */
    @PostMapping("/preview")
    public ResponseEntity<Map<String, Integer>> preview(
            @RequestBody BulkMessageRequest req) {
        int count = service.resolveRecipients(req).size();
        return ResponseEntity.ok(Map.of("recipientCount", count));
    }

    /** Send bulk message and return recipient count. */
    @PostMapping
    public ResponseEntity<Map<String, Integer>> send(
            @RequestBody BulkMessageRequest req,
            @AuthenticationPrincipal User actor) {
        int count = service.send(req, actor);
        return ResponseEntity.ok(Map.of("recipientCount", count));
    }
}
