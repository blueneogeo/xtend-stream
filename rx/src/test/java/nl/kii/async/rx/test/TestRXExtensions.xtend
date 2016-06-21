package nl.kii.async.rx.test

import org.junit.Test
import rx.Observable

import static org.junit.Assert.*

import static extension nl.kii.async.promise.PromiseExtensions.*
import static extension nl.kii.async.rx.RXExtensions.*
import static extension nl.kii.async.stream.StreamExtensions.*

class TestRXExtensions {

	static var counter = 0
	
	@Test
	def void testRxObservableToStream() {
		val iterations = 1_000
		// create an observable
		val observable = Observable.from(1..iterations)
		// count the results from the observable using xtend-stream
		val count = observable.stream.count
		// now all the data from the observable should have been counted
		assertEquals(iterations, count.await as int)
	}

	@Test
	def void testRxObservableToPromise() {
		val iterations = 2
		// create an observable
		val observable = Observable.from(1..iterations)
		// now all the first entry from the observable should have been promised
		val first = observable.promise.await
		assertEquals(1, first)
	}
	
}
