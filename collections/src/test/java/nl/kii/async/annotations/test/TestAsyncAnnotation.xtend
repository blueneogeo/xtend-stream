package nl.kii.async.annotations.test

import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import nl.kii.async.annotation.Async
import nl.kii.async.promise.Input
import nl.kii.async.promise.Task
import org.junit.Test

import static java.util.concurrent.Executors.*
import static org.junit.Assert.*

import static extension nl.kii.async.promise.PromiseExtensions.*

class TestAsyncAnnotation {

	@Test
	def void testAsyncPromise() {
		val result = new AtomicInteger
		increment(5)
			.then [ result.set(it) ]
		assertEquals(6, result.get)
	}

	@Test
	def void testAsyncTask() {
		val result = new AtomicBoolean
		printHello('world')
			.on(Throwable) [ println(it) ]
			.then [ result.set(true) ]
		assertTrue(result.get)
	}

	@Test
	def void testAsyncErrorHandling() {
		val isError = new AtomicBoolean
		printHello(null)
			.on(Throwable) [ isError.set(true) ]
			.then [ isError.set(false) ]
		assertTrue(isError.get)
	}

	@Test
	def void testAsyncTaskOnExecutor() {
		val success = new AtomicBoolean
		val exec = newCachedThreadPool
		exec.printHello('world').then [ success.set(true) ]
		Thread.sleep(10)
		assertTrue(success.get)
	}
	
	@Async def increment(int number, Input<Integer> promise) {
		promise.set(number + 1) 
	}

	@Async(true) def printHello(Task task, String name) {
		if(name === null) throw new Exception('name cannot be empty')
		println('hello ' + name)
		task.complete
	}

}
