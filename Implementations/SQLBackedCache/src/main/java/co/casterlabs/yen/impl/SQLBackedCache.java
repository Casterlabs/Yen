package co.casterlabs.yen.impl;

import java.io.Closeable;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.jetbrains.annotations.Nullable;

import co.casterlabs.rakurai.json.Rson;
import co.casterlabs.yen.Cache;
import co.casterlabs.yen.Cacheable;
import lombok.NonNull;
import lombok.SneakyThrows;

public class SQLBackedCache<T extends Cacheable> extends Cache<T> implements Closeable {
    private Connection conn;

    public SQLBackedCache(String url) throws IOException {
        try {
            this.conn = DriverManager.getConnection(url);

            this.conn
                .createStatement()
                .execute("CREATE TABLE IF NOT EXISTS cache (id text PRIMARY KEY, className text NOT NULL, instance text NOT NULL)");
        } catch (SQLException e) {
            throw new IOException(e);
        }
    }

    @SneakyThrows
    @Override
    public void submit(@NonNull T instanceObject) {
        String id = instanceObject.id();
        String className = instanceObject.getClass().getCanonicalName();
        String instance = Rson.DEFAULT.toJson(instanceObject).toString(false);

        // Remove all entries with that ID.
        // Not all SQL servers support "INSERT OR REPLACE"
        try (PreparedStatement statement = this.conn.prepareStatement("DELETE FROM cache WHERE id = ?")) {
            statement.setString(1, id);
            statement.executeUpdate();
        }

        // Insert the entry.
        try (PreparedStatement statement = this.conn.prepareStatement("INSERT INTO cache (id, className, instance) VALUES(?, ?, ?)")) {
            statement.setString(1, id);
            statement.setString(2, className);
            statement.setString(3, instance);
            statement.executeUpdate();
        }
    }

    @SneakyThrows
    @Override
    public boolean has(@NonNull String id) {
        try (PreparedStatement statement = this.conn.prepareStatement("SELECT id FROM cache WHERE id = ?")) {
            statement.setString(1, id);

            ResultSet result = statement.executeQuery();
            return result.next();
        }
    }

    @SuppressWarnings("unchecked")
    @SneakyThrows
    @Override
    public @Nullable T get(@NonNull String id) {
        try (PreparedStatement statement = this.conn.prepareStatement("SELECT className, instance FROM cache WHERE id = ?")) {
            statement.setString(1, id);

            ResultSet result = statement.executeQuery();
            if (!result.next()) {
                return null; // No entry.
            }

            String className = result.getString(1);
            String instance = result.getString(2);

            Class<?> clazz = Class.forName(className);
            return (T) Rson.DEFAULT.fromJson(instance, clazz);
        }
    }

    @Override
    public void close() throws IOException {
        try {
            this.conn.close();
        } catch (SQLException e) {
            throw new IOException(e);
        }
    }

}
