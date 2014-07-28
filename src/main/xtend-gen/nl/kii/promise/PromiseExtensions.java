package nl.kii.promise;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import nl.kii.promise.Promise;
import nl.kii.promise.PromiseFuture;
import nl.kii.promise.Task;
import nl.kii.stream.Entry;
import org.eclipse.xtext.xbase.lib.Conversions;
import org.eclipse.xtext.xbase.lib.Exceptions;
import org.eclipse.xtext.xbase.lib.Functions.Function1;
import org.eclipse.xtext.xbase.lib.IterableExtensions;
import org.eclipse.xtext.xbase.lib.Pair;
import org.eclipse.xtext.xbase.lib.Procedures.Procedure1;

@SuppressWarnings("all")
public class PromiseExtensions {
  /**
   * Create a promise of the given type
   */
  public static <T extends Object> Promise<T> promise(final Class<T> type) {
    return new Promise<T>();
  }
  
  /**
   * Create a promise of a list of the given type
   */
  public static <T extends Object> Promise<List<T>> promiseList(final Class<T> type) {
    return new Promise<List<T>>();
  }
  
  /**
   * Create a promise of a map of the given key and value types
   */
  public static <K extends Object, V extends Object> Promise<Map<K, V>> promiseMap(final Pair<Class<K>, Class<V>> type) {
    return new Promise<Map<K, V>>();
  }
  
  /**
   * Create a promise that immediately resolves to the passed value
   */
  public static <T extends Object> Promise<T> promise(final T value) {
    return new Promise<T>(value);
  }
  
  /**
   * When the promise gives a result, call the function that returns another promise and
   * return that promise so you can chain and continue. Any thrown errors will be caught
   * and passed down the chain so you can catch them at the bottom.
   * <p>
   * Example:
   * <pre>
   * loadUser
   *   .then [ checkCredentialsAsync ]
   *   .then [ signinUser ]
   *   .onError [ setErrorMessage('could not sign you in') ]
   *   .then [ println('success!') ]
   * </pre>
   */
  public static <T extends Object, R extends Object, P extends Promise<R>> Promise<R> thenAsync(final Promise<T> promise, final Function1<? super T, ? extends P> promiseFn) {
    final Function1<T, P> _function = new Function1<T, P>() {
      public P apply(final T it) {
        return promiseFn.apply(it);
      }
    };
    Promise<P> _map = PromiseExtensions.<T, P>map(promise, _function);
    return PromiseExtensions.<R>flatten(_map);
  }
  
  /**
   * Tell the task it went wrong
   */
  public static Task error(final Task task, final String message) {
    Exception _exception = new Exception(message);
    Promise<Boolean> _error = task.error(_exception);
    return ((Task) _error);
  }
  
  /**
   * Tell the promise it went wrong
   */
  public static <T extends Object> Promise<T> error(final Promise<T> promise, final String message) {
    Exception _exception = new Exception(message);
    return promise.error(_exception);
  }
  
  /**
   * Fulfill a promise
   */
  public static <T extends Object> Promise<T> operator_doubleGreaterThan(final T value, final Promise<T> promise) {
    Promise<T> _xblockexpression = null;
    {
      promise.set(value);
      _xblockexpression = promise;
    }
    return _xblockexpression;
  }
  
  /**
   * Fulfill a promise
   */
  public static <T extends Object> Promise<T> operator_doubleLessThan(final Promise<T> promise, final T value) {
    Promise<T> _xblockexpression = null;
    {
      promise.set(value);
      _xblockexpression = promise;
    }
    return _xblockexpression;
  }
  
  /**
   * Create a new promise from an existing promise,
   * that transforms the value of the promise
   * once the existing promise is resolved.
   */
  public static <T extends Object, R extends Object> Promise<R> map(final Promise<T> promise, final Function1<? super T, ? extends R> mappingFn) {
    Promise<R> _xblockexpression = null;
    {
      final Promise<R> newPromise = new Promise<R>(promise);
      final Procedure1<T> _function = new Procedure1<T>() {
        public void apply(final T it) {
          R _apply = mappingFn.apply(it);
          newPromise.set(_apply);
        }
      };
      promise.then(_function);
      _xblockexpression = newPromise;
    }
    return _xblockexpression;
  }
  
  /**
   * Flattens a promise of a promise to directly a promise.
   */
  public static <T extends Object> Promise<T> flatten(final Promise<? extends Promise<T>> promise) {
    Promise<T> _xblockexpression = null;
    {
      final Promise<T> newPromise = new Promise<T>(promise);
      final Procedure1<Promise<T>> _function = new Procedure1<Promise<T>>() {
        public void apply(final Promise<T> it) {
          final Procedure1<Throwable> _function = new Procedure1<Throwable>() {
            public void apply(final Throwable it) {
              newPromise.error(it);
            }
          };
          Promise<T> _onError = it.onError(_function);
          final Procedure1<T> _function_1 = new Procedure1<T>() {
            public void apply(final T it) {
              newPromise.set(it);
            }
          };
          _onError.then(_function_1);
        }
      };
      promise.then(_function);
      _xblockexpression = newPromise;
    }
    return _xblockexpression;
  }
  
  /**
   * Fork a single promise into a list of promises
   * Note that the original promise is then being listened to and you
   * can no longer perform .then and .onError on it.
   */
  public static <T extends Object> Promise<T>[] fork(final Promise<T> promise, final int amount) {
    Promise<T>[] _xblockexpression = null;
    {
      final Promise<T>[] promises = new Promise[amount];
      final Procedure1<Throwable> _function = new Procedure1<Throwable>() {
        public void apply(final Throwable t) {
          final Procedure1<Promise<T>> _function = new Procedure1<Promise<T>>() {
            public void apply(final Promise<T> p) {
              p.error(t);
            }
          };
          IterableExtensions.<Promise<T>>forEach(((Iterable<Promise<T>>)Conversions.doWrapArray(promises)), _function);
        }
      };
      Promise<T> _onError = promise.onError(_function);
      final Procedure1<T> _function_1 = new Procedure1<T>() {
        public void apply(final T value) {
          final Procedure1<Promise<T>> _function = new Procedure1<Promise<T>>() {
            public void apply(final Promise<T> p) {
              p.set(value);
            }
          };
          IterableExtensions.<Promise<T>>forEach(((Iterable<Promise<T>>)Conversions.doWrapArray(promises)), _function);
        }
      };
      _onError.then(_function_1);
      _xblockexpression = promises;
    }
    return _xblockexpression;
  }
  
  /**
   * Forward the events from this promise to another promise of the same type
   */
  public static <T extends Object> void forwardTo(final Promise<T> promise, final Promise<T> existingPromise) {
    final Procedure1<Entry<T>> _function = new Procedure1<Entry<T>>() {
      public void apply(final Entry<T> it) {
        existingPromise.apply(it);
      }
    };
    Promise<T> _always = promise.always(_function);
    final Procedure1<T> _function_1 = new Procedure1<T>() {
      public void apply(final T it) {
      }
    };
    _always.then(_function_1);
  }
  
  /**
   * Convert a promise into a Future.
   * Promises are non-blocking. However you can convert to a Future
   * if you must block and wait for a promise to resolve.
   * <pre>
   * val result = promise.future.get // blocks code until the promise is fulfilled
   */
  public static <T extends Object> Future<T> future(final Promise<T> promise) {
    return new PromiseFuture<T>(promise);
  }
  
  /**
   * Execute the callable in the background and return as a promise.
   * Lets you specify the executorservice to run on.
   * <pre>
   * val service = Executors.newSingleThreadExecutor
   * service.promise [| return doSomeHeavyLifting ].then [ println('result:' + it) ]
   */
  public static <T extends Object> Promise<T> async(final ExecutorService service, final Callable<T> callable) {
    Promise<T> _xblockexpression = null;
    {
      final Promise<T> promise = new Promise<T>();
      final Runnable _function = new Runnable() {
        public void run() {
          try {
            final T result = callable.call();
            promise.set(result);
          } catch (final Throwable _t) {
            if (_t instanceof Throwable) {
              final Throwable t = (Throwable)_t;
              promise.error(t);
            } else {
              throw Exceptions.sneakyThrow(_t);
            }
          }
        }
      };
      final Runnable processor = _function;
      service.submit(processor);
      _xblockexpression = promise;
    }
    return _xblockexpression;
  }
  
  /**
   * Execute the runnable in the background and return as a promise.
   * Lets you specify the executorservice to run on.
   * <pre>
   * val service = Executors.newSingleThreadExecutor
   * service.promise [| doSomeHeavyLifting ].then [ println('done!') ]
   */
  public static Task run(final ExecutorService service, final Runnable runnable) {
    Task _xblockexpression = null;
    {
      final Task task = new Task();
      final Runnable _function = new Runnable() {
        public void run() {
          try {
            runnable.run();
            task.complete();
          } catch (final Throwable _t) {
            if (_t instanceof Throwable) {
              final Throwable t = (Throwable)_t;
              task.error(t);
            } else {
              throw Exceptions.sneakyThrow(_t);
            }
          }
        }
      };
      final Runnable processor = _function;
      service.submit(processor);
      _xblockexpression = task;
    }
    return _xblockexpression;
  }
}
