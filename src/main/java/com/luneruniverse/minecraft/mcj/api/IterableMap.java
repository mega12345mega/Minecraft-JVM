package com.luneruniverse.minecraft.mcj.api;

/**
 * This uses a {@link CompoundMap} for mapping and an {@link Array} to keep track of the keys<br>
 * Use a {@link CompoundMap} if you don't need to track the keys
 * @param <K> The key type
 * @param <V> The value type
 */
@MCJImplFor("mcj:")
public class IterableMap<K, V> {
	
	private final Array<K> keys;
	private final CompoundMap<K, V> map;
	
	public IterableMap() {
		keys = new Array<>();
		map = new CompoundMap<>();
	}
	
	public int size() {
		return map.size();
	}
	
	public boolean isEmpty() {
		return map.isEmpty();
	}
	
	public boolean containsKey(Object key) {
		return map.containsKey(key);
	}
	
	public boolean containsValue(Object value) {
		int len = size();
		for (int i = 0; i < len; i++) {
			if (map.get(keys.get(i)) == value)
				return true;
		}
		return false;
	}
	
	public V get(Object key) {
		return map.get(key);
	}
	
	public V put(K key, V value) {
		keys.add(key);
		return map.put(key, value);
	}
	
	public V remove(Object key) {
		keys.remove(key);
		return map.remove(key);
	}
	
	public void putAll(IterableMap<? extends K, ? extends V> m) {
		int len = m.map.size();
		for (int i = 0; i < len; i++) {
			K key = m.keys.get(i);
			put(key, m.map.get(key));
		}
	}
	
	public void clear() {
		keys.clear();
		map.clear();
	}
	
	/**
	 * <strong>Warning:</strong> Do NOT modify the returned Array (use {@link Array#toArray()} if you need to)
	 * @return The internal keys array
	 */
	public Array<K> keys() {
		return keys;
	}
	
	/**
	 * @return The values in a new array that may be modified without affecting this IterableMap
	 */
	public Array<V> values() {
		Array<V> output = new Array<>();
		int len = size();
		for (int i = 0; i < len; i++) {
			output.add(map.get(keys.get(i)));
		}
		return output;
	}
	
	public Array.ArrayIterator<K> iterator() {
		return keys.iterator();
	}
	
}
