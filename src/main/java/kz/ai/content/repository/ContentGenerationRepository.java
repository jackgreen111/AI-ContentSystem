package kz.ai.content.repository;

import kz.ai.content.model.ContentGeneration;
import kz.ai.content.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ContentGenerationRepository extends JpaRepository<ContentGeneration, Long> {
    List<ContentGeneration> findByUserOrderByCreatedAtDesc(User user);
    List<ContentGeneration> findByUserAndSavedTrueOrderByCreatedAtDesc(User user);
    long countByUser(User user);
    Optional<ContentGeneration> findByShareToken(String shareToken);

    @Query("SELECT g FROM ContentGeneration g WHERE g.user = ?1 AND LOWER(g.topic) LIKE LOWER(CONCAT('%', ?2, '%')) ORDER BY g.createdAt DESC")
    List<ContentGeneration> searchByTopic(User user, String query);

    @Query("SELECT COALESCE(SUM(g.tokensUsed),0) FROM ContentGeneration g WHERE g.user=?1")
    long sumTokensByUser(User user);

    @Query("SELECT COALESCE(SUM(g.wordCount),0) FROM ContentGeneration g WHERE g.user=?1")
    long sumWordsByUser(User user);

    @Query("SELECT COUNT(g) FROM ContentGeneration g WHERE g.user=?1 AND g.rating=1")
    long countLikedByUser(User user);

    @Query("SELECT COALESCE(SUM(g.tokensUsed),0) FROM ContentGeneration g")
    long sumAllTokens();

    @Query("SELECT COALESCE(SUM(g.wordCount),0) FROM ContentGeneration g")
    long sumAllWords();

    @Query("SELECT g.contentType, COUNT(g) FROM ContentGeneration g GROUP BY g.contentType")
    List<Object[]> countGroupByContentType();

    @Query("SELECT g.language, COUNT(g) FROM ContentGeneration g WHERE g.user=?1 GROUP BY g.language")
    List<Object[]> countByLanguageForUser(User user);

    @Query("SELECT g.contentType, COUNT(g) FROM ContentGeneration g WHERE g.user=?1 GROUP BY g.contentType")
    List<Object[]> countByTypeForUser(User user);

    @Query("SELECT g FROM ContentGeneration g WHERE g.user=?1 AND g.createdAt >= ?2 ORDER BY g.createdAt ASC")
    List<ContentGeneration> findByUserAfterDate(User user, LocalDateTime date);
}