package kz.ai.content.service;
import kz.ai.content.model.User;
import kz.ai.content.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
@Service
public class UserService {
    private static final Logger log = LoggerFactory.getLogger(UserService.class);
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    public UserService(UserRepository ur, PasswordEncoder pe){ this.userRepository=ur; this.passwordEncoder=pe; }
    @Transactional
    public User register(String username, String email, String rawPassword){
        if(userRepository.existsByEmail(email)) throw new IllegalArgumentException("Email уже зарегистрирован");
        if(userRepository.existsByUsername(username)) throw new IllegalArgumentException("Имя уже занято");
        User u=User.builder().username(username).email(email).password(passwordEncoder.encode(rawPassword)).role(User.Role.USER).enabled(true).build();
        log.info("Зарегистрирован: {}",email);
        return userRepository.save(u);
    }
    public Optional<User> findByEmail(String email){ return userRepository.findByEmail(email); }
    @Transactional
    public void updateLastLogin(String email){ userRepository.findByEmail(email).ifPresent(u->{u.setLastLogin(LocalDateTime.now());userRepository.save(u);}); }
    @Transactional
    public void toggleTheme(String email){ userRepository.findByEmail(email).ifPresent(u->{u.setPreferredTheme("dark".equals(u.getPreferredTheme())?"light":"dark");userRepository.save(u);}); }
    public List<User> getAllUsers(){ return userRepository.findAll(); }
    @Transactional
    public void toggleEnabled(Long id){ userRepository.findById(id).ifPresent(u->{u.setEnabled(!u.isEnabled());userRepository.save(u);}); }
    @Transactional
    public void deleteUser(Long id){ userRepository.deleteById(id); }
}
