package nl.kii.async;

import nl.kii.promise.Promise;
import nl.kii.promise.Task;

/**
 * An asynchronous version of a Java Map.
 * <p>
 * This means that all operations are non-blocking, and instead of returning void and values,
 * they return Tasks and Promises. These can be listened to for the result of the operation,
 * or to catch any thrown exceptions.
 * <p>
 * Async maps are especially useful representing networked operations, since it allows
 * for slower operations to not block the code and to have a mechanism to catch exceptions.
 */
@SuppressWarnings("all")
public interface AsyncMap<K extends Object, V extends Object> {
  public abstract Task put(final K key, final V value);
  
  public abstract Promise<V> get(final K key);
  
  public abstract Task remove(final K key);
  
  public abstract Task clear();
  
  public abstract Promise<Boolean> isEmpty();
}
