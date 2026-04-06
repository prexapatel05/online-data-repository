package com.onlinedatatepo.data_repository.service;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import com.onlinedatatepo.data_repository.entity.Comment;
import com.onlinedatatepo.data_repository.entity.Dataset;
import com.onlinedatatepo.data_repository.entity.Rating;
import com.onlinedatatepo.data_repository.entity.User;

import jakarta.mail.internet.MimeMessage;

@Service
public class MailService {

    private static final Logger LOGGER = LoggerFactory.getLogger(MailService.class);

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    @Value("${app.mail.from:noreply@datahub.local}")
    private String fromAddress;

    @Value("${app.mail.from-name:DataHub}")
    private String fromName;

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    public MailService(JavaMailSender mailSender, TemplateEngine templateEngine) {
        this.mailSender = mailSender;
        this.templateEngine = templateEngine;
    }

    @Async
    public void sendWelcomeEmail(User user) {
        if (user == null || user.getEmail() == null || user.getEmail().isBlank()) {
            return;
        }

        Map<String, Object> variables = Map.of(
                "name", displayName(user),
                "dashboardUrl", baseUrl + "/dashboard"
        );

        sendTemplateEmail(user.getEmail(), "Welcome to DataHub", "emails/welcome", variables);
    }

    @Async
    public void sendCommentNotification(User recipient, User actor, Dataset dataset, Comment comment, boolean isReply) {
        if (!shouldSend(recipient, actor)) {
            return;
        }

        String subject = isReply ? "New reply on your dataset" : "New comment on your dataset";
        Map<String, Object> variables = Map.of(
                "recipientName", displayName(recipient),
                "actorName", displayName(actor),
                "datasetName", dataset.getName(),
                "commentText", comment.getContent(),
                "datasetUrl", baseUrl + "/datasets/" + dataset.getDatasetId() + "?tab=discussion"
        );

        sendTemplateEmail(recipient.getEmail(), subject, "emails/comment-notification", variables);
    }

    @Async
    public void sendReplyNotification(User recipient, User actor, Dataset dataset, Comment comment) {
        if (!shouldSend(recipient, actor)) {
            return;
        }

        Map<String, Object> variables = Map.of(
                "recipientName", displayName(recipient),
                "actorName", displayName(actor),
                "datasetName", dataset.getName(),
                "commentText", comment.getContent(),
                "datasetUrl", baseUrl + "/datasets/" + dataset.getDatasetId() + "?tab=discussion"
        );

        sendTemplateEmail(recipient.getEmail(), "New reply to your comment", "emails/reply-notification", variables);
    }

    @Async
    public void sendRatingNotification(User recipient, User actor, Dataset dataset, Rating rating, boolean updated) {
        if (!shouldSend(recipient, actor)) {
            return;
        }

        Map<String, Object> variables = Map.of(
                "recipientName", displayName(recipient),
                "actorName", displayName(actor),
                "datasetName", dataset.getName(),
                "ratingValue", rating.getRatingValue(),
                "actionLabel", updated ? "updated their rating" : "rated your dataset",
                "datasetUrl", baseUrl + "/datasets/" + dataset.getDatasetId() + "?tab=discussion"
        );

        sendTemplateEmail(recipient.getEmail(), "New rating on your dataset", "emails/rating-notification", variables);
    }

    private void sendTemplateEmail(String to, String subject, String templateName, Map<String, Object> variables) {
        try {
            Context context = new Context();
            context.setVariables(variables);
            String html = templateEngine.process(templateName, context);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromAddress, fromName);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(html, true);
            mailSender.send(message);
        } catch (Exception ex) {
            LOGGER.warn("Failed to send mail '{}' to '{}': {}", subject, to, ex.getMessage());
        }
    }

    private boolean shouldSend(User recipient, User actor) {
        if (recipient == null || actor == null || recipient.getUserId() == null || actor.getUserId() == null) {
            return false;
        }
        if (recipient.getUserId().equals(actor.getUserId())) {
            return false;
        }
        return recipient.getEmail() != null && !recipient.getEmail().isBlank();
    }

    private String displayName(User user) {
        if (user.getFullName() != null && !user.getFullName().isBlank()) {
            return user.getFullName();
        }
        if (user.getUsername() != null && !user.getUsername().isBlank()) {
            return user.getUsername();
        }
        return "User";
    }
}
