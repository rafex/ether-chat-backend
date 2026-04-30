package dev.rafex.chat.auth.domain;
public record Session(String token, String expiresAt, String userId) {}
