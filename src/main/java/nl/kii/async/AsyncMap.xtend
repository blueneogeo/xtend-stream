package nl.kii.async

import java.util.Map
import nl.kii.promise.Promise
import nl.kii.promise.Task
import java.util.List

/**
 * An asynchronous version of a Java Map.
 * <p>
 * This means that all operations are non-blocking, and instead of returning void and values,
 * they return Tasks and Promises. These can be listened to for the result of the operation,
 * or to catch any thrown exceptions.
 * <p>
 * Async maps are especially useful representing networked operations, since it allows
 * for slower operations to not block the code and to have a mechanism to catch exceptions.
 * <p>
 * The get for a list of keys is added because it allows the remote implementation to optimize.  
 */
interface AsyncMap<K, V> {
	
	def Task put(K key, V value)
	
	def Promise<V> get(K key)
	
	def Promise<Map<K, V>> get(List<K> keys)
	
	def Task remove(K key)
	
}

/**
 * An AsyncMap that lets you query for results using an index.
 * It has Strings as keys and allows for adding of values,
 * using autogenerated keys.
 */
interface IndexedAsyncMap<V> extends AsyncMap<String, V> {

	/** Perform a query and promise a map of keys and values. 
	 * All parameters are optional and may be null.
	 */
	def Promise<Map<String, V>> query(String index, String query, String startKey, String endKey, Integer skip, Integer limit)

	/** 
	 * Perform a query and promise a list of keys whose values match the query. 
	 * All parameters are optional and may be null.
	 */
	def Promise<List<String>> queryKeys(String index, String query, String startKey, String endKey, Integer skip, Integer limit)

	def Promise<String> add(V value)

	def Promise<String> add(String index, V value)
	
	def Task put(String index, String key, V value)
	
	def Promise<V> get(String index, String key)
	
	def Promise<Map<String, V>> get(String index, List<String> keys)
	
	def Task remove(String index, String key)
	
	/** Generate a new key for the given index. */
	def Promise<String> newKey(String index)

}
