package dev.rafex.chat.chat.handler;

import dev.rafex.chat.shared.error.AppError;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ChatMessageHandlerTest {

    @Test
    void rejectsMissingBearerScheme() {
        var error = assertThrows(AppError.Unauthorized.class, () -> ChatMessageHandler.extractBearerToken("token-without-scheme"));

        assertEquals("Authorization header must use Bearer scheme", error.getMessage());
    }

    @Test
    void rejectsEmptyBearerToken() {
        var error = assertThrows(AppError.Unauthorized.class, () -> ChatMessageHandler.extractBearerToken("Bearer   "));

        assertEquals("Bearer token is required", error.getMessage());
    }

    @Test
    void returnsBearerToken() {
        assertEquals("abc.def.ghi", ChatMessageHandler.extractBearerToken("Bearer abc.def.ghi"));
    }
}
