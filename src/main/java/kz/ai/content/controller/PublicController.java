package kz.ai.content.controller;

import kz.ai.content.service.ContentService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class PublicController {
    private final ContentService contentService;
    public PublicController(ContentService cs) { this.contentService = cs; }

    @GetMapping("/public/{token}")
    public String publicShare(@PathVariable String token, Model model) {
        contentService.getByShareToken(token).ifPresentOrElse(
                g  -> model.addAttribute("generation", g),
                () -> model.addAttribute("error", "Ссылка недействительна"));
        return "public/share";
    }
}
