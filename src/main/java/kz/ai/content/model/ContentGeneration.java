package kz.ai.content.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "content_generations")
public class ContentGeneration {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 500) private String topic;
    @Enumerated(EnumType.STRING) @Column(name = "content_type", nullable = false) private ContentType contentType;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private Style style;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private Language language;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private Volume volume = Volume.MEDIUM;
    @Column(name = "generated_text", columnDefinition = "TEXT") private String generatedText;
    @Column(name = "prompt_sent", columnDefinition = "TEXT") private String promptSent;
    @Column(name = "tokens_used") private Integer tokensUsed = 0;
    @Column(name = "word_count") private Integer wordCount = 0;
    @Column(name = "created_at", nullable = false) private LocalDateTime createdAt = LocalDateTime.now();
    @Column(name = "is_saved") private boolean saved = false;
    @Column(name = "rating") private Integer rating = 0;
    @Column(name = "tags", length = 200) private String tags = "";
    @Column(name = "share_token", unique = true) private String shareToken;
    @Column(name = "image_url", length = 500) private String imageUrl;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "user_id", nullable = false) private User user;
    @OneToMany(mappedBy = "generation", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ContentVersion> versions;

    public enum ContentType {
        TEXT("Текст"), TASK("Задание"), QUESTION("Вопрос"),
        LETTER("Письмо"), PLAN("План"), MATH("Математика");
        private final String d; ContentType(String d){this.d=d;}
        public String getDisplayName(){return d;}
    }
    public enum Style {
        FORMAL("Формальный"), INFORMAL("Неформальный"),
        ACADEMIC("Академический"), CREATIVE("Креативный");
        private final String d; Style(String d){this.d=d;}
        public String getDisplayName(){return d;}
    }
    public enum Language {
        RUSSIAN("Русский"), KAZAKH("Казахский"), ENGLISH("English");
        private final String d; Language(String d){this.d=d;}
        public String getDisplayName(){return d;}
    }
    public enum Volume {
        SHORT("Короткий (до 200 слов)"), MEDIUM("Средний (200–500 слов)"), LONG("Длинный (500+ слов)");
        private final String d; Volume(String d){this.d=d;}
        public String getDisplayName(){return d;}
    }

    public ContentGeneration() {}

    public Long getId(){return id;} public void setId(Long id){this.id=id;}
    public String getTopic(){return topic;} public void setTopic(String t){this.topic=t;}
    public ContentType getContentType(){return contentType;} public void setContentType(ContentType c){this.contentType=c;}
    public Style getStyle(){return style;} public void setStyle(Style s){this.style=s;}
    public Language getLanguage(){return language;} public void setLanguage(Language l){this.language=l;}
    public Volume getVolume(){return volume;} public void setVolume(Volume v){this.volume=v;}
    public String getGeneratedText(){return generatedText;} public void setGeneratedText(String t){this.generatedText=t;}
    public String getPromptSent(){return promptSent;} public void setPromptSent(String p){this.promptSent=p;}
    public Integer getTokensUsed(){return tokensUsed;} public void setTokensUsed(Integer t){this.tokensUsed=t;}
    public Integer getWordCount(){return wordCount;} public void setWordCount(Integer w){this.wordCount=w;}
    public LocalDateTime getCreatedAt(){return createdAt;} public void setCreatedAt(LocalDateTime t){this.createdAt=t;}
    public boolean isSaved(){return saved;} public void setSaved(boolean s){this.saved=s;}
    public Integer getRating(){return rating;} public void setRating(Integer r){this.rating=r;}
    public String getTags(){return tags;} public void setTags(String t){this.tags=t;}
    public String getShareToken(){return shareToken;} public void setShareToken(String s){this.shareToken=s;}
    public String getImageUrl(){return imageUrl;} public void setImageUrl(String u){this.imageUrl=u;}
    public User getUser(){return user;} public void setUser(User u){this.user=u;}

    public static Builder builder(){return new Builder();}
    public static class Builder {
        private final ContentGeneration g = new ContentGeneration();
        public Builder topic(String v){g.topic=v;return this;}
        public Builder contentType(ContentType v){g.contentType=v;return this;}
        public Builder style(Style v){g.style=v;return this;}
        public Builder language(Language v){g.language=v;return this;}
        public Builder volume(Volume v){g.volume=v;return this;}
        public Builder generatedText(String v){g.generatedText=v;return this;}
        public Builder promptSent(String v){g.promptSent=v;return this;}
        public Builder tokensUsed(Integer v){g.tokensUsed=v;return this;}
        public Builder wordCount(Integer v){g.wordCount=v;return this;}
        public Builder saved(boolean v){g.saved=v;return this;}
        public Builder imageUrl(String v){g.imageUrl=v;return this;}
        public Builder user(User v){g.user=v;return this;}
        public ContentGeneration build(){return g;}
    }
}
