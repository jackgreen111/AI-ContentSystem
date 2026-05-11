package kz.ai.content.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.*;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.io.IOException;
import java.util.Base64;
import java.util.concurrent.TimeUnit;

@Service
public class OpenAiService {

    private static final Logger log = LoggerFactory.getLogger(OpenAiService.class);

    @Value("${openai.api.key}")       private String apiKey;
    @Value("${openai.api.url}")       private String apiUrl;
    @Value("${openai.api.model}")     private String model;
    @Value("${openai.api.max-tokens}") private int maxTokens;
    @Value("${openai.api.temperature}") private double temperature;

    private final ObjectMapper mapper = new ObjectMapper();
    private final OkHttpClient http = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build();

    /** Обычная генерация текста */
    public OpenAiResult generateContent(String prompt) {
        return generate(buildTextBody(prompt));
    }

    /** Генерация с изображением через Groq Vision (Llama 4) */
    public OpenAiResult generateWithImage(String prompt, byte[] imageBytes, String mimeType) {
        return generate(buildVisionBodyGroq(prompt, imageBytes, mimeType));
    }

    /** Vision запрос через Groq — тот же ключ и URL что для текста */
    private ObjectNode buildVisionBodyGroq(String prompt, byte[] imageBytes, String mimeType) {
        String base64 = Base64.getEncoder().encodeToString(imageBytes);
        ObjectNode body = mapper.createObjectNode();
        // Llama 4 Scout поддерживает изображения, работает на Groq бесплатно
        body.put("model", "meta-llama/llama-4-scout-17b-16e-instruct");
        body.put("max_tokens", maxTokens);
        body.put("temperature", temperature);
        ArrayNode messages = body.putArray("messages");
        ObjectNode usr = messages.addObject();
        usr.put("role", "user");
        ArrayNode content = usr.putArray("content");
        // Текст
        ObjectNode textPart = content.addObject();
        textPart.put("type", "text");
        textPart.put("text", prompt);
        // Картинка в base64
        ObjectNode imgPart = content.addObject();
        imgPart.put("type", "image_url");
        ObjectNode imgUrl = imgPart.putObject("image_url");
        imgUrl.put("url", "data:" + mimeType + ";base64," + base64);
        return body;
    }

    /** AI-оценка текста — возвращает JSON строку */
    public String evaluateText(String text, String language) {
        String evalPrompt = """
            Оцени следующий текст по 5 критериям. Верни ТОЛЬКО JSON без пояснений:
            {
              "grammar": <0-100>,
              "style": <0-100>,
              "structure": <0-100>,
              "topic": <0-100>,
              "originality": <0-100>,
              "summary": "<одно предложение — общий вывод на языке: %s>"
            }
            
            Текст для оценки:
            %s
            """.formatted(language, text);
        OpenAiResult result = generateContent(evalPrompt);
        return result.generatedText();
    }

    /** Перевод с объяснением */
    public OpenAiResult translate(String text, String targetLang) {
        String prompt = """
            Переведи текст на %s. Затем дай краткое объяснение 2-3 интересных грамматических особенностей перевода.
            
            Формат ответа:
            ## Перевод
            <переведённый текст>
            
            ## Грамматические особенности
            <объяснение>
            
            Текст: %s
            """.formatted(targetLang, text);
        return generateContent(prompt);
    }

    /** Проверка и исправление текста */
    public OpenAiResult proofread(String text, String language) {
        String prompt = """
            Проверь текст на ошибки (орфография, пунктуация, стиль). 
            Язык текста: %s
            
            Формат ответа:
            ## Исправленный текст
            <текст с исправлениями>
            
            ## Найденные ошибки
            <список ошибок с объяснением>
            
            Текст:
            %s
            """.formatted(language, text);
        return generateContent(prompt);
    }

    /** Суммаризация документа */
    public OpenAiResult summarize(String documentText) {
        String prompt = """
            Сделай краткое содержание следующего документа.
            Выдели: главные темы, ключевые факты, основные выводы.
            Используй тот же язык что и документ.
            
            Документ:
            %s
            """.formatted(documentText.substring(0, Math.min(documentText.length(), 8000)));
        return generateContent(prompt);
    }

    /** Генерация изображения через Pollinations (бесплатно, без ключа) */
    public String generateImageUrl(String topic) {
        String encoded = topic.replace(" ", "+").replace(",", "");
        return "https://image.pollinations.ai/prompt/" + encoded + "?width=512&height=512&nologo=true";
    }

    // ── Приватные методы ──────────────────────────────────────────────

    private OpenAiResult generate(ObjectNode body) {
        for (int attempt = 1; attempt <= 3; attempt++) {
            try {
                String response = post(body.toString());
                return parse(response);
            } catch (IOException e) {
                log.warn("Попытка {} из 3: {}", attempt, e.getMessage());
                if (attempt == 3) return new OpenAiResult("Ошибка подключения к AI. Проверьте API-ключ.", 0);
                try { Thread.sleep(2000L * attempt); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
            }
        }
        return new OpenAiResult("Не удалось получить ответ.", 0);
    }

    private ObjectNode buildTextBody(String prompt) {
        ObjectNode body = mapper.createObjectNode();
        body.put("model", model);
        body.put("max_tokens", maxTokens);
        body.put("temperature", temperature);
        ArrayNode messages = body.putArray("messages");
        ObjectNode sys = messages.addObject();
        sys.put("role", "system");
        sys.put("content", "Ты профессиональный AI-ассистент. Отвечай на языке запроса.");
        ObjectNode usr = messages.addObject();
        usr.put("role", "user");
        usr.put("content", prompt);
        return body;
    }

    private ObjectNode buildVisionBody(String prompt, byte[] imageBytes, String mimeType) {
        // Gemini формат (не OpenAI)
        ObjectNode body = mapper.createObjectNode();
        body.put("model", "gemini-1.5-flash");
        body.put("max_tokens", maxTokens);
        ArrayNode messages = body.putArray("messages");
        ObjectNode usr = messages.addObject();
        usr.put("role", "user");
        ArrayNode content = usr.putArray("content");
        ObjectNode textPart = content.addObject();
        textPart.put("type", "text");
        textPart.put("text", prompt);
        ObjectNode imgPart = content.addObject();
        imgPart.put("type", "image_url");
        ObjectNode imgUrl = imgPart.putObject("image_url");
        imgUrl.put("url", "data:" + mimeType + ";base64," + Base64.getEncoder().encodeToString(imageBytes));
        return body;
    }

    private String post(String jsonBody) throws IOException {
        RequestBody rb = RequestBody.create(jsonBody, MediaType.parse("application/json; charset=utf-8"));
        Request req = new Request.Builder()
                .url(apiUrl)
                .addHeader("Authorization", "Bearer " + apiKey)
                .post(rb).build();
        try (Response resp = http.newCall(req).execute()) {
            if (!resp.isSuccessful()) {
                String err = resp.body() != null ? resp.body().string() : "";
                throw new IOException("HTTP " + resp.code() + ": " + err);
            }
            return resp.body().string();
        }
    }

    private OpenAiResult parse(String json) {
        try {
            JsonNode root = mapper.readTree(json);
            String text = root.path("choices").get(0).path("message").path("content").asText("Пустой ответ.");
            int tokens = root.path("usage").path("total_tokens").asInt(0);
            return new OpenAiResult(text, tokens);
        } catch (Exception e) {
            log.error("Ошибка разбора ответа", e);
            return new OpenAiResult("Ошибка обработки ответа.", 0);
        }
    }

    public record OpenAiResult(String generatedText, int tokensUsed) {}
}