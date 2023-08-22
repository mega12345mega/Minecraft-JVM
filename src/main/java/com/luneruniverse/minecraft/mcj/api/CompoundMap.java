package com.luneruniverse.minecraft.mcj.api;

/**
 * This uses an NBTCompound to store key-value pairs<br>
 * This makes it unable to iterate through keys (or values)<br>
 * Use an {@link IterableMap} if you need to track the keys
 * @param <K> The key type
 * @param <V> The value type
 */
@MCJImplFor("mcj:")
public class CompoundMap<K, V> {
	
	public CompoundMap() {
		clear();
	}
	
	@MCJNativeImpl({"""
			function $(~pointer_handler) with storage mcj:data localvars.v0
			""", """
			# pointer_handler
			data modify storage mcj:data stack append value {}
			$execute store result storage mcj:data stack[-1].value int 1 run data get storage mcj:data heap.v$(value).map
			"""})
	public native int size();
	
	public boolean isEmpty() {
		return size() == 0;
	}
	
	@MCJNativeImpl({"""
			data modify storage mcj:data localvars.v0.key set from storage mcj:data localvars.v1.value
			function $(~pointer_handler) with storage mcj:data localvars.v0
			""", """
			# pointer_handler
			data modify storage mcj:data stack append value {value:0b}
			$execute if data storage mcj:data heap.v$(value).map.k$(key) run data modify storage mcj:data stack[-1].value set value 1b
			"""})
	public native boolean containsKey(Object key);
	
	@MCJNativeImpl({"""
		data modify storage mcj:data localvars.v0.key set from storage mcj:data localvars.v1.value
		function $(~pointer_handler) with storage mcj:data localvars.v0
		""", """
		# pointer_handler
		data modify storage mcj:data stack append value {value:0}
		$execute if data storage mcj:data heap.v$(value).map.k$(key) run data modify storage mcj:data stack[-1] set from storage mcj:data heap.v$(value).map.k$(key)
		"""})
	public native V get(Object key);
	
	@MCJNativeImpl({"""
		data modify storage mcj:data localvars.v0.key set from storage mcj:data localvars.v1.value
		function $(~pointer_handler) with storage mcj:data localvars.v0
		""", """
		# pointer_handler
		data modify storage mcj:data stack append value {value:0}
		$execute if data storage mcj:data heap.v$(value).map.k$(key) run data modify storage mcj:data stack[-1] set from storage mcj:data heap.v$(value).map.k$(key)
		$data modify storage mcj:data heap.v$(value).map.k$(key) set from storage mcj:data localvars.v2
		"""})
	public native V put(K key, V value);
	
	@MCJNativeImpl({"""
		data modify storage mcj:data localvars.v0.key set from storage mcj:data localvars.v1.value
		function $(~pointer_handler) with storage mcj:data localvars.v0
		""", """
		# pointer_handler
		data modify storage mcj:data stack append value {value:0}
		$execute if data storage mcj:data heap.v$(value).map.k$(key) run data modify storage mcj:data stack[-1] set from storage mcj:data heap.v$(value).map.k$(key)
		$data remove storage mcj:data heap.v$(value).map.k$(key)
		"""})
	public native V remove(Object key);
	
	@MCJNativeImpl({"""
		function $(~pointer_handler) with storage mcj:data localvars.v0
		""", """
		# pointer_handler
		data modify storage mcj:data heap.v$(value).map set value {}
		"""})
	public native void clear();
	
}
