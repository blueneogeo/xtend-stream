package nl.kii.observe;

import com.google.common.base.Objects;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import nl.kii.act.Actor;
import nl.kii.observe.Observable;
import org.eclipse.xtext.xbase.lib.CollectionLiterals;
import org.eclipse.xtext.xbase.lib.Procedures.Procedure0;
import org.eclipse.xtext.xbase.lib.Procedures.Procedure1;

/**
 * A threadsafe distributor of events to its registered listeners.
 * <p>
 * A Publisher is more lightweight than a stream with a streamobserver.
 * It does not have any flow control or async support, and has only
 * a single queue.
 * <p>
 * For it to work correctly, the listeners should be non-blocking.
 */
@SuppressWarnings("all")
public class Publisher<T extends Object> extends Actor<T> implements Observable<T> {
  private final AtomicBoolean _publishing = new AtomicBoolean(true);
  
  private final AtomicReference<List<Procedure1<T>>> observers = new AtomicReference<List<Procedure1<T>>>();
  
  public synchronized Procedure0 onChange(final Procedure1<? super T> observeFn) {
    List<Procedure1<T>> _get = this.observers.get();
    boolean _equals = Objects.equal(_get, null);
    if (_equals) {
      LinkedList<Procedure1<T>> _newLinkedList = CollectionLiterals.<Procedure1<T>>newLinkedList(((Procedure1<T>)observeFn));
      this.observers.set(_newLinkedList);
    } else {
      List<Procedure1<T>> _get_1 = this.observers.get();
      _get_1.add(((Procedure1<T>)observeFn));
    }
    final Procedure0 _function = new Procedure0() {
      public void apply() {
        List<Procedure1<T>> _get = Publisher.this.observers.get();
        _get.remove(observeFn);
      }
    };
    return _function;
  }
  
  protected void act(final T message, final Procedure0 done) {
    boolean _isPublishing = this.isPublishing();
    if (_isPublishing) {
      List<Procedure1<T>> _get = this.observers.get();
      for (final Procedure1<T> observer : _get) {
        observer.apply(message);
      }
    }
    done.apply();
  }
  
  public boolean isPublishing() {
    return this._publishing.get();
  }
  
  public void setPublishing(final boolean value) {
    this._publishing.set(value);
  }
}
