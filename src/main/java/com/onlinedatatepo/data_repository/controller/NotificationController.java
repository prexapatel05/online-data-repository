package com.onlinedatatepo.data_repository.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.onlinedatatepo.data_repository.entity.User;
import com.onlinedatatepo.data_repository.service.AuthService;
import com.onlinedatatepo.data_repository.service.NotificationService;

@Controller
@RequestMapping("/notifications")
public class NotificationController {

    private final NotificationService notificationService;
    private final AuthService authService;

    public NotificationController(NotificationService notificationService, AuthService authService) {
        this.notificationService = notificationService;
        this.authService = authService;
    }

    @GetMapping("/unread-count")
    @ResponseBody
    public Map<String, Long> unreadCount(org.springframework.security.core.Authentication authentication) {
        User user = authService.findByEmail(authentication.getName());
        return Map.of("count", notificationService.unreadCount(user.getUserId()));
    }

    @GetMapping("/recent")
    @ResponseBody
    public List<NotificationService.NotificationItem> recent(@RequestParam(value = "limit", defaultValue = "8") int limit,
                                                             org.springframework.security.core.Authentication authentication) {
        User user = authService.findByEmail(authentication.getName());
        return notificationService.recent(user.getUserId(), limit);
    }

    @GetMapping("/{notificationId}/read")
    public String markRead(@PathVariable Integer notificationId,
                           @RequestParam(value = "redirect", required = false) String redirect,
                           org.springframework.security.core.Authentication authentication) {
        User user = authService.findByEmail(authentication.getName());
        notificationService.markAsRead(user.getUserId(), notificationId);

        if (redirect != null && !redirect.isBlank() && redirect.startsWith("/")) {
            return "redirect:" + redirect;
        }
        return "redirect:/dashboard";
    }

    @GetMapping("/read-all")
    @ResponseBody
    public ResponseEntity<Void> markAllRead(org.springframework.security.core.Authentication authentication) {
        User user = authService.findByEmail(authentication.getName());
        notificationService.markAllAsRead(user.getUserId());
        return ResponseEntity.ok().build();
    }
}
