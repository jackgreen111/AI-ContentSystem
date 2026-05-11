package kz.ai.content;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
@SpringBootApplication
public class AiContentSystemApplication {
    public static void main(String[] args) {
        SpringApplication.run(AiContentSystemApplication.class, args);
        System.out.println("\n========================================");
        System.out.println("  AI Content System v2.0 запущен!");
        System.out.println("  http://localhost:8080");
        System.out.println("========================================\n");
    }
}
