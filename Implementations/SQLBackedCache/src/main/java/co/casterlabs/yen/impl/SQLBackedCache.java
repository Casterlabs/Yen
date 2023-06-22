package co.casterlabs.yen.impl;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.jetbrains.annotations.Nullable;

import co.casterlabs.rakurai.json.Rson;
import co.casterlabs.yen.Cache;
import co.casterlabs.yen.CacheIterator;
import co.casterlabs.yen.Cacheable;
import lombok.Getter;
import lombok.NonNull;
import lombok.SneakyThrows;

public class SQLBackedCache<T extends Cacheable> extends Cache<T> {
    private Connection conn;
    private @Getter String table;

    public SQLBackedCache(long expireAfter, @NonNull Connection conn, @NonNull String table) throws IOException {
        super(expireAfter);
        this.table = table;
        this.conn = conn;

        try {
            this.conn
                .createStatement()
                .execute("CREATE TABLE IF NOT EXISTS " + this.table + " (id text PRIMARY KEY, className text NOT NULL, instance text NOT NULL, lastAccess bigint)");
        } catch (SQLException e) {
            throw new IOException(e);
        }
    }

    @SneakyThrows
    @Override
    public void submit(@NonNull T instanceObject) {
        this.evictExpiredItems();

        String id = instanceObject.id();
        String className = instanceObject.getClass().getCanonicalName();
        String instance = Rson.DEFAULT.toJson(instanceObject).toString(false);

        // Remove all entries with that ID.
        // Not all SQL servers support "INSERT OR REPLACE"
        try (PreparedStatement statement = this.conn.prepareStatement("DELETE FROM " + this.table + " WHERE id = ?")) {
            statement.setString(1, id);
            statement.executeUpdate();
        }

        // Insert the entry.
        try (PreparedStatement statement = this.conn.prepareStatement("INSERT INTO " + this.table + " (id, className, instance, lastAccess) VALUES(?, ?, ?, ?)")) {
            statement.setString(1, id);
            statement.setString(2, className);
            statement.setString(3, instance);
            statement.setLong(4, System.currentTimeMillis());
            statement.executeUpdate();
        }
    }

    @SneakyThrows
    @Override
    public boolean has(@NonNull String id) {
        this.evictExpiredItems();

        try (PreparedStatement statement = this.conn.prepareStatement("SELECT id FROM " + this.table + " WHERE id = ?")) {
            statement.setString(1, id);

            ResultSet result = statement.executeQuery();
            return result.next();
        }
    }

    @SneakyThrows
    @Override
    public CacheIterator<T> enumerate() {
        this.evictExpiredItems();

        PreparedStatement statement = this.conn.prepareStatement("SELECT id, className, instance FROM " + this.table + "");
        ResultSet result = statement.executeQuery();

        return new CacheIterator<T>() {

            @SneakyThrows
            @Override
            public boolean hasNext() {
                return result.next();
            }

            @SuppressWarnings("unchecked")
            @SneakyThrows
            @Override
            public T next() {
                String id = result.getString(1);
                String className = result.getString(2);
                String instance = result.getString(3);

                // Update the lastAccess time for this entry.
                try (PreparedStatement updateStatement = conn.prepareStatement("UPDATE " + table + " SET lastAccess = ? WHERE id = ?")) {
                    updateStatement.setLong(1, System.currentTimeMillis());
                    updateStatement.setString(2, id);
                    updateStatement.executeUpdate();
                }

                Class<?> clazz = Class.forName(className);

                return (T) Rson.DEFAULT.fromJson(instance, clazz);
            }

            @Override
            public void close() throws Exception {
                statement.close();
            }

        };
    }

    @SuppressWarnings("unchecked")
    @SneakyThrows
    @Override
    public @Nullable T get(@NonNull String id) {
        this.evictExpiredItems();

        try (PreparedStatement statement = this.conn.prepareStatement("SELECT className, instance FROM " + this.table + " WHERE id = ?")) {
            statement.setString(1, id);

            ResultSet result = statement.executeQuery();
            if (!result.next()) {
                return null; // No entry.
            }

            // Update the lastAccess time for this entry.
            try (PreparedStatement updateStatement = this.conn.prepareStatement("UPDATE " + this.table + " SET lastAccess = ? WHERE id = ?")) {
                updateStatement.setLong(1, System.currentTimeMillis());
                updateStatement.setString(2, id);
                updateStatement.executeUpdate();
            }

            String className = result.getString(1);
            String instance = result.getString(2);

            Class<?> clazz = Class.forName(className);
            return (T) Rson.DEFAULT.fromJson(instance, clazz);
        }
    }

    /**
     * Forcefully evicts expired entries. Has no effect if {@link #limit} is -1.
     */
    @SneakyThrows
    @Override
    public void evictExpiredItems() {
        if (this.expireAfter == -1) return;

        final long checkBefore = System.currentTimeMillis() - this.expireAfter;
        try (PreparedStatement statement = this.conn.prepareStatement("DELETE FROM " + this.table + " WHERE lastAccess < ?")) {
            statement.setLong(1, checkBefore);
            statement.executeUpdate();
        }
    }

}
