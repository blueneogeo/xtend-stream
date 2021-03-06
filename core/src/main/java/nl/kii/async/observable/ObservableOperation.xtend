package nl.kii.async.observable

import co.paralleluniverse.fibers.Suspendable
import java.util.Date
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference
import nl.kii.async.SuspendableFunctions.Function1
import nl.kii.async.SuspendableFunctions.Function2
import nl.kii.async.SuspendableFunctions.Function4
import nl.kii.async.SuspendableProcedures.Procedure2
import nl.kii.async.annotation.Cold
import nl.kii.async.annotation.Controlled
import nl.kii.async.annotation.Hot
import nl.kii.async.annotation.NoBackpressure
import nl.kii.async.annotation.Suspending
import nl.kii.async.annotation.Uncontrolled
import nl.kii.async.annotation.Unsorted
import nl.kii.async.promise.Promise
import nl.kii.async.promise.Task
import nl.kii.async.stream.Source
import nl.kii.async.stream.Stream
import nl.kii.util.Opt
import nl.kii.util.Period

import static extension nl.kii.util.DateExtensions.*
import static extension nl.kii.util.OptExtensions.*
import static extension nl.kii.util.ThrowableExtensions.*

final class ObservableOperation {

	// OBSERVATION /////////////////////////////////////////////////////////////////////////////

	/** 
	 * Lets you observe with multiple observers at the same time.
	 */
	@Cold @NoBackpressure
	def static <IN, OUT> observeWith(Observable<IN, OUT> observable, @Suspending Observer<IN, OUT>... observers) {
		observable.observer = new Observer<IN, OUT> {
			
			@Suspendable
			override value(IN in, OUT value) {
				for(observer : observers) {
					try {
						observer.value(in, value)
					} catch(Throwable t) {
						observer.error(in, t)
					}
				}
			}
			
			@Suspendable
			override error(IN in, Throwable t) {
				for(observer : observers) {
					try {
						observer.error(in, t)
					} catch(Throwable t2) {
						// let errors of error handling die quietly
					}
				}
			}
			
			@Suspendable
			override complete() {
				for(observer : observers) {
					try {
						observer.complete
					} catch(Throwable t) {
						observer.error(null, t)
					}
				}
			}
		}
	}
	
	// COMBINE /////////////////////////////////////////////////////////////////////////////////

	@Hot @Controlled @Unsorted
	def static <IN, OUT> void merge(Observer<IN, OUT> observer, @Suspending Observable<IN, OUT>... observables) {
		val completed = new AtomicInteger(0)
		for(observable : observables) {
			observable.observer = new Observer<IN, OUT> {
				
				@Suspendable
				override value(IN in, OUT value) {
					observer.value(in, value)
					observable.next
				}
				
				@Suspendable
				override error(IN in, Throwable t) {
					observer.error(in, t)
					observable.next
				}
				
				@Suspendable
				override complete() {
					// complete when all observables are complete
					if(completed.incrementAndGet >= observables.size) {
						observer.complete
					}
				}
				
			}
			observable.next
		}
	}

	// UNTIL ///////////////////////////////////////////////////////////////////////////////////

	@Cold @Controlled
	def static <IN, OUT> void until(Observable<IN, OUT> observable, @Suspending Observer<IN, OUT> observer, @Suspending Function4<IN, OUT, Long, Long, Boolean> stopObservingFn) {
		val index = new AtomicLong(0)
		val passed = new AtomicLong(0)
		observable.observer = new Observer<IN, OUT> {
			
			@Suspendable
			override value(IN in, OUT value) {
				if(stopObservingFn.apply(in, value, index.incrementAndGet, passed.get)) {
					observer.complete
				} else {
					passed.incrementAndGet
					observer.value(in, value)
				}
			}
			
			@Suspendable
			override error(IN in, Throwable t) {
				observer.error(in, t)
			}
			
			@Suspendable
			override complete() {
				observer.complete
			}
			
		}
	}

	// MAPPING /////////////////////////////////////////////////////////////////////////////////

	@Cold @Controlled
	def static <IN, OUT, MAP> void map(Observable<IN, OUT> observable, @Suspending Observer<IN, MAP> observer, @Suspending Function2<IN, OUT, MAP> mapFn) {
		observable.observer = new Observer<IN, OUT> {
			
			@Suspendable
			override value(IN in, OUT value) {
				try {
					val mapped = mapFn.apply(in, value)
					observer.value(in, mapped)
				} catch(Throwable t) {
					observer.error(in, t)
				}
			}
			
			@Suspendable
			override error(IN in, Throwable t) {
				observer.error(in, t)
			}
			
			@Suspendable
			override complete() {
				observer.complete
			}
		}
	}
		
	@Cold @Unsorted @Uncontrolled
	def static <IN, OUT, IN2, OBS extends Observable<IN2, OUT>> flatten(Observable<IN, OBS> observable, @Suspending Observer<IN, OUT> observer) {
		val isComplete = new AtomicBoolean(false)
		val openProcesses = new AtomicInteger(0)
		
		observable.observer = new Observer<IN, OBS> {
			
			@Suspendable
			override value(IN in, OBS innerObservable) {
				innerObservable.observer = new Observer<IN2, OUT> {
					
					@Suspendable
					override value(IN2 ignore, OUT value) {
						observer.value(in, value)
						innerObservable.next
					}
					
					@Suspendable
					override error(Object ignore, Throwable error) {
						observer.error(in, error)
						innerObservable.next
					}
					
					@Suspendable
					override complete() {
						if(openProcesses.decrementAndGet == 0 && isComplete.compareAndSet(true, false)) {
							observer.complete
						}
					}
					
				}
				
				openProcesses.incrementAndGet
				innerObservable.next
			}
			
			@Suspendable
			override error(IN in, Throwable error) {
				observer.error(in, error)
			}
			
			@Suspendable
			override complete() {
				if(openProcesses.get == 0) {
					// we are not parallel processing, you may inform the listening stream
					observer.complete
				} else {
					// we are still busy, so remember to call finish when we are done
					isComplete.set(true)
				}
			}
			
		}

	}	

	@Cold @Controlled	
	def static <IN1, IN2, OUT> mapInput(Observable<IN1, OUT> observable, @Suspending Observer<IN2, OUT> observer, @Suspending Function2<IN1, Opt<OUT>, IN2> inputMapFn) {
		observable.observer = new Observer<IN1, OUT> {
			
			@Suspendable
			override value(IN1 in1, OUT value) {
				val in2 = inputMapFn.apply(in1, value.option)
				observer.value(in2, value)
			}
			
			@Suspendable
			override error(IN1 in1, Throwable t) {
				val in2 = inputMapFn.apply(in1, none)
				observer.error(in2, t)
			}
			
			@Suspendable
			override complete() {
				observer.complete
			}
			
		}
	}
	
	// ERROR HANDLING //////////////////////////////////////////////////////////////////////////
	
	@Cold @Controlled
	def static <IN, OUT, E extends Throwable> void onError(Observable<IN, OUT> observable, @Suspending Observer<IN, OUT> observer, Class<E> errorClass, boolean swallow, @Suspending Procedure2<IN, E> onErrorFn) {
		observable.observer = new Observer<IN, OUT> {
			
			@Suspendable
			override value(IN in, OUT value) {
				observer.value(in, value)
			}
			
			@Suspendable
			override error(IN in, Throwable t) {
				try {
					if(errorClass.isAssignableFrom(t.class)) {
						onErrorFn.apply(in, t as E)
						if(swallow) {
							observable.next
						} else {
							observer.error(in, t)
						}
					} else {
						observer.error(in, t)
					}
				} catch(Exception e) {
					 observer.error(in, t)
				}
			}
			
			@Suspendable
			override complete() {
				observer.complete
			}
			
		}
	}

	@Cold @Controlled
	def static <IN, OUT, ERROR extends Throwable> void onErrorMap(Observable<IN, OUT> observable, @Suspending Observer<IN, OUT> observer, Class<ERROR> errorClass, boolean swallow, @Suspending Function2<IN, ERROR, OUT> onErrorMapFn) {
		observable.observer = new Observer<IN, OUT> {
			
			@Suspendable
			override value(IN in, OUT value) {
				observer.value(in, value)
			}
			
			@Suspendable
			override error(IN in, Throwable error) {
				try {
					if(error.matches(errorClass)) {
						val value = onErrorMapFn.apply(in, error as ERROR)
						observer.value(in, value)
					} else {
						observer.error(in, error)
					}
				} catch(Throwable t) {
					observer.error(in, t)
				}
			}
			
			@Suspendable
			override complete() {
				observer.complete
			}
			
		}
	}
	
	/** Asynchronously map an error back to a value. Swallows the error. */
	@Cold @Unsorted @Controlled
	def static <ERROR extends Throwable, IN, OUT, IN2> void onErrorCall(Observable<IN, OUT> observable, @Suspending Observer<IN, OUT> observer, Class<ERROR> errorType, @Suspending Function2<IN, ERROR, Promise<IN2, OUT>> onErrorCallFn) {
		val completed = new AtomicBoolean(false)
		val processes = new AtomicInteger(0)
		observable.observer = new Observer<IN, OUT> {
			
			@Suspendable
			override value(IN in, OUT value) {
				observer.value(in, value)
			}
			
			@Suspendable
			override error(IN in, Throwable error) {
				if(error.matches(errorType)) {
					// start a new async process by calling the mapFn
					try {
						processes.incrementAndGet
						val promise = onErrorCallFn.apply(in, error as ERROR)
						promise.observer = new Observer<IN2, OUT> {
							
							override value(IN2 unused, OUT value) {
								observer.value(in, value)
							}
							
							override error(IN2 unused, Throwable error) {
								observer.error(in, error)
							}
							
							override complete() {
								// if the stream completed and this was the last process, we are done
								if(processes.decrementAndGet <= 0 && completed.get) {
									observer.complete
								}
							}
							
						}
					} catch(Throwable t) {
						observer.error(in, t)
						// if the stream completed and this was the last process, we are done
						if(processes.decrementAndGet <= 0 && completed.get) {
							observer.complete
						}
					}
				} else {
					observer.error(in, error)
				}
			}
			
			@Suspendable
			override complete() {
				// the observable is complete
				completed.set(true)
				// if there are no more processes running, we are done
				if(processes.get <= 0) {
					observer.complete
				}
			}
			
		}
		
	}
	
	// RETENTION AND TIME //////////////////////////////////////////////////////////////////////

	/** 
	 * Adds delay to each observed value.
	 * If the observable is completed, it will only send complete to the observer once all delayed values have been sent.
	 * <p>
	 * Better than using stream.perform [ timerFn.apply(period) ] since uncontrolled streams then can be completed before
	 * all values have been pushed.
	 */	
	@Cold @Controlled	
	def static <IN, OUT> delay(Observable<IN, OUT> observable, @Suspending Observer<IN, OUT> observer, Period delay, @Suspending Function1<Period, Task> timerFn) {
		observable.observer = new Observer<IN, OUT> {
			
			val timers = new AtomicInteger
			val completed = new AtomicBoolean
			
			@Suspendable
			override value(IN in, OUT value) {
				timers.incrementAndGet
				val task = timerFn.apply(delay)
				task.observer = new Observer<Void, Void> {
					
					@Suspendable
					override value(Void ignore, Void ignore2) {
						val openTimers = timers.decrementAndGet 
						observer.value(in, value)
						if(completed.get && openTimers == 0) {
							observer.complete
						}
					}
					
					@Suspendable
					override error(Void ignore, Throwable t) {
						observer.error(in, t)
					}
					
					@Suspendable
					override complete() {
						// do nothing
					}
					
				}
			}
			
			@Suspendable
			override error(IN in, Throwable t) {
				observer.error(in, t)
			}
			
			@Suspendable
			override complete() {
				if(completed.compareAndSet(false, true)) {
					if(timers.get == 0) {
						observer.complete
					}
				}
			}
			
		}
	}

	/**
	 * Cuts up the incoming observable into time windows. Each window is a stream of values.
	 */
	@Cold @NoBackpressure	
	def static <IN, OUT> window(Observable<IN, OUT> observable, @Suspending Observer<IN, Stream<IN, OUT>> observer, Period interval) {
		observable.observer = new Observer<IN, OUT> {

			val lastValueMoment = new AtomicReference<Date>
			val currentObservable = new AtomicReference<Source<IN, OUT>>	
			
			@Suspendable
			override value(IN in, OUT value) {
				val windowExpired = (lastValueMoment.get === null || now - lastValueMoment.get > interval)
				if(windowExpired || currentObservable.get === null) {
					currentObservable.get?.complete
					lastValueMoment.set(now)
					val newObservable = new Source<IN, OUT> {
						
						override onNext() {
							observable.next
						}
						
						override onClose() {
							// do nothing
						}
						
					}
					currentObservable.set(newObservable)
					observer.value(in, newObservable)
				}
				currentObservable.get.value(in, value)
			}
			
			@Suspendable
			override error(IN in, Throwable t) {
				observer.error(in, t)
			}
			
			@Suspendable
			override complete() {
				currentObservable.get?.complete
				observer.complete
			}
			
		}
	}

	/**
	 * Stream only [amount] values per [period]. Anything more will be rejected and the next value asked.
	 * Errors and complete are streamed immediately.
	 */
	@Cold @Controlled	
	def static <IN, OUT> throttle(Observable<IN, OUT> observable, @Suspending Observer<IN, OUT> observer, Period minimumInterval) {
		observable.observer = new Observer<IN, OUT> {

			val lastValueMoment = new AtomicReference<Date>	
			
			@Suspendable
			override value(IN in, OUT value) {
				if(lastValueMoment.get === null || now - lastValueMoment.get > minimumInterval) {
					lastValueMoment.set(now())
					observer.value(in, value)
				} else {
					observable.next
				}
			}
			
			@Suspendable
			override error(IN in, Throwable t) {
				observer.error(in, t)
			}
			
			@Suspendable
			override complete() {
				observer.complete
			}
			
		}
	}

	/**
	 * Stream only [amount] values per [period]. Anything more will be buffered until available. 
	 * Requires a buffered stream to work.
	 */
	@Cold @Controlled
	def static <IN, OUT> ratelimit(Observable<IN, OUT> observable, @Suspending Observer<IN, OUT> observer, Period minimumInterval, @Suspending Function1<Period, Task> timerFn) {
		observable.observer = new Observer<IN, OUT> {

			val lastValueMoment = new AtomicReference<Date>	
			val timing = new AtomicBoolean
			val completed = new AtomicBoolean
			
			@Suspendable
			override value(IN in, OUT value) {
				val now = new Date
				if(lastValueMoment.get === null) {
					// we can send right away
					observer.value(in, value)
					lastValueMoment.set(now)
				} else if(now - lastValueMoment.get > minimumInterval) {
					// we can send right away
					observer.value(in, value)
					lastValueMoment.set(now)
				} else {
					if(timing.compareAndSet(false, true)) {
						// not yet, delay this value
						val timeExpired = now - lastValueMoment.get
						val timeRemaining = minimumInterval - timeExpired
						timerFn.apply(timeRemaining).observer = new Observer<Void, Void> {
							
							override value(Void ignore, Void ignore2) {
								timing.set(false)
								lastValueMoment.set(new Date)
								observer.value(in, value)
								if(completed.get && !timing.get) {
									observer.complete
								}
							}
							
							override error(Void ignore, Throwable t) {
								observer.error(in, t)
							}
							
							override complete() {
								timing.set(false)
							}
						}
					} else {
						// too soon for the next value, disregard
						// when the value has been sent, that will call next from the buffer.
					}
				}
			}
			
			@Suspendable
			override error(IN in, Throwable t) {
				observer.error(in, t)
			}
			
			@Suspendable
			override complete() {
				if(completed.compareAndSet(false, true)) {
					if(!timing.get) {
						observer.complete
					}
				}
			}
			
		}
	}
	
}
