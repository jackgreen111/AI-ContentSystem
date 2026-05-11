package kz.ai.content.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "users")
public class User {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank @Column(nullable = false, unique = true)
    private String username;

    @NotBlank @Email @Column(nullable = false, unique = true)
    private String email;

    @NotBlank @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private Role role = Role.USER;

    @Column(nullable = false)
    private boolean enabled = true;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "last_login")
    private LocalDateTime lastLogin;

    /** light или dark */
    @Column(name = "preferred_theme")
    private String preferredTheme = "light";

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<ContentGeneration> generations;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<PromptTemplate> promptTemplates;

    public enum Role { USER, ADMIN }

    public User() {}

    public static Builder builder() { return new Builder(); }
    public static class Builder {
        private final User u = new User();
        public Builder username(String v) { u.username=v; return this; }
        public Builder email(String v)    { u.email=v; return this; }
        public Builder password(String v) { u.password=v; return this; }
        public Builder role(Role v)       { u.role=v; return this; }
        public Builder enabled(boolean v) { u.enabled=v; return this; }
        public User build() { return u; }
    }

    public Long getId() { return id; }
    public String getUsername() { return username; }
    public void setUsername(String v) { this.username=v; }
    public String getEmail() { return email; }
    public void setEmail(String v) { this.email=v; }
    public String getPassword() { return password; }
    public void setPassword(String v) { this.password=v; }
    public Role getRole() { return role; }
    public void setRole(Role v) { this.role=v; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean v) { this.enabled=v; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getLastLogin() { return lastLogin; }
    public void setLastLogin(LocalDateTime v) { this.lastLogin=v; }
    public String getPreferredTheme() { return preferredTheme; }
    public void setPreferredTheme(String v) { this.preferredTheme=v; }
    public List<ContentGeneration> getGenerations() { return generations; }
    public List<PromptTemplate> getPromptTemplates() { return promptTemplates; }
}
