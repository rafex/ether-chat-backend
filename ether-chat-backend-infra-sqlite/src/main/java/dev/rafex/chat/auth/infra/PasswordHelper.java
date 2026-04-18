package dev.rafex.chat.auth.infra;

import dev.rafex.ether.crypto.password.PasswordHasherPBKDF2;
import java.security.SecureRandom;
import java.util.Base64;

public final class PasswordHelper {
    private static final int ITERATIONS = 10000;
    private PasswordHelper() {}

    public static String hash(String password) {
        byte[] salt = new byte[16];
        new SecureRandom().nextBytes(salt);
        var ph = new PasswordHasherPBKDF2(ITERATIONS).hash(password.toCharArray(), salt, ITERATIONS);
        return Base64.getEncoder().encodeToString(ph.salt()) + ":" + ph.iterations() + ":" + Base64.getEncoder().encodeToString(ph.hash());
    }
}
