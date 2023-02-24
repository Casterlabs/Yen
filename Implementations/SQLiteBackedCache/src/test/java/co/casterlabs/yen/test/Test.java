package co.casterlabs.yen.test;

import java.io.IOException;

import co.casterlabs.yen.impl.SQLiteBackedCache;

public class Test {

    public static void main(String[] args) throws IOException {
        try (SQLiteBackedCache<ExampleItem> cache = new SQLiteBackedCache<>("jdbc:sqlite:test.db")) {
            cache.submit(new ExampleItem(1));
            cache.submit(new ExampleItem(2));
            cache.submit(new ExampleItem(3));
        }
    }

}
