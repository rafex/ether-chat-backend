package dev.rafex.chat.shared.error;

public sealed class AppError extends RuntimeException
        permits AppError.Unauthorized, AppError.NotFound, AppError.BadRequest {
    public AppError(String message) { super(message); }
    public static final class Unauthorized extends AppError {
        public Unauthorized(String msg) { super(msg); }
    }
    public static final class NotFound extends AppError {
        public NotFound(String msg) { super(msg); }
    }
    public static final class BadRequest extends AppError {
        public BadRequest(String msg) { super(msg); }
    }
}
