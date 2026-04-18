package dev.rafex.chat.auth.port;
import dev.rafex.chat.auth.domain.Credentials;
import dev.rafex.chat.auth.domain.Session;
public interface AuthPort {
    Session login(Credentials credentials);
}
