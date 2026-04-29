package dev.rafex.chat.auth.infra;

import dev.rafex.chat.auth.domain.User;
import dev.rafex.chat.auth.port.UserRepository;
import dev.rafex.ether.database.core.DatabaseClient;
import dev.rafex.ether.database.core.sql.SqlBuilder;
import dev.rafex.ether.database.core.sql.SqlQuery;
import java.util.Objects;
import java.util.Optional;

public final class UserRepositoryImpl implements UserRepository {
    private final DatabaseClient db;

    public UserRepositoryImpl(DatabaseClient db) {
        this.db = Objects.requireNonNull(db, "db");
    }

    @Override
    public Optional<User> findByUsername(String username) {
        return db.queryOne(
            new SqlBuilder("SELECT id, username, password_hash, created_at FROM users WHERE username=").param(username).build(),
            rs -> new User(rs.getLong("id"), rs.getString("username"), rs.getString("password_hash"), rs.getString("created_at"))
        );
    }

    @Override
    public User save(User user) {
        db.execute(new SqlBuilder("INSERT INTO users(username, password_hash) VALUES(").param(user.username()).append(",").param(user.passwordHash()).append(")").build());
        long id = db.queryOne(SqlQuery.of("SELECT last_insert_rowid()"), rs -> rs.getLong(1)).orElse(0L);
        return new User(id, user.username(), user.passwordHash(), user.createdAt());
    }
}
