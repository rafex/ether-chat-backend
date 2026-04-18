package dev.rafex.chat.chat.domain;
public record Message(Long id, String conversationId, MessageRole role, String content, String createdAt) {}
