package kz.ai.content.controller;

import kz.ai.content.repository.ContentGenerationRepository;
import kz.ai.content.service.UserService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {
    private final UserService userService;
    private final ContentGenerationRepository repo;

    public AdminController(UserService us, ContentGenerationRepository r) {
        this.userService = us; this.repo = r;
    }

    @GetMapping
    public String dashboard(Model model) {
        model.addAttribute("totalUsers",       userService.getAllUsers().size());
        model.addAttribute("totalGenerations", repo.count());
        model.addAttribute("totalTokens",      repo.sumAllTokens());
        model.addAttribute("totalWords",       repo.sumAllWords());
        model.addAttribute("statsByType",      repo.countGroupByContentType());
        return "admin/dashboard";
    }

    @GetMapping("/users")
    public String users(Model model) {
        model.addAttribute("users", userService.getAllUsers()); return "admin/users";
    }

    @PostMapping("/users/{id}/toggle")
    public String toggle(@PathVariable Long id) { userService.toggleEnabled(id); return "redirect:/admin/users"; }

    @PostMapping("/users/{id}/delete")
    public String delete(@PathVariable Long id) { userService.deleteUser(id); return "redirect:/admin/users"; }
}
