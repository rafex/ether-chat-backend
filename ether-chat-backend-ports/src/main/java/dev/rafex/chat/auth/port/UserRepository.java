package dev.rafex.chat.auth.port;
import dev.rafex.chat.auth.domain.User;
import java.util.Optional;
public interface UserRepository {
    Optional<User> findByUsername(String username);
    User save(User user);
}
