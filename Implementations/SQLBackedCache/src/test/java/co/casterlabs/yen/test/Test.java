package co.casterlabs.yen.test;

import co.casterlabs.yen.impl.SQLBackedCache;

public class Test {

    public static void main(String[] args) throws Exception {
        try (SQLBackedCache<ExampleItem> cache = new SQLBackedCache<>(-1, "jdbc:sqlite:test.db", "cache")) {
            cache.submit(new ExampleItem(1));
            cache.submit(new ExampleItem(2));
            cache.submit(new ExampleItem(3));

            System.out.println(cache.has("1"));
            System.out.println(cache.get("1"));
            System.out.println(cache.enumerate().toList());
        }
    }

}
