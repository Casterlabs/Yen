package co.casterlabs.yen;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;

import lombok.SneakyThrows;

public interface CacheIterator<T> extends Iterator<T>, AutoCloseable {

    /**
     * This method closes the iterator.
     * 
     * @return a list with all of the items filled.
     */
    @SneakyThrows
    default List<T> toList() {
        List<T> contents = new ArrayList<>();
        this.forEachRemaining(contents::add);
        this.close();
        return contents;
    }

    /**
     * Performs the given action for each remaining element until all elements have
     * been processed or the action throws an exception. Actions are performed in
     * the order of iteration, if that order is specified. Exceptions thrown by the
     * action are relayed to the caller.
     *
     * @implSpec
     *                                <p>
     *                                The default implementation behaves as if:
     * 
     *                                <pre>{@code
     *     while (hasNext())
     *         action.accept(next());
     * }</pre>
     *
     * @param    action               The action to be performed for each element
     * 
     * @throws   NullPointerException if the specified action is null
     * 
     * @since                         1.8
     */
    @SneakyThrows
    @Override
    default void forEachRemaining(Consumer<? super T> action) {
        try {
            Iterator.super.forEachRemaining(action);
        } finally {
            this.close();
        }
    }

}
