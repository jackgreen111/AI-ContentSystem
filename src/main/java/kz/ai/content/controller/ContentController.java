package kz.ai.content.controller;

import kz.ai.content.model.*;
import kz.ai.content.repository.*;
import kz.ai.content.service.*;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/content")
public class ContentController {

    private final ContentService contentService;
    private final ExportService exportService;
    private final UserRepository userRepository;
    private final PromptTemplateRepository templateRepository;
    private final DocumentReaderService documentReaderService;
    private final kz.ai.content.repository.ContentGenerationRepository contentRepository;

    public ContentController(ContentService cs, ExportService es, UserRepository ur,
                             PromptTemplateRepository tr, DocumentReaderService dr,
                             kz.ai.content.repository.ContentGenerationRepository cr) {
        this.contentService = cs; this.exportService = es;
        this.userRepository = ur; this.templateRepository = tr;
        this.documentReaderService = dr; this.contentRepository = cr;
    }

    // ── Форма генерации ───────────────────────────────────────────────

    @GetMapping("/generate")
    public String generateForm(@RequestParam(required=false) String topicPrefill,
                               @RequestParam(required=false) String typePrefill,
                               @RequestParam(required=false) String langPrefill,
                               Model model) {
        model.addAttribute("contentTypes", ContentGeneration.ContentType.values());
        model.addAttribute("styles",       ContentGeneration.Style.values());
        model.addAttribute("languages",    ContentGeneration.Language.values());
        model.addAttribute("volumes",      ContentGeneration.Volume.values());
        model.addAttribute("topicPrefill", topicPrefill != null ? topicPrefill : "");
        model.addAttribute("typePrefill",  typePrefill != null ? typePrefill : "TEXT");
        model.addAttribute("langPrefill",  langPrefill != null ? langPrefill : "RUSSIAN");
        return "content/generate";
    }

    @PostMapping("/generate")
    public String doGenerate(@RequestParam String topic,
                             @RequestParam ContentGeneration.ContentType contentType,
                             @RequestParam ContentGeneration.Style style,
                             @RequestParam ContentGeneration.Language language,
                             @RequestParam ContentGeneration.Volume volume,
                             @RequestParam(required=false) MultipartFile image,
                             @AuthenticationPrincipal UserDetails ud) throws IOException {
        User user = getUser(ud);
        ContentGeneration result;
        if (image != null && !image.isEmpty()) {
            result = contentService.generateWithImage(topic, contentType, style, language,
                    volume, image.getBytes(), image.getContentType(), user);
        } else {
            result = contentService.generateAndSave(topic, contentType, style, language, volume, user);
        }
        return "redirect:/content/view/" + result.getId();
    }

    // ── Загрузка документа ────────────────────────────────────────────

    @GetMapping("/upload-doc")
    public String uploadDocForm() { return "content/upload-doc"; }

    @PostMapping("/upload-doc")
    public String uploadDoc(@RequestParam MultipartFile file,
                            @AuthenticationPrincipal UserDetails ud,
                            RedirectAttributes ra) {
        try {
            String text = documentReaderService.extractText(file);
            ContentGeneration gen = contentService.summarizeDocument(
                    text, file.getOriginalFilename(), getUser(ud));
            return "redirect:/content/view/" + gen.getId();
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", "Ошибка чтения файла: " + e.getMessage());
            return "redirect:/content/upload-doc";
        }
    }

    // ── Просмотр результата ───────────────────────────────────────────

    @GetMapping("/view/{id}")
    public String view(@PathVariable Long id, @AuthenticationPrincipal UserDetails ud, Model model) {
        User user = getUser(ud);
        contentService.getById(id, user).ifPresentOrElse(
                g  -> {
                    model.addAttribute("generation", g);
                    model.addAttribute("versions", contentService.getVersions(id, user));
                },
                () -> model.addAttribute("error", "Генерация не найдена"));
        return "content/view";
    }

    // ── JSON для сравнения ────────────────────────────────────────────

    @GetMapping("/{id}/json")
    @ResponseBody
    public ResponseEntity<?> getJson(@PathVariable Long id, @AuthenticationPrincipal UserDetails ud) {
        return contentService.getById(id, getUser(ud))
                .map(g -> ResponseEntity.ok(Map.of(
                        "topic", g.getTopic(),
                        "generatedText", g.getGeneratedText() != null ? g.getGeneratedText() : "",
                        "contentType", g.getContentType().getDisplayName(),
                        "language", g.getLanguage().getDisplayName(),
                        "wordCount", g.getWordCount() != null ? g.getWordCount() : 0
                ))).orElse(ResponseEntity.notFound().build());
    }

    // ── Регенерация ───────────────────────────────────────────────────

    @PostMapping("/{id}/regenerate")
    public String regenerate(@PathVariable Long id, @AuthenticationPrincipal UserDetails ud) {
        ContentGeneration gen = contentService.regenerate(id, getUser(ud));
        return "redirect:/content/view/" + gen.getId();
    }

    // ── Версии ────────────────────────────────────────────────────────

    @PostMapping("/{genId}/restore/{verId}")
    public String restoreVersion(@PathVariable Long genId, @PathVariable Long verId,
                                 @AuthenticationPrincipal UserDetails ud) {
        contentService.restoreVersion(genId, verId, getUser(ud));
        return "redirect:/content/view/" + genId;
    }

    // ── AI инструменты (AJAX) ─────────────────────────────────────────

    @PostMapping("/{id}/evaluate")
    @ResponseBody
    public ResponseEntity<?> evaluate(@PathVariable Long id, @AuthenticationPrincipal UserDetails ud) {
        try {
            String json = contentService.evaluateText(id, getUser(ud));
            return ResponseEntity.ok(Map.of("result", json));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{id}/proofread")
    @ResponseBody
    public ResponseEntity<?> proofread(@PathVariable Long id, @AuthenticationPrincipal UserDetails ud) {
        String result = contentService.proofread(id, getUser(ud));
        return ResponseEntity.ok(Map.of("result", result));
    }

    @PostMapping("/{id}/translate")
    @ResponseBody
    public ResponseEntity<?> translate(@PathVariable Long id,
                                       @RequestBody Map<String,String> body,
                                       @AuthenticationPrincipal UserDetails ud) {
        String result = contentService.translate(id, body.getOrDefault("lang","English"), getUser(ud));
        return ResponseEntity.ok(Map.of("result", result));
    }

    @PostMapping("/{id}/generate-image")
    public String generateImage(@PathVariable Long id, @AuthenticationPrincipal UserDetails ud) {
        contentService.generateAndAttachImage(id, getUser(ud));
        return "redirect:/content/view/" + id;
    }

    @PostMapping("/{id}/chat")
    @ResponseBody
    public ResponseEntity<?> chat(@PathVariable Long id,
                                  @RequestBody Map<String,String> body,
                                  @AuthenticationPrincipal UserDetails ud) {
        String reply = contentService.chatWithAi(id, getUser(ud),
                body.getOrDefault("message",""), body.getOrDefault("context",""));
        return ResponseEntity.ok(Map.of("reply", reply));
    }

    @PostMapping("/{id}/tags")
    @ResponseBody
    public ResponseEntity<?> updateTags(@PathVariable Long id,
                                        @RequestBody Map<String,String> body,
                                        @AuthenticationPrincipal UserDetails ud) {
        contentService.updateTags(id, getUser(ud), body.getOrDefault("tags",""));
        return ResponseEntity.ok(Map.of("ok", true));
    }

    @PostMapping("/{id}/rate")
    public String rate(@PathVariable Long id, @RequestParam int rating,
                       @AuthenticationPrincipal UserDetails ud,
                       @RequestHeader(value="Referer", defaultValue="/dashboard") String ref) {
        contentService.rate(id, getUser(ud), rating);
        return "redirect:" + ref;
    }

    @PostMapping("/{id}/share")
    public String share(@PathVariable Long id, @AuthenticationPrincipal UserDetails ud,
                        RedirectAttributes ra) {
        ContentGeneration gen = contentService.generateShareToken(id, getUser(ud));
        ra.addFlashAttribute("shareLink", "/public/" + gen.getShareToken());
        return "redirect:/content/view/" + id;
    }

    @PostMapping("/{id}/toggle-save")
    public String toggleSave(@PathVariable Long id, @AuthenticationPrincipal UserDetails ud,
                             @RequestHeader(value="Referer", defaultValue="/dashboard") String ref) {
        contentService.toggleSaved(id, getUser(ud));
        return "redirect:" + ref;
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, @AuthenticationPrincipal UserDetails ud) {
        contentService.delete(id, getUser(ud));
        return "redirect:/content/history";
    }

    // ── Экспорт ───────────────────────────────────────────────────────

    @GetMapping("/{id}/export/{format}")
    public ResponseEntity<byte[]> export(@PathVariable Long id, @PathVariable String format,
                                         @AuthenticationPrincipal UserDetails ud) throws IOException {
        ContentGeneration gen = contentService.getById(id, getUser(ud))
                .orElseThrow(() -> new IllegalArgumentException("Не найдено"));
        String filename = "content_" + id;
        byte[] data; MediaType mt;
        switch (format.toLowerCase()) {
            case "docx" -> { data=exportService.toDocx(gen);
                mt=MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
                filename+=".docx"; }
            case "pdf"  -> { data=exportService.toPdf(gen); mt=MediaType.APPLICATION_PDF; filename+=".pdf"; }
            default     -> { data=exportService.toTxt(gen); mt=MediaType.TEXT_PLAIN; filename+=".txt"; }
        }
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\""+filename+"\"")
                .contentType(mt).body(data);
    }

    // ── Страницы списков ──────────────────────────────────────────────

    @GetMapping("/history")
    public String history(@AuthenticationPrincipal UserDetails ud, Model model) {
        model.addAttribute("generations", contentService.getUserHistory(getUser(ud)));
        return "content/history";
    }

    @GetMapping("/library")
    public String library(@AuthenticationPrincipal UserDetails ud, Model model) {
        model.addAttribute("generations", contentService.getSavedGenerations(getUser(ud)));
        return "content/library";
    }

    @GetMapping("/stats")
    public String stats(@AuthenticationPrincipal UserDetails ud, Model model) {
        User user = getUser(ud);
        model.addAttribute("stats",        contentService.getUserStats(user));
        model.addAttribute("typeChartData",contentService.getTypeChartData(user));
        model.addAttribute("langChartData",contentService.getLangChartData(user));
        model.addAttribute("activityData", contentService.getActivityData(user));
        return "content/stats";
    }

    @GetMapping("/templates")
    public String templates(@AuthenticationPrincipal UserDetails ud, Model model) {
        User user = getUser(ud);
        model.addAttribute("templates",    templateRepository.findByUserOrderByCreatedAtDesc(user));
        model.addAttribute("contentTypes", ContentGeneration.ContentType.values());
        model.addAttribute("styles",       ContentGeneration.Style.values());
        model.addAttribute("languages",    ContentGeneration.Language.values());
        return "content/templates";
    }

    @PostMapping("/templates/save")
    public String saveTemplate(@RequestParam String name, @RequestParam String topic,
                               @RequestParam String contentType, @RequestParam String style,
                               @RequestParam String language,
                               @AuthenticationPrincipal UserDetails ud) {
        PromptTemplate t = new PromptTemplate();
        t.setName(name); t.setTopic(topic);
        t.setContentType(contentType); t.setStyle(style); t.setLanguage(language);
        t.setUser(getUser(ud)); templateRepository.save(t);
        return "redirect:/content/templates";
    }

    @PostMapping("/templates/{id}/delete")
    public String deleteTemplate(@PathVariable Long id, @AuthenticationPrincipal UserDetails ud) {
        templateRepository.findById(id).ifPresent(t -> {
            if (t.getUser().getId().equals(getUser(ud).getId())) templateRepository.delete(t);
        });
        return "redirect:/content/templates";
    }

    @GetMapping("/compare")
    public String compare(@AuthenticationPrincipal UserDetails ud, Model model) {
        model.addAttribute("generations", contentService.getUserHistory(getUser(ud)));
        return "content/compare";
    }

    // ── Поиск для sidebar ───────────────────────────────────────
    @GetMapping("/search")
    @ResponseBody
    public ResponseEntity<?> search(@RequestParam String q,
                                    @AuthenticationPrincipal UserDetails ud) {
        if (q == null || q.trim().length() < 2)
            return ResponseEntity.ok(List.of());
        User user = getUser(ud);
        List<Map<String, Object>> results = contentRepository
                .searchByTopic(user, q.trim())
                .stream().limit(6)
                .map(g -> Map.<String, Object>of("id", g.getId(), "topic", g.getTopic()))
                .toList();
        return ResponseEntity.ok(results);
    }

    private User getUser(UserDetails ud) {
        return userRepository.findByEmail(ud.getUsername())
                .orElseThrow(() -> new IllegalStateException("Пользователь не найден"));
    }
}