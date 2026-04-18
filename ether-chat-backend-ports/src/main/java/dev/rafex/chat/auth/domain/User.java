package dev.rafex.chat.auth.domain;
public record User(Long id, String username, String passwordHash, String createdAt) {}
