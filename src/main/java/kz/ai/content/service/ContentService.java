package kz.ai.content.service;

import kz.ai.content.model.*;
import kz.ai.content.repository.*;
import kz.ai.content.util.PromptBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class ContentService {

    private static final Logger log = LoggerFactory.getLogger(ContentService.class);

    private final ContentGenerationRepository repo;
    private final ContentVersionRepository versionRepo;
    private final OpenAiService openAiService;
    private final PromptBuilder promptBuilder;

    public ContentService(ContentGenerationRepository r, ContentVersionRepository vr,
                          OpenAiService o, PromptBuilder p) {
        this.repo = r; this.versionRepo = vr;
        this.openAiService = o; this.promptBuilder = p;
    }

    // ── Генерация текста ──────────────────────────────────────────────

    @Transactional
    public ContentGeneration generateAndSave(String topic,
            ContentGeneration.ContentType contentType, ContentGeneration.Style style,
            ContentGeneration.Language language, ContentGeneration.Volume volume, User user) {
        log.info("Генерация: {}, тема: {}", user.getEmail(), topic);
        String prompt = promptBuilder.build(topic, contentType, style, language, volume);
        OpenAiService.OpenAiResult result = openAiService.generateContent(prompt);
        return createAndSave(topic, contentType, style, language, volume,
                result.generatedText(), result.tokensUsed(), null, user);
    }

    // ── Генерация с изображением ──────────────────────────────────────

    @Transactional
    public ContentGeneration generateWithImage(String topic,
            ContentGeneration.ContentType contentType, ContentGeneration.Style style,
            ContentGeneration.Language language, ContentGeneration.Volume volume,
            byte[] imageBytes, String mimeType, User user) {
        log.info("Vision-генерация: {}, тема: {}", user.getEmail(), topic);
        String prompt = promptBuilder.build(topic, contentType, style, language, volume);
        OpenAiService.OpenAiResult result = openAiService.generateWithImage(prompt, imageBytes, mimeType);
        return createAndSave(topic, contentType, style, language, volume,
                result.generatedText(), result.tokensUsed(), null, user);
    }

    // ── Суммаризация документа ────────────────────────────────────────

    @Transactional
    public ContentGeneration summarizeDocument(String docText, String filename, User user) {
        log.info("Суммаризация документа: {}", filename);
        OpenAiService.OpenAiResult result = openAiService.summarize(docText);
        return createAndSave("📄 " + filename,
                ContentGeneration.ContentType.TEXT, ContentGeneration.Style.FORMAL,
                ContentGeneration.Language.RUSSIAN, ContentGeneration.Volume.MEDIUM,
                result.generatedText(), result.tokensUsed(), null, user);
    }

    // ── Внутренний метод создания и сохранения ────────────────────────

    private ContentGeneration createAndSave(String topic,
            ContentGeneration.ContentType type, ContentGeneration.Style style,
            ContentGeneration.Language lang, ContentGeneration.Volume vol,
            String text, int tokens, String imageUrl, User user) {
        ContentGeneration gen = ContentGeneration.builder()
                .topic(topic).contentType(type).style(style).language(lang).volume(vol)
                .generatedText(text).tokensUsed(tokens)
                .wordCount(countWords(text)).imageUrl(imageUrl)
                .user(user).saved(false).build();
        ContentGeneration saved = repo.save(gen);
        saveVersion(saved, text, 1);
        return saved;
    }

    // ── Регенерация с сохранением версии ─────────────────────────────

    @Transactional
    public ContentGeneration regenerate(Long id, User user) {
        ContentGeneration old = getById(id, user)
                .orElseThrow(() -> new IllegalArgumentException("Не найдено"));
        String prompt = promptBuilder.build(old.getTopic(), old.getContentType(),
                old.getStyle(), old.getLanguage(), old.getVolume());
        OpenAiService.OpenAiResult result = openAiService.generateContent(prompt);
        int nextVer = (int) (versionRepo.countByGeneration(old) + 1);
        old.setGeneratedText(result.generatedText());
        old.setWordCount(countWords(result.generatedText()));
        old.setTokensUsed(result.tokensUsed());
        ContentGeneration updated = repo.save(old);
        saveVersion(updated, result.generatedText(), nextVer);
        return updated;
    }

    // ── Восстановление версии ─────────────────────────────────────────

    @Transactional
    public ContentGeneration restoreVersion(Long genId, Long versionId, User user) {
        ContentGeneration gen = getById(genId, user)
                .orElseThrow(() -> new IllegalArgumentException("Не найдено"));
        ContentVersion ver = versionRepo.findById(versionId)
                .orElseThrow(() -> new IllegalArgumentException("Версия не найдена"));
        gen.setGeneratedText(ver.getText());
        gen.setWordCount(countWords(ver.getText()));
        return repo.save(gen);
    }

    public List<ContentVersion> getVersions(Long genId, User user) {
        ContentGeneration gen = getById(genId, user)
                .orElseThrow(() -> new IllegalArgumentException("Не найдено"));
        return versionRepo.findByGenerationOrderByVersionNumDesc(gen);
    }

    private void saveVersion(ContentGeneration gen, String text, int num) {
        ContentVersion v = new ContentVersion();
        v.setGeneration(gen); v.setText(text); v.setVersionNum(num);
        versionRepo.save(v);
    }

    // ── AI инструменты ────────────────────────────────────────────────

    public String evaluateText(Long id, User user) {
        ContentGeneration gen = getById(id, user)
                .orElseThrow(() -> new IllegalArgumentException("Не найдено"));
        return openAiService.evaluateText(
                gen.getGeneratedText(), gen.getLanguage().getDisplayName());
    }

    public String proofread(Long id, User user) {
        ContentGeneration gen = getById(id, user)
                .orElseThrow(() -> new IllegalArgumentException("Не найдено"));
        return openAiService.proofread(
                gen.getGeneratedText(), gen.getLanguage().getDisplayName()).generatedText();
    }

    public String translate(Long id, String targetLang, User user) {
        ContentGeneration gen = getById(id, user)
                .orElseThrow(() -> new IllegalArgumentException("Не найдено"));
        return openAiService.translate(gen.getGeneratedText(), targetLang).generatedText();
    }

    public String chatWithAi(Long id, User user, String message, String context) {
        String prompt = "Контекст (текст):\n" + context
                + "\n\nВопрос пользователя: " + message
                + "\n\nОтветь кратко и по делу на языке вопроса.";
        return openAiService.generateContent(prompt).generatedText();
    }

    @Transactional
    public ContentGeneration generateAndAttachImage(Long id, User user) {
        ContentGeneration gen = getById(id, user)
                .orElseThrow(() -> new IllegalArgumentException("Не найдено"));
        String url = openAiService.generateImageUrl(gen.getTopic());
        gen.setImageUrl(url);
        return repo.save(gen);
    }

    // ── CRUD ──────────────────────────────────────────────────────────

    public List<ContentGeneration> getUserHistory(User user) {
        return repo.findByUserOrderByCreatedAtDesc(user);
    }

    public List<ContentGeneration> getSavedGenerations(User user) {
        return repo.findByUserAndSavedTrueOrderByCreatedAtDesc(user);
    }

    public Optional<ContentGeneration> getById(Long id, User user) {
        return repo.findById(id).filter(g -> g.getUser().getId().equals(user.getId()));
    }

    public Optional<ContentGeneration> getByShareToken(String token) {
        return repo.findByShareToken(token);
    }

    @Transactional
    public ContentGeneration toggleSaved(Long id, User user) {
        ContentGeneration g = getById(id, user).orElseThrow();
        g.setSaved(!g.isSaved());
        return repo.save(g);
    }

    @Transactional
    public ContentGeneration rate(Long id, User user, int rating) {
        ContentGeneration g = getById(id, user).orElseThrow();
        g.setRating(g.getRating() == rating ? 0 : rating);
        return repo.save(g);
    }

    @Transactional
    public ContentGeneration updateTags(Long id, User user, String tags) {
        ContentGeneration g = getById(id, user).orElseThrow();
        g.setTags(tags);
        return repo.save(g);
    }

    @Transactional
    public ContentGeneration updateGeneratedText(Long id, User user, String text) {
        ContentGeneration g = getById(id, user).orElseThrow();
        g.setGeneratedText(text);
        g.setWordCount(countWords(text));
        return repo.save(g);
    }

    @Transactional
    public ContentGeneration generateShareToken(Long id, User user) {
        ContentGeneration g = getById(id, user).orElseThrow();
        if (g.getShareToken() == null)
            g.setShareToken(UUID.randomUUID().toString().replace("-","").substring(0,16));
        return repo.save(g);
    }

    @Transactional
    public void delete(Long id, User user) {
        getById(id, user).ifPresent(repo::delete);
    }

    // ── Статистика ────────────────────────────────────────────────────

    public UserStats getUserStats(User user) {
        return new UserStats(
                repo.countByUser(user),
                repo.sumTokensByUser(user),
                repo.sumWordsByUser(user),
                getSavedGenerations(user).size(),
                repo.countLikedByUser(user));
    }

    public Map<String, Long> getTypeChartData(User user) {
        Map<String, Long> m = new LinkedHashMap<>();
        repo.countByTypeForUser(user)
                .forEach(r -> m.put(r[0].toString(), (Long) r[1]));
        return m;
    }

    public Map<String, Long> getLangChartData(User user) {
        Map<String, Long> m = new LinkedHashMap<>();
        repo.countByLanguageForUser(user)
                .forEach(r -> m.put(r[0].toString(), (Long) r[1]));
        return m;
    }

    public Map<String, Long> getActivityData(User user) {
        LocalDateTime from = LocalDateTime.now().minusDays(13);
        List<ContentGeneration> list = repo.findByUserAfterDate(user, from);
        Map<String, Long> m = new LinkedHashMap<>();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd.MM");
        for (int i = 13; i >= 0; i--)
            m.put(LocalDate.now().minusDays(i).format(fmt), 0L);
        list.forEach(g -> m.merge(g.getCreatedAt().toLocalDate().format(fmt), 1L, Long::sum));
        return m;
    }

    // ── Вспомогательные ──────────────────────────────────────────────

    private int countWords(String text) {
        if (text == null || text.isBlank()) return 0;
        return text.trim().split("\\s+").length;
    }

    public record UserStats(long totalGenerations, long totalTokens,
                            long totalWords, long savedCount, long likedCount) {}
}
