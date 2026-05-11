package kz.ai.content.config;
import kz.ai.content.model.User;
import kz.ai.content.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
@Component
public class DataInitializer implements CommandLineRunner {
    private static final Logger log=LoggerFactory.getLogger(DataInitializer.class);
    private final UserRepository ur; private final PasswordEncoder pe;
    public DataInitializer(UserRepository ur,PasswordEncoder pe){this.ur=ur;this.pe=pe;}
    @Override public void run(String... args){
        if(!ur.existsByEmail("admin@ai-content.kz")){
            ur.save(User.builder().username("Администратор").email("admin@ai-content.kz").password(pe.encode("Admin123!")).role(User.Role.ADMIN).enabled(true).build());
            log.info("Создан admin: admin@ai-content.kz / Admin123!");
        }
        if(!ur.existsByEmail("user@test.kz")){
            ur.save(User.builder().username("Тестовый пользователь").email("user@test.kz").password(pe.encode("User1234!")).role(User.Role.USER).enabled(true).build());
            log.info("Создан user: user@test.kz / User1234!");
        }
    }
}
