package co.casterlabs.yen;

import java.util.function.Function;

import org.jetbrains.annotations.Nullable;

import lombok.NonNull;

public abstract class Cache<T extends Cacheable> {

    static {
        Cache.class.getClassLoader().setPackageAssertionStatus("co.casterlabs.yen", true);
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

}
