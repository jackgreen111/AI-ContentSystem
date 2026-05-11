package kz.ai.content.repository;

import kz.ai.content.model.ContentGeneration;
import kz.ai.content.model.ContentVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ContentVersionRepository extends JpaRepository<ContentVersion, Long> {
    List<ContentVersion> findByGenerationOrderByVersionNumDesc(ContentGeneration generation);
    long countByGeneration(ContentGeneration generation);
}
