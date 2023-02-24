package co.casterlabs.yen.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.jetbrains.annotations.Nullable;

import co.casterlabs.yen.Cache;
import co.casterlabs.yen.Cacheable;
import lombok.NonNull;

public class MemoryBackedCache<T extends Cacheable> extends Cache<T> {
    private final Map<String, CacheItem> cache = new HashMap<>();

    /**
     * A limit of -1 means infinite.
     */
    public final int limit;

    /**
     * Creates a MemoryBackedCache with no item limit.
     * 
     * @see #MemoryBackedCache(int)
     */
    public MemoryBackedCache() {
        this.limit = -1;
    }

    /**
     * Creates a MemoryBackedCache with the specified item limit. Infrequently
     * accessed items will be evicted from the cache first. Use the other
     * constructor to create a cache with no item limit.
     * 
     * @param  limit          the maxmium amount of items to store before evicting
     *                        old entries.
     * 
     * @throws AssertionError if limit is less than or equal to zero.
     * 
     * @see                   #MemoryBackedCache()
     */
    public MemoryBackedCache(int limit) {
        assert limit > 0 : "Limit must be greater than 0.";
        this.limit = limit;
    }

    @Override
    public void submit(@NonNull T instance) {
        if (this.limit == -1) {
            this.cache.put(
                instance.id(),
                new CacheItem(instance, -1) // No timestamp.
            );
            return;
        }

        synchronized (this.cache) {
            if (this.cache.size() >= this.limit) {
                Optional<Map.Entry<String, CacheItem>> toRemove = this.cache.entrySet()
                    .parallelStream()
                    .sorted((ci1, ci2) -> {
                        return Long.compare(ci1.getValue().lastAccess, ci2.getValue().lastAccess);
                    })
                    .findFirst();
                this.cache.remove(toRemove.get().getKey());
            }

            this.cache.put(
                instance.id(),
                new CacheItem(instance, System.currentTimeMillis())
            );
        }
    }

    @Override
    public boolean has(@NonNull String id) {
        return this.cache.containsKey(id);
    }

    @Override
    public synchronized @Nullable T get(@NonNull String id) {
        CacheItem item = this.cache.get(id);

        // If we have an item and we have a limit, update the lastAccess time.
        if ((item != null) && (this.limit != -1)) {
            item.lastAccess = System.currentTimeMillis();
        }

        return item.instance;
    }

    /**
     * Dumps the entire cache into a list and returns it.
     * 
     * @return all of the items currently in the cache.
     */
    public List<T> dumpCache() {
        synchronized (this.cache) {
            return this.cache.values()
                .parallelStream()
                .map((ci) -> ci.instance)
                .collect(Collectors.toList());
        }
    }

    private class CacheItem {
        private final T instance;
        private long lastAccess;

        private CacheItem(T instance, long time) {
            this.instance = instance;
            this.lastAccess = time;
        }
    }

}
