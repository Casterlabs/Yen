package co.casterlabs.yen.impl;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.jetbrains.annotations.Nullable;

import co.casterlabs.yen.Cache;
import co.casterlabs.yen.CacheIterator;
import co.casterlabs.yen.Cacheable;
import lombok.NonNull;

public class MemoryBackedCache<T extends Cacheable> extends Cache<T> {
    private final Map<String, CacheItem> cache = new HashMap<>();

    /**
     * A limit of -1 means infinite.
     */
    public final int limit;

    /**
     * Creates a MemoryBackedCache with the specified item limit. Infrequently
     * accessed items will be evicted from the cache first. Use the other
     * constructor to create a cache with no item limit.
     * 
     * @param    expireAfter    the time after insertion to mark an entry as
     *                          "expired."
     * @param    limit          the maxmium amount of items to store before evicting
     *                          old entries. -1 to disable.
     * 
     * @implNote                Expired items may not be evicted until they are
     *                          retrieved or until an item is inserted. You can use
     *                          {@link #evictExpiredItems()} to trigger eviction
     *                          manually.
     * 
     * @throws   AssertionError if limit is less than or equal to zero AND is not
     *                          -1.
     */
    public MemoryBackedCache(long expireAfter, int limit) {
        super(expireAfter);
        this.limit = limit;
        assert expireAfter > 0 || expireAfter == -1 : "Expire time must be greater than 0 OR equal to -1 to disable.";
        assert limit > 0 || limit == -1 : "Limit must be greater than 0 OR equal to -1 to disable.";
    }

    @Override
    public void submit(@NonNull T instance) {
        this.evictExpiredItems();

        synchronized (this.cache) {
            if ((this.limit != -1) &&
                (this.cache.size() >= this.limit)) {
                Optional<CacheItem> toRemove = this.cache.values()
                    .parallelStream()
                    .sorted((ci1, ci2) -> {
                        return Long.compare(ci1.lastAccess, ci2.lastAccess);
                    })
                    .findFirst();
                this.cache.remove(toRemove.get().id);
            }

            String id = instance.id();
            this.cache.put(id, new CacheItem(id, instance, System.currentTimeMillis()));
        }
    }

    @Override
    public boolean has(@NonNull String id) {
        this.evictExpiredItems();
        return this.cache.containsKey(id);
    }

    @Override
    public CacheIterator<T> enumerate() {
        this.evictExpiredItems();
        Iterator<MemoryBackedCache<T>.CacheItem> nativeIt = this.cache.values().iterator();
        return new CacheIterator<T>() {

            @Override
            public boolean hasNext() {
                return nativeIt.hasNext();
            }

            @Override
            public T next() {
                CacheItem item = nativeIt.next();

                // If we have an item and we have a limit, update the lastAccess time.
                if ((item != null) && (limit != -1)) {
                    item.lastAccess = System.currentTimeMillis();
                }

                return item.instance;
            }

            @Override
            public void close() throws Exception {} // NOOP

        };
    }

    @Override
    public synchronized @Nullable T get(@NonNull String id) {
        this.evictExpiredItems();

        CacheItem item = this.cache.get(id);

        // If we have an item and we have a limit, update the lastAccess time.
        if ((item != null) && (this.limit != -1)) {
            item.lastAccess = System.currentTimeMillis();
        }

        return item.instance;
    }

    /**
     * Forcefully evicts expired entries. Has no effect if {@link #limit} is -1.
     */
    @Override
    public void evictExpiredItems() {
        if (this.expireAfter == -1) return;

        synchronized (this.cache) {
            final long checkBefore = System.currentTimeMillis() - this.expireAfter;

            List<String> toRemove = this.cache.values()
                .parallelStream()
                .filter((ci) -> ci.lastAccess < checkBefore)
                .map((ci) -> ci.id)
                .collect(Collectors.toList());
            toRemove.forEach(this.cache::remove);
        }
    }

    private class CacheItem {
        private final String id;
        private final T instance;
        private long lastAccess;

        private CacheItem(String id, T instance, long time) {
            this.id = id;
            this.instance = instance;
            this.lastAccess = time;
        }

    }

}
