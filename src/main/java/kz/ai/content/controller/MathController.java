package kz.ai.content.controller;

import kz.ai.content.model.ContentGeneration;
import kz.ai.content.model.User;
import kz.ai.content.repository.UserRepository;
import kz.ai.content.service.ContentService;
import kz.ai.content.service.OpenAiService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/content/math")
public class MathController {

    private final ContentService contentService;
    private final OpenAiService openAiService;
    private final UserRepository userRepository;

    public MathController(ContentService cs, OpenAiService oas, UserRepository ur) {
        this.contentService = cs; this.openAiService = oas; this.userRepository = ur;
    }

    @GetMapping
    public String mathPage(Model model) {
        return "content/math";
    }

    @PostMapping
    public String solveMath(@RequestParam(required = false) String problem,
                            @RequestParam(required = false) String subject,
                            @RequestParam(required = false) MultipartFile photo,
                            @AuthenticationPrincipal UserDetails ud,
                            RedirectAttributes ra) {
        User user = getUser(ud);
        try {
            OpenAiService.OpenAiResult result;
            String topic;

            if (photo != null && !photo.isEmpty()) {
                // Решение по фото через Gemini
                String prompt = buildMathPrompt(problem != null ? problem : "реши задачу с фото", subject);
                result = openAiService.generateWithImage(prompt, photo.getBytes(), photo.getContentType());
                topic = "📸 " + (problem != null && !problem.isBlank() ? problem : "Задача с фото");
            } else if (problem != null && !problem.isBlank()) {
                // Решение по тексту
                String prompt = buildMathPrompt(problem, subject);
                result = openAiService.generateContent(prompt);
                topic = problem;
            } else {
                ra.addFlashAttribute("error", "Введите условие задачи или загрузите фото");
                return "redirect:/content/math";
            }

            // Сохраняем как генерацию типа MATH
            ContentGeneration gen = contentService.generateAndSave(
                    topic.length() > 1990 ? topic.substring(0, 1990) : topic,
                    ContentGeneration.ContentType.MATH,
                    ContentGeneration.Style.ACADEMIC,
                    ContentGeneration.Language.RUSSIAN,
                    ContentGeneration.Volume.MEDIUM,
                    user);

            // Перезаписываем текст реальным ответом AI
            contentService.updateGeneratedText(gen.getId(), user, result.generatedText());

            return "redirect:/content/view/" + gen.getId();
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Ошибка: " + e.getMessage());
            return "redirect:/content/math";
        }
    }

    private String buildMathPrompt(String problem, String subject) {
        String subjectHint = subject != null ? switch (subject) {
            case "algebra"   -> "Алгебра. ";
            case "geometry"  -> "Геометрия. ";
            case "physics"   -> "Физика. ";
            case "chemistry" -> "Химия. ";
            default -> "";
        } : "";

        return """
            %sРеши следующую задачу ПОШАГОВО на русском языке.
            
            Требования:
            - Каждый шаг пронумеруй и объясни
            - Математические формулы записывай в LaTeX: $формула$ для инлайн
            - В конце напиши ОТВЕТ отдельно жирным
            - Если задача не математическая — напиши об этом
            
            Задача: %s
            """.formatted(subjectHint, problem);
    }

    private User getUser(UserDetails ud) {
        return userRepository.findByEmail(ud.getUsername())
                .orElseThrow(() -> new IllegalStateException("Пользователь не найден"));
    }
}
