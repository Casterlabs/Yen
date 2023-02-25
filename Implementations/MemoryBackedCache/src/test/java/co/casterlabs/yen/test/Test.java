package co.casterlabs.yen.test;

import java.util.List;

import co.casterlabs.yen.impl.MemoryBackedCache;

public class Test {

    public static void main(String[] args) {
        MemoryBackedCache<ExampleItem> cache = new MemoryBackedCache<>(-1, 4);

        cache.submit(new ExampleItem(1));
        cache.submit(new ExampleItem(2));
        cache.submit(new ExampleItem(3));
        cache.submit(new ExampleItem(4));
        cache.submit(new ExampleItem(5));

        List<ExampleItem> contents = cache.dumpCache();
        System.out.println("Cache should not contain item 1, as the cache has a limit of 4 items and we've added 5.");
        System.out.printf("Cache contents: %s\n", contents);
    }

}
