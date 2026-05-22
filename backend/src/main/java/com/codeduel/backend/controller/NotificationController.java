package com.codeduel.backend.controller;

import com.codeduel.backend.dto.MarkNotificationsReadResponse;
import com.codeduel.backend.dto.NotificationListResponse;
import com.codeduel.backend.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public ResponseEntity<NotificationListResponse> getNotifications(Authentication authentication) {
        return ResponseEntity.ok(notificationService.getNotifications(authentication.getName()));
    }

    @PostMapping("/mark-all-read")
    public ResponseEntity<MarkNotificationsReadResponse> markAllRead(Authentication authentication) {
        return ResponseEntity.ok(notificationService.markAllAsRead(authentication.getName()));
    }
}
