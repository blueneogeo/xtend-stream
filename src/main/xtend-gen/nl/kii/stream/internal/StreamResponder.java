package nl.kii.stream.internal;

import java.util.concurrent.atomic.AtomicReference;
import nl.kii.async.annotation.Atomic;
import nl.kii.stream.IStream;
import nl.kii.stream.internal.StreamObserver;
import org.eclipse.xtext.xbase.lib.Procedures.Procedure1;
import org.eclipse.xtext.xbase.lib.Procedures.Procedure2;

/**
 * A basic builder for asynchronous stream listening.
 * Combine with StreamExtensions.on like this:
 * <p>
 * <pre>
 * stream.on [
 *    each [ ... stream.next ]
 *    finish [ ... ]
 *    error [ ... ]
 * ]
 * stream.next
 * </pre>
 * <p>
 * Remember to call stream.next to start the stream!
 */
@SuppressWarnings("all")
public class StreamResponder<I extends Object, O extends Object> implements StreamObserver<I, O> {
  @Atomic
  private final AtomicReference<IStream<I, O>> _stream = new AtomicReference<IStream<I, O>>();
  
  @Atomic
  private final AtomicReference<Procedure2<I, O>> _valueFn = new AtomicReference<Procedure2<I, O>>();
  
  @Atomic
  private final AtomicReference<Procedure2<I, Throwable>> _errorFn = new AtomicReference<Procedure2<I, Throwable>>();
  
  @Atomic
  private final AtomicReference<Procedure2<I, Integer>> _finishFn = new AtomicReference<Procedure2<I, Integer>>();
  
  @Atomic
  private final AtomicReference<Procedure1<Void>> _closedFn = new AtomicReference<Procedure1<Void>>();
  
  /**
   * listen for each incoming value
   */
  public void each(final Procedure2<? super I, ? super O> handler) {
    this.setValueFn(((Procedure2<I, O>)handler));
  }
  
  /**
   * listen for any finish
   */
  public void finish(final Procedure2<? super I, ? super Integer> handler) {
    this.setFinishFn(((Procedure2<I, Integer>)handler));
  }
  
  /**
   * listen for any uncaught errors
   */
  public void error(final Procedure2<? super I, ? super Throwable> handler) {
    this.setErrorFn(((Procedure2<I, Throwable>)handler));
  }
  
  /**
   * listen for when the stream closes
   */
  public void closed(final Procedure1<? super Void> handler) {
    this.setClosedFn(((Procedure1<Void>)handler));
  }
  
  @Override
  public void onValue(final I from, final O value) {
    Procedure2<I, O> _valueFn = this.getValueFn();
    if (_valueFn!=null) {
      _valueFn.apply(from, value);
    }
  }
  
  @Override
  public void onError(final I from, final Throwable t) {
    Procedure2<I, Throwable> _errorFn = this.getErrorFn();
    if (_errorFn!=null) {
      _errorFn.apply(from, t);
    }
  }
  
  @Override
  public void onFinish(final I from, final int level) {
    Procedure2<I, Integer> _finishFn = this.getFinishFn();
    if (_finishFn!=null) {
      _finishFn.apply(from, Integer.valueOf(level));
    }
  }
  
  @Override
  public void onClosed() {
    Procedure1<Void> _closedFn = this.getClosedFn();
    if (_closedFn!=null) {
      _closedFn.apply(null);
    }
  }
  
  public void setStream(final IStream<I, O> value) {
    this._stream.set(value);
  }
  
  public IStream<I, O> getStream() {
    return this._stream.get();
  }
  
  protected IStream<I, O> getAndSetStream(final IStream<I, O> value) {
    return this._stream.getAndSet(value);
  }
  
  private void setValueFn(final Procedure2<I, O> value) {
    this._valueFn.set(value);
  }
  
  private Procedure2<I, O> getValueFn() {
    return this._valueFn.get();
  }
  
  private Procedure2<I, O> getAndSetValueFn(final Procedure2<I, O> value) {
    return this._valueFn.getAndSet(value);
  }
  
  private void setErrorFn(final Procedure2<I, Throwable> value) {
    this._errorFn.set(value);
  }
  
  private Procedure2<I, Throwable> getErrorFn() {
    return this._errorFn.get();
  }
  
  private Procedure2<I, Throwable> getAndSetErrorFn(final Procedure2<I, Throwable> value) {
    return this._errorFn.getAndSet(value);
  }
  
  private void setFinishFn(final Procedure2<I, Integer> value) {
    this._finishFn.set(value);
  }
  
  private Procedure2<I, Integer> getFinishFn() {
    return this._finishFn.get();
  }
  
  private Procedure2<I, Integer> getAndSetFinishFn(final Procedure2<I, Integer> value) {
    return this._finishFn.getAndSet(value);
  }
  
  private void setClosedFn(final Procedure1<Void> value) {
    this._closedFn.set(value);
  }
  
  private Procedure1<Void> getClosedFn() {
    return this._closedFn.get();
  }
  
  private Procedure1<Void> getAndSetClosedFn(final Procedure1<Void> value) {
    return this._closedFn.getAndSet(value);
  }
}
