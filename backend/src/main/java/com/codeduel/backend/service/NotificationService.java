package com.codeduel.backend.service;

import com.codeduel.backend.dto.MarkNotificationsReadResponse;
import com.codeduel.backend.dto.NotificationListResponse;
import com.codeduel.backend.dto.NotificationResponse;
import com.codeduel.backend.model.Notification;
import com.codeduel.backend.model.User;
import com.codeduel.backend.model.enums.NotificationType;
import com.codeduel.backend.repository.NotificationRepository;
import com.codeduel.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    @Transactional
    public Notification createRankChangeNotification(User user, int oldElo, int newElo) {
        int delta = newElo - oldElo;
        String sign = delta > 0 ? "+" : "";

        Notification notification = Notification.builder()
                .user(user)
                .type(NotificationType.RANK_CHANGE)
                .title("Rank updated")
                .message("Your ELO changed " + sign + delta + " (" + oldElo + " → " + newElo + ")")
                .oldElo(oldElo)
                .newElo(newElo)
                .build();

        return notificationRepository.save(notification);
    }

    @Transactional(readOnly = true)
    public NotificationListResponse getNotifications(String username) {
        User user = getUser(username);
        List<NotificationResponse> notifications = notificationRepository.findByUserIdOrderByCreatedAtDesc(user.getId())
                .stream()
                .map(notification -> NotificationResponse.builder()
                        .id(notification.getId())
                        .type(notification.getType())
                        .title(notification.getTitle())
                        .message(notification.getMessage())
                        .read(notification.isRead())
                        .oldElo(notification.getOldElo())
                        .newElo(notification.getNewElo())
                        .createdAt(notification.getCreatedAt())
                        .build())
                .toList();

        return NotificationListResponse.builder()
                .unreadCount(notificationRepository.countByUserIdAndReadFalse(user.getId()))
                .notifications(notifications)
                .build();
    }

    @Transactional
    public MarkNotificationsReadResponse markAllAsRead(String username) {
        User user = getUser(username);
        List<Notification> notifications = notificationRepository.findByUserIdOrderByCreatedAtDesc(user.getId());
        long markedCount = notifications.stream()
                .filter(notification -> !notification.isRead())
                .peek(notification -> notification.setRead(true))
                .count();

        if (markedCount > 0) {
            notificationRepository.saveAll(notifications);
        }

        return MarkNotificationsReadResponse.builder()
                .markedCount(markedCount)
                .unreadCount(notificationRepository.countByUserIdAndReadFalse(user.getId()))
                .build();
    }

    private User getUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalStateException("User not found: " + username));
    }
}
