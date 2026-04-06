package com.onlinedatatepo.data_repository.service;

import java.time.format.DateTimeFormatter;
import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.onlinedatatepo.data_repository.entity.Comment;
import com.onlinedatatepo.data_repository.entity.Dataset;
import com.onlinedatatepo.data_repository.entity.Notification;
import com.onlinedatatepo.data_repository.entity.NotificationType;
import com.onlinedatatepo.data_repository.entity.Rating;
import com.onlinedatatepo.data_repository.entity.User;
import com.onlinedatatepo.data_repository.repository.NotificationRepository;

@Service
public class NotificationService {

    public record NotificationItem(Integer notificationId,
                                   String message,
                                   String type,
                                   String timestamp,
                                   String targetUrl) {
    }

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final NotificationRepository notificationRepository;

    public NotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @Transactional
    public void notifyComment(User recipient, User actor, Dataset dataset, Comment comment) {
        if (shouldSkip(recipient, actor)) {
            return;
        }

        Notification notification = new Notification();
        notification.setUser(recipient);
        notification.setType(NotificationType.COMMENT);
        notification.setDataset(dataset);
        notification.setComment(comment);
        notification.setMessage(displayName(actor) + " commented on your dataset \"" + dataset.getName() + "\".");
        notificationRepository.save(notification);
    }

    @Transactional
    public void notifyReply(User recipient, User actor, Dataset dataset, Comment comment) {
        if (shouldSkip(recipient, actor)) {
            return;
        }

        Notification notification = new Notification();
        notification.setUser(recipient);
        notification.setType(NotificationType.REPLY);
        notification.setDataset(dataset);
        notification.setComment(comment);
        notification.setMessage(displayName(actor) + " replied to your comment on \"" + dataset.getName() + "\".");
        notificationRepository.save(notification);
    }

    @Transactional
    public void notifyRating(User recipient, User actor, Dataset dataset, Rating rating, boolean updated) {
        if (shouldSkip(recipient, actor)) {
            return;
        }

        Notification notification = new Notification();
        notification.setUser(recipient);
        notification.setType(NotificationType.RATING);
        notification.setDataset(dataset);
        notification.setRating(rating);
        String action = updated ? "updated their rating" : "rated";
        notification.setMessage(displayName(actor) + " " + action + " your dataset \"" + dataset.getName() + "\" (" + rating.getRatingValue() + "/5).");
        notificationRepository.save(notification);
    }

    @Transactional(readOnly = true)
    public long unreadCount(Integer userId) {
        return notificationRepository.countByUser_UserIdAndIsReadFalse(userId);
    }

    @Transactional(readOnly = true)
    public List<NotificationItem> recent(Integer userId, int limit) {
        return notificationRepository.findByUser_UserId(
                        userId,
                        PageRequest.of(0, Math.max(1, limit), Sort.by(Sort.Direction.DESC, "timestamp"))
                )
                .getContent()
                .stream()
                .map(this::toNotificationItem)
                .toList();
    }

    @Transactional
    public void markAsRead(Integer userId, Integer notificationId) {
        notificationRepository.findById(notificationId).ifPresent(notification -> {
            if (notification.getUser() != null && notification.getUser().getUserId().equals(userId)) {
                notification.setIsRead(true);
                notificationRepository.save(notification);
            }
        });
    }

    @Transactional
    public void markAllAsRead(Integer userId) {
        List<Notification> unread = notificationRepository.findByUser_UserIdAndIsReadFalse(userId);
        for (Notification notification : unread) {
            notification.setIsRead(true);
        }
        notificationRepository.saveAll(unread);
    }

    private NotificationItem toNotificationItem(Notification notification) {
        String targetUrl = "/dashboard";
        if (notification.getDataset() != null && notification.getDataset().getDatasetId() != null) {
            targetUrl = "/datasets/" + notification.getDataset().getDatasetId() + "?tab=discussion";
        }

        return new NotificationItem(
                notification.getNotificationId(),
                notification.getMessage(),
                notification.getType() != null ? notification.getType().name() : "INFO",
                notification.getTimestamp() != null ? notification.getTimestamp().format(DATE_TIME_FORMATTER) : "",
                targetUrl
        );
    }

    private boolean shouldSkip(User recipient, User actor) {
        if (recipient == null || actor == null || recipient.getUserId() == null || actor.getUserId() == null) {
            return true;
        }
        return recipient.getUserId().equals(actor.getUserId());
    }

    private String displayName(User user) {
        if (user.getFullName() != null && !user.getFullName().isBlank()) {
            return user.getFullName();
        }
        if (user.getUsername() != null && !user.getUsername().isBlank()) {
            return user.getUsername();
        }
        return "A user";
    }
}
