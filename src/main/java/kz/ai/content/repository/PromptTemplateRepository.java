package kz.ai.content.repository;
import kz.ai.content.model.PromptTemplate;
import kz.ai.content.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PromptTemplateRepository extends JpaRepository<PromptTemplate, Long> {
    List<PromptTemplate> findByUserOrderByCreatedAtDesc(User user);
}
