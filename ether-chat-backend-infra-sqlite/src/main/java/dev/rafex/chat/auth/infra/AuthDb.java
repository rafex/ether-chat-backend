package dev.rafex.chat.auth.infra;

import dev.rafex.ether.database.core.DatabaseClient;
import dev.rafex.ether.database.core.sql.SqlQuery;

public final class AuthDb {
    private AuthDb() {}
    public static void init(DatabaseClient db) {
        db.execute(SqlQuery.of("PRAGMA journal_mode=WAL;"));
        db.execute(SqlQuery.of("PRAGMA synchronous=NORMAL;"));
        db.execute(SqlQuery.of("PRAGMA cache_size=10000;"));
        db.execute(SqlQuery.of("PRAGMA temp_store=MEMORY;"));
        db.execute(SqlQuery.of("PRAGMA foreign_keys=ON;"));
        db.execute(SqlQuery.of("""
            CREATE TABLE IF NOT EXISTS users (
                id            INTEGER PRIMARY KEY AUTOINCREMENT,
                username      TEXT    NOT NULL UNIQUE,
                password_hash TEXT    NOT NULL,
                created_at    TEXT    NOT NULL DEFAULT (strftime('%Y-%m-%dT%H:%M:%SZ','now'))
            )
        """));
    }
}
