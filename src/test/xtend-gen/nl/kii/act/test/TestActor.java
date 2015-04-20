package nl.kii.act.test;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import nl.kii.act.Actor;
import nl.kii.act.ActorExtensions;
import nl.kii.async.ExecutorExtensions;
import nl.kii.async.annotation.Async;
import nl.kii.async.annotation.Atomic;
import nl.kii.promise.IPromise;
import nl.kii.promise.PromiseExtensions;
import nl.kii.promise.Task;
import nl.kii.promise.internal.SubPromise;
import nl.kii.stream.Stream;
import nl.kii.stream.StreamExtensions;
import nl.kii.stream.internal.SubStream;
import org.eclipse.xtext.xbase.lib.Exceptions;
import org.eclipse.xtext.xbase.lib.Functions.Function1;
import org.eclipse.xtext.xbase.lib.InputOutput;
import org.eclipse.xtext.xbase.lib.IntegerRange;
import org.eclipse.xtext.xbase.lib.IterableExtensions;
import org.eclipse.xtext.xbase.lib.Procedures.Procedure0;
import org.eclipse.xtext.xbase.lib.Procedures.Procedure1;
import org.eclipse.xtext.xbase.lib.Procedures.Procedure2;
import org.junit.Assert;
import org.junit.Test;

@SuppressWarnings("all")
public class TestActor {
  @Test
  public void testHelloWorld() {
    final Procedure2<String, Procedure0> _function = (String it, Procedure0 done) -> {
      InputOutput.<String>println(("hello " + it));
      done.apply();
    };
    final Actor<String> greeter = ActorExtensions.<String>actor(_function);
    ActorExtensions.<String>operator_doubleGreaterThan(
      "world", greeter);
    ActorExtensions.<String>operator_doubleGreaterThan(
      "Christian!", greeter);
    ActorExtensions.<String>operator_doubleGreaterThan(
      "time to go!", greeter);
  }
  
  @Atomic
  private final AtomicInteger _access = new AtomicInteger();
  
  @Atomic
  private final AtomicInteger _value = new AtomicInteger();
  
  @Atomic
  private final AtomicInteger _multipleThreadAccessViolation = new AtomicInteger();
  
  @Test
  public void testActorsAreSingleThreaded() {
    try {
      final Actor<Integer> actor = new Actor<Integer>() {
        @Override
        protected void act(final Integer message, final Procedure0 done) {
          final Integer a = TestActor.this.incAccess();
          if (((a).intValue() > 1)) {
            TestActor.this.incMultipleThreadAccessViolation();
          }
          Integer _value = TestActor.this.getValue();
          int _plus = ((_value).intValue() + 1);
          TestActor.this.setValue(Integer.valueOf(_plus));
          TestActor.this.decAccess();
          done.apply();
        }
      };
      final ExecutorService threads = Executors.newCachedThreadPool();
      final Runnable _function = () -> {
        IntegerRange _upTo = new IntegerRange(1, 1000);
        for (final Integer i : _upTo) {
          actor.apply(i);
        }
      };
      ExecutorExtensions.task(threads, _function);
      final Runnable _function_1 = () -> {
        IntegerRange _upTo = new IntegerRange(1, 1000);
        for (final Integer i : _upTo) {
          actor.apply(i);
        }
      };
      ExecutorExtensions.task(threads, _function_1);
      final Runnable _function_2 = () -> {
        IntegerRange _upTo = new IntegerRange(1, 1000);
        for (final Integer i : _upTo) {
          actor.apply(i);
        }
      };
      ExecutorExtensions.task(threads, _function_2);
      final Runnable _function_3 = () -> {
        IntegerRange _upTo = new IntegerRange(1, 1000);
        for (final Integer i : _upTo) {
          actor.apply(i);
        }
      };
      ExecutorExtensions.task(threads, _function_3);
      Thread.sleep(2000);
      Integer _multipleThreadAccessViolation = this.getMultipleThreadAccessViolation();
      Assert.assertEquals(0, (_multipleThreadAccessViolation).intValue());
      Integer _value = this.getValue();
      Assert.assertEquals(4000, (_value).intValue());
    } catch (Throwable _e) {
      throw Exceptions.sneakyThrow(_e);
    }
  }
  
  @Atomic
  private final AtomicReference<Actor<Integer>> _decreaser = new AtomicReference<Actor<Integer>>();
  
  @Test
  public void testAsyncCrosscallingActors() {
    try {
      final AtomicInteger doneCounter = new AtomicInteger(0);
      final ExecutorService threads = Executors.newCachedThreadPool();
      final Procedure1<Integer> _function = (Integer y) -> {
        final Runnable _function_1 = () -> {
          try {
            Thread.sleep(5);
            if ((y <= 0)) {
              doneCounter.incrementAndGet();
            } else {
              Actor<Integer> _decreaser = this.getDecreaser();
              ActorExtensions.<Integer>operator_doubleGreaterThan(Integer.valueOf(y), _decreaser);
            }
          } catch (Throwable _e) {
            throw Exceptions.sneakyThrow(_e);
          }
        };
        ExecutorExtensions.task(threads, _function_1);
      };
      final Actor<Integer> checkDone = ActorExtensions.<Integer>actor(_function);
      final Procedure1<Integer> _function_1 = (Integer value) -> {
        final Runnable _function_2 = () -> {
          ActorExtensions.<Integer>operator_doubleGreaterThan(Integer.valueOf((value - 1)), checkDone);
        };
        ExecutorExtensions.task(threads, _function_2);
      };
      Actor<Integer> _actor = ActorExtensions.<Integer>actor(_function_1);
      this.setDecreaser(_actor);
      ActorExtensions.<Integer>operator_doubleLessThan(checkDone, Integer.valueOf(100));
      ActorExtensions.<Integer>operator_doubleLessThan(checkDone, Integer.valueOf(300));
      ActorExtensions.<Integer>operator_doubleLessThan(checkDone, Integer.valueOf(200));
      Thread.sleep(2000);
      int _get = doneCounter.get();
      Assert.assertEquals(3, _get);
    } catch (Throwable _e) {
      throw Exceptions.sneakyThrow(_e);
    }
  }
  
  @Atomic
  private final AtomicInteger _actorCounter = new AtomicInteger(0);
  
  @Atomic
  private final AtomicInteger _functCounter = new AtomicInteger(0);
  
  @Atomic
  private final AtomicInteger _unsyncedCounter = new AtomicInteger(0);
  
  @Atomic
  private final AtomicInteger _syncedCounter = new AtomicInteger(0);
  
  public Integer unsynced() {
    return this.incSyncedCounter();
  }
  
  public synchronized Integer synced() {
    return this.incSyncedCounter();
  }
  
  /**
   * Test of actor versus function calls, method calls and synchronized method calls,
   * under a single threaded load.
   * <p>
   * Synchronized calls seem to be about twice as slow as functions and unsynced methods.
   * Actors are about 3x as slow as synchronized methods.
   */
  @Test
  public void testActorRelativeSingleThreadedPerformance() {
    final IntegerRange iterations = new IntegerRange(1, 10_000_000);
    final Function1<Integer, Integer> _function = (Integer it) -> {
      return this.incFunctCounter();
    };
    final Function1<Integer, Integer> funct = _function;
    final Procedure1<Integer> _function_1 = (Integer it) -> {
      this.incActorCounter();
    };
    final Actor<Integer> actor = ActorExtensions.<Integer>actor(_function_1);
    IntegerRange _upTo = new IntegerRange(1, 20_000_000);
    for (final Integer i : _upTo) {
      actor.apply(i);
    }
    final Procedure0 _function_2 = () -> {
      for (final Integer i_1 : iterations) {
        funct.apply(i_1);
      }
    };
    final long functTimeMs = this.measure(_function_2);
    InputOutput.<String>println(("function took: " + Long.valueOf(functTimeMs)));
    final Procedure0 _function_3 = () -> {
      for (final Integer i_1 : iterations) {
        this.unsynced();
      }
    };
    final long unsyncedTimeMs = this.measure(_function_3);
    InputOutput.<String>println(("unsynced method took: " + Long.valueOf(unsyncedTimeMs)));
    final Procedure0 _function_4 = () -> {
      for (final Integer i_1 : iterations) {
        this.synced();
      }
    };
    final long syncedTimeMs = this.measure(_function_4);
    InputOutput.<String>println(("synced method took: " + Long.valueOf(syncedTimeMs)));
    final Procedure0 _function_5 = () -> {
      for (final Integer i_1 : iterations) {
        actor.apply(i_1);
      }
    };
    final long actorTimeMs = this.measure(_function_5);
    InputOutput.<String>println(("actor took: " + Long.valueOf(actorTimeMs)));
  }
  
  /**
   * Test of actor versus function calls, method calls and synchronized method calls,
   * under a multithreaded load.
   * <p>
   * Synchronized calls seem to be about twice as slow as functions and unsynced methods.
   * Actors are about 3x as slow as synchronized methods.
   * <p>
   * Interestingly this is about the same as under singlethreaded load.
   */
  @Test
  public void testActorRelativeMultiThreadedPerformance() {
    try {
      final Function1<Integer, Integer> _function = (Integer it) -> {
        return this.incFunctCounter();
      };
      final Function1<Integer, Integer> funct = _function;
      final Procedure1<Integer> _function_1 = (Integer it) -> {
        this.incActorCounter();
      };
      final Actor<Integer> actor = ActorExtensions.<Integer>actor(_function_1);
      IntegerRange _upTo = new IntegerRange(1, 20_000_000);
      for (final Integer i : _upTo) {
        actor.apply(i);
      }
      final IntegerRange iterations = new IntegerRange(1, 1_000_000);
      final int threads = 10;
      Task _complete = PromiseExtensions.complete();
      final Function1<Boolean, SubPromise<Integer, Long>> _function_2 = (Boolean it) -> {
        final Procedure0 _function_3 = () -> {
          for (final Integer i_1 : iterations) {
            funct.apply(i_1);
          }
        };
        return this.measure(threads, _function_3);
      };
      SubPromise<Boolean, Long> _call = PromiseExtensions.<Boolean, Boolean, Long, SubPromise<Integer, Long>>call(_complete, _function_2);
      final Procedure1<Long> _function_3 = (Long it) -> {
        InputOutput.<String>println(("function took: " + it));
      };
      Task _then = _call.then(_function_3);
      final Function1<Boolean, SubPromise<Integer, Long>> _function_4 = (Boolean it) -> {
        final Procedure0 _function_5 = () -> {
          for (final Integer i_1 : iterations) {
            this.unsynced();
          }
        };
        return this.measure(threads, _function_5);
      };
      SubPromise<Boolean, Long> _call_1 = PromiseExtensions.<Boolean, Boolean, Long, SubPromise<Integer, Long>>call(_then, _function_4);
      final Procedure1<Long> _function_5 = (Long it) -> {
        InputOutput.<String>println(("unsynced method took: " + it));
      };
      Task _then_1 = _call_1.then(_function_5);
      final Function1<Boolean, SubPromise<Integer, Long>> _function_6 = (Boolean it) -> {
        final Procedure0 _function_7 = () -> {
          for (final Integer i_1 : iterations) {
            this.synced();
          }
        };
        return this.measure(threads, _function_7);
      };
      SubPromise<Boolean, Long> _call_2 = PromiseExtensions.<Boolean, Boolean, Long, SubPromise<Integer, Long>>call(_then_1, _function_6);
      final Procedure1<Long> _function_7 = (Long it) -> {
        InputOutput.<String>println(("synced method took: " + it));
      };
      Task _then_2 = _call_2.then(_function_7);
      final Function1<Boolean, SubPromise<Integer, Long>> _function_8 = (Boolean it) -> {
        final Procedure0 _function_9 = () -> {
          for (final Integer i_1 : iterations) {
            actor.apply(i_1);
          }
        };
        return this.measure(threads, _function_9);
      };
      SubPromise<Boolean, Long> _call_3 = PromiseExtensions.<Boolean, Boolean, Long, SubPromise<Integer, Long>>call(_then_2, _function_8);
      final Procedure1<Long> _function_9 = (Long it) -> {
        InputOutput.<String>println(("actor took: " + it));
      };
      Task _then_3 = _call_3.then(_function_9);
      Future<Boolean> _future = ExecutorExtensions.<Boolean, Boolean>future(_then_3);
      _future.get();
    } catch (Throwable _e) {
      throw Exceptions.sneakyThrow(_e);
    }
  }
  
  /**
   * List processing vs Stream processing.
   * <p>
   * In this example, in small lists, the difference is performance is about a factor 2.
   * <p>
   * When the lists grow in size and are constantly pushed, it seems the GC can't keep
   * up and the difference grows to a factor 100 for lists of 100_000 items (processed a 1000 times).
   * <p>
   * In normal use cases the stream/list processing will not be the heaviest operation, but this
   * does mean there is room for optimisation.
   */
  @Test
  public void testStreamRelativeSingleThreadedPerformance() {
    final int iterations = 1000;
    final int listSize = 1000;
    final IntegerRange list = new IntegerRange(1, listSize);
    final Procedure1<Integer> _function = (Integer it) -> {
      final Function1<Integer, Integer> _function_1 = (Integer it_1) -> {
        return Integer.valueOf(((it_1).intValue() + 1000));
      };
      Iterable<Integer> _map = IterableExtensions.<Integer, Integer>map(list, _function_1);
      final Function1<Integer, Boolean> _function_2 = (Integer it_1) -> {
        return Boolean.valueOf((((it_1).intValue() % 2) == 0));
      };
      Iterable<Integer> _filter = IterableExtensions.<Integer>filter(_map, _function_2);
      final Consumer<Integer> _function_3 = (Integer it_1) -> {
        this.incFunctCounter();
      };
      _filter.forEach(_function_3);
    };
    final Procedure1<Integer> funct = _function;
    final Procedure1<Integer> _function_1 = (Integer it) -> {
      Stream<Integer> _stream = StreamExtensions.<Integer>stream(list);
      final Function1<Integer, Integer> _function_2 = (Integer it_1) -> {
        return Integer.valueOf(((it_1).intValue() + 1000));
      };
      SubStream<Integer, Integer> _map = StreamExtensions.<Integer, Integer, Integer>map(_stream, _function_2);
      final Function1<Integer, Boolean> _function_3 = (Integer it_1) -> {
        return Boolean.valueOf((((it_1).intValue() % 2) == 0));
      };
      SubStream<Integer, Integer> _filter = StreamExtensions.<Integer, Integer>filter(_map, _function_3);
      final Procedure1<Integer> _function_4 = (Integer it_1) -> {
        this.incActorCounter();
      };
      StreamExtensions.<Integer, Integer>onEach(_filter, _function_4);
    };
    final Actor<Integer> actor = ActorExtensions.<Integer>actor(_function_1);
    IntegerRange _upTo = new IntegerRange(1, 100);
    for (final Integer i : _upTo) {
      actor.apply(i);
    }
    final Procedure0 _function_2 = () -> {
      IntegerRange _upTo_1 = new IntegerRange(1, iterations);
      for (final Integer i_1 : _upTo_1) {
        funct.apply(i_1);
      }
    };
    final long functTimeMs = this.measure(_function_2);
    InputOutput.<String>println(("function took: " + Long.valueOf(functTimeMs)));
    final Procedure0 _function_3 = () -> {
      IntegerRange _upTo_1 = new IntegerRange(1, iterations);
      for (final Integer i_1 : _upTo_1) {
        actor.apply(i_1);
      }
    };
    final long streamTimeMs = this.measure(_function_3);
    InputOutput.<String>println(("stream took: " + Long.valueOf(streamTimeMs)));
  }
  
  /**
   * measure the duration of an action
   */
  public long measure(final Procedure0 actionFn) {
    long _xblockexpression = (long) 0;
    {
      final long start = System.currentTimeMillis();
      actionFn.apply();
      final long end = System.currentTimeMillis();
      _xblockexpression = (end - start);
    }
    return _xblockexpression;
  }
  
  /**
   * measure the duration of an action executed on multiple threads at once
   */
  @Async
  public SubPromise<Integer, Long> measure(final int threads, final Procedure0 actionFn) {
    SubPromise<Integer, Long> _xblockexpression = null;
    {
      final ExecutorService pool = Executors.newFixedThreadPool(threads);
      final long start = System.currentTimeMillis();
      IntegerRange _upTo = new IntegerRange(1, threads);
      Stream<Integer> _stream = StreamExtensions.<Integer>stream(_upTo);
      final Function1<Integer, Task> _function = (Integer it) -> {
        final Runnable _function_1 = () -> {
          actionFn.apply();
        };
        return ExecutorExtensions.task(pool, _function_1);
      };
      SubStream<Integer, Task> _map = StreamExtensions.<Integer, Integer, Task>map(_stream, _function);
      SubStream<Integer, Boolean> _resolve = StreamExtensions.<Integer, Boolean>resolve(_map, threads);
      SubStream<Integer, List<Boolean>> _collect = StreamExtensions.<Integer, Boolean>collect(_resolve);
      IPromise<Integer, List<Boolean>> _first = StreamExtensions.<Integer, List<Boolean>>first(_collect);
      final Function1<List<Boolean>, Long> _function_1 = (List<Boolean> it) -> {
        long _currentTimeMillis = System.currentTimeMillis();
        return Long.valueOf((_currentTimeMillis - start));
      };
      _xblockexpression = PromiseExtensions.<Integer, List<Boolean>, Long>map(_first, _function_1);
    }
    return _xblockexpression;
  }
  
  private void setAccess(final Integer value) {
    this._access.set(value);
  }
  
  private Integer getAccess() {
    return this._access.get();
  }
  
  private Integer getAndSetAccess(final Integer value) {
    return this._access.getAndSet(value);
  }
  
  private Integer incAccess() {
    return this._access.incrementAndGet();
  }
  
  private Integer decAccess() {
    return this._access.decrementAndGet();
  }
  
  private Integer incAccess(final Integer value) {
    return this._access.addAndGet(value);
  }
  
  private void setValue(final Integer value) {
    this._value.set(value);
  }
  
  private Integer getValue() {
    return this._value.get();
  }
  
  private Integer getAndSetValue(final Integer value) {
    return this._value.getAndSet(value);
  }
  
  private Integer incValue() {
    return this._value.incrementAndGet();
  }
  
  private Integer decValue() {
    return this._value.decrementAndGet();
  }
  
  private Integer incValue(final Integer value) {
    return this._value.addAndGet(value);
  }
  
  private void setMultipleThreadAccessViolation(final Integer value) {
    this._multipleThreadAccessViolation.set(value);
  }
  
  private Integer getMultipleThreadAccessViolation() {
    return this._multipleThreadAccessViolation.get();
  }
  
  private Integer getAndSetMultipleThreadAccessViolation(final Integer value) {
    return this._multipleThreadAccessViolation.getAndSet(value);
  }
  
  private Integer incMultipleThreadAccessViolation() {
    return this._multipleThreadAccessViolation.incrementAndGet();
  }
  
  private Integer decMultipleThreadAccessViolation() {
    return this._multipleThreadAccessViolation.decrementAndGet();
  }
  
  private Integer incMultipleThreadAccessViolation(final Integer value) {
    return this._multipleThreadAccessViolation.addAndGet(value);
  }
  
  private void setDecreaser(final Actor<Integer> value) {
    this._decreaser.set(value);
  }
  
  private Actor<Integer> getDecreaser() {
    return this._decreaser.get();
  }
  
  private Actor<Integer> getAndSetDecreaser(final Actor<Integer> value) {
    return this._decreaser.getAndSet(value);
  }
  
  private void setActorCounter(final Integer value) {
    this._actorCounter.set(value);
  }
  
  private Integer getActorCounter() {
    return this._actorCounter.get();
  }
  
  private Integer getAndSetActorCounter(final Integer value) {
    return this._actorCounter.getAndSet(value);
  }
  
  private Integer incActorCounter() {
    return this._actorCounter.incrementAndGet();
  }
  
  private Integer decActorCounter() {
    return this._actorCounter.decrementAndGet();
  }
  
  private Integer incActorCounter(final Integer value) {
    return this._actorCounter.addAndGet(value);
  }
  
  private void setFunctCounter(final Integer value) {
    this._functCounter.set(value);
  }
  
  private Integer getFunctCounter() {
    return this._functCounter.get();
  }
  
  private Integer getAndSetFunctCounter(final Integer value) {
    return this._functCounter.getAndSet(value);
  }
  
  private Integer incFunctCounter() {
    return this._functCounter.incrementAndGet();
  }
  
  private Integer decFunctCounter() {
    return this._functCounter.decrementAndGet();
  }
  
  private Integer incFunctCounter(final Integer value) {
    return this._functCounter.addAndGet(value);
  }
  
  private void setUnsyncedCounter(final Integer value) {
    this._unsyncedCounter.set(value);
  }
  
  private Integer getUnsyncedCounter() {
    return this._unsyncedCounter.get();
  }
  
  private Integer getAndSetUnsyncedCounter(final Integer value) {
    return this._unsyncedCounter.getAndSet(value);
  }
  
  private Integer incUnsyncedCounter() {
    return this._unsyncedCounter.incrementAndGet();
  }
  
  private Integer decUnsyncedCounter() {
    return this._unsyncedCounter.decrementAndGet();
  }
  
  private Integer incUnsyncedCounter(final Integer value) {
    return this._unsyncedCounter.addAndGet(value);
  }
  
  private void setSyncedCounter(final Integer value) {
    this._syncedCounter.set(value);
  }
  
  private Integer getSyncedCounter() {
    return this._syncedCounter.get();
  }
  
  private Integer getAndSetSyncedCounter(final Integer value) {
    return this._syncedCounter.getAndSet(value);
  }
  
  private Integer incSyncedCounter() {
    return this._syncedCounter.incrementAndGet();
  }
  
  private Integer decSyncedCounter() {
    return this._syncedCounter.decrementAndGet();
  }
  
  private Integer incSyncedCounter(final Integer value) {
    return this._syncedCounter.addAndGet(value);
  }
}
