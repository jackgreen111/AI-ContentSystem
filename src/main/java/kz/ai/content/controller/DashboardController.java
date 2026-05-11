package kz.ai.content.controller;

import kz.ai.content.model.User;
import kz.ai.content.repository.UserRepository;
import kz.ai.content.service.ContentService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DashboardController {
    private final UserRepository userRepository;
    private final ContentService contentService;

    public DashboardController(UserRepository ur, ContentService cs) {
        this.userRepository = ur; this.contentService = cs;
    }

    @GetMapping("/")
    public String home() { return "home"; }

    @GetMapping("/dashboard")
    public String dashboard(@AuthenticationPrincipal UserDetails ud, Model model) {
        User user = getUser(ud);
        model.addAttribute("user", user);
        model.addAttribute("stats", contentService.getUserStats(user));
        model.addAttribute("recentGenerations",
                contentService.getUserHistory(user).stream().limit(5).toList());
        return "dashboard";
    }

    @GetMapping("/profile")
    public String profile(@AuthenticationPrincipal UserDetails ud, Model model) {
        model.addAttribute("user", getUser(ud)); return "profile";
    }

    protected User getUser(UserDetails ud) {
        return userRepository.findByEmail(ud.getUsername())
                .orElseThrow(() -> new IllegalStateException("Пользователь не найден"));
    }
}
