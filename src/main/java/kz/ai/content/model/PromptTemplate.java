package kz.ai.content.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "prompt_templates")
public class PromptTemplate {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false) private String name;
    @Column(nullable = false, length = 500) private String topic;
    @Column private String contentType;
    @Column private String style;
    @Column private String language;
    @Column(name = "created_at") private LocalDateTime createdAt = LocalDateTime.now();
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "user_id") private User user;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getTopic() { return topic; }
    public void setTopic(String topic) { this.topic = topic; }
    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }
    public String getStyle() { return style; }
    public void setStyle(String style) { this.style = style; }
    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
}
