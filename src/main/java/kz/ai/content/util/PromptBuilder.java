package kz.ai.content.util;

import kz.ai.content.model.ContentGeneration;
import org.springframework.stereotype.Component;

@Component
public class PromptBuilder {

    public String build(String topic,
                        ContentGeneration.ContentType contentType,
                        ContentGeneration.Style style,
                        ContentGeneration.Language language,
                        ContentGeneration.Volume volume) {
        return getLang(language) + "\n\n"
             + getType(contentType) + "\n\n"
             + getStyle(style) + "\n\n"
             + getVolume(volume) + "\n\n"
             + "Тема / задание: " + topic + "\n\n"
             + "Создай качественный структурированный текст. "
             + "Используй Markdown-форматирование. "
             + (contentType == ContentGeneration.ContentType.MATH
                ? "Математические формулы записывай в LaTeX: $формула$ для инлайн, $$формула$$ для блока."
                : "");
    }

    private String getLang(ContentGeneration.Language l) {
        return switch (l) {
            case RUSSIAN -> "Ответ должен быть полностью на русском языке.";
            case KAZAKH  -> "Жауап толығымен қазақ тілінде болуы керек.";
            case ENGLISH -> "The response must be entirely in English.";
        };
    }

    private String getType(ContentGeneration.ContentType t) {
        return switch (t) {
            case TEXT     -> "Создай информационный текст по теме.";
            case TASK     -> "Создай учебное задание с чёткими инструкциями и критериями оценки.";
            case QUESTION -> "Составь список вопросов (минимум 5-7) от простых к сложным.";
            case LETTER   -> "Напиши деловое письмо: обращение, основная часть, заключение.";
            case PLAN     -> "Составь подробный план с разделами, подпунктами и временными рамками.";
            case MATH     -> "Реши задачу пошагово. Объясни каждый шаг. Используй LaTeX для формул.";
        };
    }

    private String getStyle(ContentGeneration.Style s) {
        return switch (s) {
            case FORMAL   -> "Используй формальный официальный стиль.";
            case INFORMAL -> "Используй неформальный дружелюбный стиль.";
            case ACADEMIC -> "Используй академический научный стиль с терминологией.";
            case CREATIVE -> "Используй творческий образный стиль.";
        };
    }

    private String getVolume(ContentGeneration.Volume v) {
        return switch (v) {
            case SHORT  -> "Объём: до 200 слов. Будь лаконичен.";
            case MEDIUM -> "Объём: 200–500 слов. Охвати основные аспекты.";
            case LONG   -> "Объём: более 500 слов. Раскрой тему детально.";
        };
    }
}
