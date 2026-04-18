package dev.rafex.chat.chat.service;

import dev.rafex.chat.chat.domain.ChatResponse;
import dev.rafex.chat.chat.domain.Message;
import dev.rafex.chat.chat.domain.MessageRole;
import dev.rafex.chat.chat.port.AiGateway;
import dev.rafex.chat.chat.port.ChatService;
import dev.rafex.chat.chat.port.ConversationRepository;
import dev.rafex.chat.chat.port.MessageRepository;
import dev.rafex.chat.shared.error.AppError;
import java.util.Objects;

public final class ChatServiceImpl implements ChatService {
    private final ConversationRepository convRepo;
    private final MessageRepository msgRepo;
    private final AiGateway ai;

    public ChatServiceImpl(ConversationRepository convRepo, MessageRepository msgRepo, AiGateway ai) {
        this.convRepo = Objects.requireNonNull(convRepo, "convRepo");
        this.msgRepo  = Objects.requireNonNull(msgRepo,  "msgRepo");
        this.ai       = Objects.requireNonNull(ai,       "ai");
    }

    @Override
    public ChatResponse sendMessage(String userId, String conversationId, String message) {
        Objects.requireNonNull(userId, "userId");
        if (message == null || message.isBlank()) throw new AppError.BadRequest("message is required");
        var conv    = convRepo.findOrCreate(userId, conversationId);
        msgRepo.save(new Message(null, conv.id(), MessageRole.user, message, null));
        var history = msgRepo.findByConversationId(conv.id());
        var content = ai.generate(history);
        msgRepo.save(new Message(null, conv.id(), MessageRole.agent, content, null));
        return new ChatResponse(content, conv.id());
    }
}
