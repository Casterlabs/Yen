package co.casterlabs.yen;

import java.util.function.Function;

import org.jetbrains.annotations.Nullable;

import lombok.NonNull;

public abstract class Cache<T extends Cacheable> {
    /**
     * A value of -1 means no expiry.
     */
    public final long expireAfter;

    static {
        Cache.class.getClassLoader().setPackageAssertionStatus("co.casterlabs.yen", true);
    }

    public Cache(long expireAfter) {
        assert expireAfter > 0 || expireAfter == -1 : "Expire time must be greater than 0 OR equal to -1 to disable.";
        this.expireAfter = expireAfter;
    }

    /**
     * Submits an object to the cache for later retrieval.
     * 
     * @param    instance the instance to add to the cache.
     * 
     * @implNote          Submitted instances should overwrite existing cached items
     *                    if there is an ID conflict.
     */
    public abstract void submit(@NonNull T instance);

    /**
     * Checks the cache for an entry with the given ID.
     * 
     * @param id the ID of the object to check for.
     */
    public abstract boolean has(@NonNull String id);

    /**
     * Retrieves an iterator for all contained items.
     * 
     * You must close the returned iterator.
     * 
     * @return an iterator.
     */
    public abstract CacheIterator<T> enumerate();

    /**
     * Retrieves an instance with the given ID.
     * 
     * @param  id the ID of the object to retrieve.
     * 
     * @return    the object, or null if the object doesn't exist.
     */
    public abstract @Nullable T get(@NonNull String id);

    /**
     * Retrieves an instance with the given ID, executing the provider function if
     * there is no entry. Note that the provided item will be automatically
     * submitted for you.
     * 
     * @param  id       the ID of the object to retrieve.
     * @param  provider the {@link Function} to execute if there is no item in the
     *                  cache with that ID. Note that this function <b>can</b>
     *                  return null to indicate an error.
     * 
     * @return          the object, or a fresh object provided by the provider
     *                  function, or null if the provider failed.
     */
    public final @Nullable T getOrProvide(@NonNull String id, @NonNull Function<String, T> provider) {
        T result = this.get(id);

        if (result == null) {
            result = provider.apply(id);
            if (result != null) {
                this.submit(result);
            }
        }

        return result;
    }

    /**
     * Removes the instance with the given ID.
     * 
     * @param id the ID of the object to remove.
     */
    public abstract void remove(@NonNull String id);

    /**
     * Forcefully evicts expired entries. Has no effect if {@link #limit} is -1.
     */
    public abstract void evictExpiredItems();

}
