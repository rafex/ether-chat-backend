package dev.rafex.chat.chat.infra;

import dev.rafex.ether.database.core.DatabaseClient;
import dev.rafex.ether.database.core.sql.SqlQuery;

public final class ChatDb {
    private ChatDb() {}
    public static void init(DatabaseClient db) {
        db.execute(SqlQuery.of("PRAGMA journal_mode=WAL;"));
        db.execute(SqlQuery.of("PRAGMA synchronous=NORMAL;"));
        db.execute(SqlQuery.of("PRAGMA cache_size=10000;"));
        db.execute(SqlQuery.of("PRAGMA temp_store=MEMORY;"));
        db.execute(SqlQuery.of("PRAGMA foreign_keys=ON;"));
        db.execute(SqlQuery.of("""
            CREATE TABLE IF NOT EXISTS conversations (
                id         TEXT    PRIMARY KEY,
                user_id    TEXT    NOT NULL,
                created_at TEXT    NOT NULL DEFAULT (strftime('%Y-%m-%dT%H:%M:%SZ','now')),
                updated_at TEXT    NOT NULL DEFAULT (strftime('%Y-%m-%dT%H:%M:%SZ','now'))
            )
        """));
        db.execute(SqlQuery.of("""
            CREATE TABLE IF NOT EXISTS messages (
                id              INTEGER PRIMARY KEY AUTOINCREMENT,
                conversation_id TEXT    NOT NULL REFERENCES conversations(id),
                role            TEXT    NOT NULL CHECK (role IN ('user','agent')),
                content         TEXT    NOT NULL,
                created_at      TEXT    NOT NULL DEFAULT (strftime('%Y-%m-%dT%H:%M:%SZ','now'))
            )
        """));
        db.execute(SqlQuery.of("CREATE INDEX IF NOT EXISTS idx_messages_conversation ON messages(conversation_id, created_at)"));
    }
}
