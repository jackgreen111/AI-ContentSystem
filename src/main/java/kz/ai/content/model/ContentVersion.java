package kz.ai.content.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "content_versions")
public class ContentVersion {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "generation_id", nullable = false)
    private ContentGeneration generation;

    @Column(columnDefinition = "TEXT")
    private String text;

    @Column(name = "version_num")
    private Integer versionNum;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    public ContentVersion() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public ContentGeneration getGeneration() { return generation; }
    public void setGeneration(ContentGeneration g) { this.generation = g; }
    public String getText() { return text; }
    public void setText(String text) { this.text = text; }
    public Integer getVersionNum() { return versionNum; }
    public void setVersionNum(Integer v) { this.versionNum = v; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime t) { this.createdAt = t; }
}
