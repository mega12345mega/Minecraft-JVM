package com.luneruniverse.minecraft.mcj.api;

import java.util.Collection;
import java.util.List;
import java.util.ListIterator;

/**
 * This wraps Minecraft's array, allowing for dynamic sizes
 * @param <T> The array's component type
 */
public class Array<T> implements List<T> {
	
	public static class ArrayIterator<T> implements ListIterator<T> {
		private final Array<T> list;
		private int index;
		private int lastIndex;
		public ArrayIterator(Array<T> list, int index) {
			this.list = list;
			this.index = index;
		}
		@Override
		public boolean hasNext() {
			return index < list.size();
		}
		@Override
		public T next() {
			return list.get(lastIndex = index++);
		}
		@Override
		public boolean hasPrevious() {
			return index >= 1;
		}
		@Override
		public T previous() {
			return list.get(lastIndex = --index);
		}
		@Override
		public int nextIndex() {
			return index;
		}
		@Override
		public int previousIndex() {
			return index - 1;
		}
		@Override
		public void remove() {
			list.remove(lastIndex);
		}
		@Override
		public void set(T e) {
			list.set(lastIndex, e);
		}
		@Override
		public void add(T e) {
			list.add(index++, e);
		}
	}
	
	private final Object[] values;
	
	public Array() {
		values = new Object[0]; // Not locked to length 0 due to Minecraft's array system
	}
	
	/**
	 * The Array instance will refer to this array, not a copy of this array, so changes to one will be reflected by the other
	 * @param values The array to wrap
	 */
	public Array(T[] values) {
		this.values = values;
	}
	
	/**
	 * The returned array is the same array that is used by this Array instance, so changes to one will be reflected by the other
	 * @return The wrapped array
	 * @see #toArray()
	 */
	@SuppressWarnings("unchecked")
	public T[] getArray() {
		return (T[]) values;
	}
	
	@MCJNativeImpl({"""
			function mcj:localvars/push_var_to_stack {index:"0"}
			function mcj:heap/getfield {name:"values"}
			function $(~pointer_handler) with storage mcj:data stack[-1]
			""", """
			# pointer_handler
			$execute store result storage mcj:data stack[-1].value int 1 run data get storage mcj:data heap.v$(value).value
			"""})
	@Override
	public native int size();
	
	@Override
	public boolean isEmpty() {
		return size() == 0;
	}
	
	@MCJNativeImpl({"""
			function mcj:localvars/push_var_to_stack {index:"0"}
			function mcj:heap/getfield {name:"values"}
			data modify storage mcj:data stack[-1].o set from storage mcj:data localvars.v1.value
			function $(~pointer_handler) with storage mcj:data stack[-1]
			""", """
			# pointer_handler
			$execute store result storage mcj:data stack[-1].value int 1 run execute if data storage mcj:data heap.v$(value).value[{value:$(o)}]
			"""})
	@Override
	public native boolean contains(Object o);
	
	/**
	 * Equivalent to {@link #listIterator()}
	 */
	@Override
	public ArrayIterator<T> iterator() {
		return new ArrayIterator<>(this, 0);
	}
	
	/**
	 * @return A copy of the wrapped array
	 * @see #getArray()
	 */
	@MCJNativeImpl({"""
			function mcj:localvars/push_var_to_stack {index:"0"}
			function mcj:heap/getfield {name:"values"}
			function mcj:heap/malloc
			data modify storage mcj:data stack[-1].values set from storage mcj:data stack[-2].value
			function $(~pointer_handler) with storage mcj:data stack[-1]
			""", """
			# pointer_handler
			$data modify storage mcj:data heap.v$(value).value set from storage mcj:data heap.v$(values).value
			"""})
	@Override
	public native T[] toArray();
	
	/**
	 * Use {@link #toArray()} instead
	 */
	@SuppressWarnings("unchecked")
	@Deprecated
	@Override
	public <T2> T2[] toArray(T2[] a) {
		return (T2[]) values;
	}
	
	@MCJNativeImpl({"""
			function mcj:localvars/push_var_to_stack {index:"0"}
			function mcj:heap/getfield {name:"values"}
			data modify storage mcj:data stack[-1].e set from storage mcj:data localvars.v1.value
			function $(~pointer_handler) with storage mcj:data stack[-1]
			data modify storage mcj:data stack[-1] set value {value:1b}
			""", """
			# pointer_handler
			$data modify storage mcj:data heap.v$(value).value append value {value:$(e)}
			"""})
	@Override
	public native boolean add(T e);
	
	@MCJNativeImpl({"""
			function mcj:localvars/push_var_to_stack {index:"0"}
			function mcj:heap/getfield {name:"values"}
			data modify storage mcj:data stack[-1].o set from storage mcj:data localvars.v1.value
			function $(~pointer_handler) with storage mcj:data stack[-1]
			""", """
			# pointer_handler
			$execute store success storage mcj:data stack[-1].value int 1 run data remove storage mcj:data heap.v$(value).value[{value:$(o)}]
			"""})
	@Override
	public native boolean remove(Object o);
	
	@Override
	public boolean containsAll(Collection<?> c) {
		for (Object o : c) {
			if (!contains(o))
				return false;
		}
		return true;
	}
	
	@Override
	public boolean addAll(Collection<? extends T> c) {
		for (T e : c) {
			add(e);
		}
		return c.size() > 0;
	}
	
	@Override
	public boolean addAll(int index, Collection<? extends T> c) {
		for (T e : c) {
			add(index, e);
			index++;
		}
		return c.size() > 0;
	}
	
	@Override
	public boolean removeAll(Collection<?> c) {
		boolean changed = false;
		for (Object o : c) {
			if (remove(o))
				changed = true;
		}
		return changed;
	}
	
	@Override
	public boolean retainAll(Collection<?> c) {
		boolean changed = false;
		ArrayIterator<T> i = iterator();
		while (i.hasNext()) {
			if (!c.contains(i.next())) {
				i.remove();
				changed = true;
			}
		}
		return changed;
	}
	
	@MCJNativeImpl({"""
			function mcj:localvars/push_var_to_stack {index:"0"}
			function mcj:heap/getfield {name:"values"}
			function $(~pointer_handler) with storage mcj:data stack[-1]
			""", """
			# pointer_handler
			$data modify storage mcj:data heap.v$(value).value set value []
			"""})
	@Override
	public native void clear();
	
	@MCJNativeImpl({"""
			function mcj:localvars/push_var_to_stack {index:"0"}
			function mcj:heap/getfield {name:"values"}
			data modify storage mcj:data stack[-1].index set from storage mcj:data localvars.v1.value
			function $(~pointer_handler) with storage mcj:data stack[-1]
			""", """
			# pointer_handler
			$data modify storage mcj:data stack[-1] set from storage mcj:data heap.v$(value).value[$(index)]
			"""})
	@Override
	public native T get(int index);
	
	@MCJNativeImpl({"""
		function mcj:localvars/push_var_to_stack {index:"0"}
		function mcj:heap/getfield {name:"values"}
		data modify storage mcj:data stack[-1].index set from storage mcj:data localvars.v1.value
		function $(~pointer_handler) with storage mcj:data stack[-1]
		""", """
		# pointer_handler
		$data modify storage mcj:data stack[-1] set from storage mcj:data heap.v$(value).value[$(index)]
		$data modify storage mcj:data heap.v$(value).value[$(index)].value set from storage mcj:data localvars.v2.value
		"""})
	@Override
	public native T set(int index, T element);
	
	@MCJNativeImpl({"""
		function mcj:localvars/push_var_to_stack {index:"0"}
		function mcj:heap/getfield {name:"values"}
		data modify storage mcj:data stack[-1].index set from storage mcj:data localvars.v1.value
		data modify storage mcj:data stack[-1].element set from storage mcj:data localvars.v2.value
		function $(~pointer_handler) with storage mcj:data stack[-1]
		""", """
		# pointer_handler
		$data modify storage mcj:data heap.v$(value).value insert $(index) value {value:$(element)}
		"""})
	@Override
	public native void add(int index, T element);
	
	@MCJNativeImpl({"""
		function mcj:localvars/push_var_to_stack {index:"0"}
		function mcj:heap/getfield {name:"values"}
		data modify storage mcj:data stack[-1].index set from storage mcj:data localvars.v1.value
		function $(~pointer_handler) with storage mcj:data stack[-1]
		""", """
		# pointer_handler
		$data modify storage mcj:data stack[-1] set from storage mcj:data heap.v$(value).value[$(index)]
		$data remove storage mcj:data heap.v$(value).value[$(index)]
		"""})
	@Override
	public native T remove(int index);
	
	@Override
	public int indexOf(Object o) {
		int len = size();
		for (int i = 0; i < len; i++) {
			if (get(i) == o)
				return i;
		}
		return -1;
	}
	
	@Override
	public int lastIndexOf(Object o) {
		for (int i = size() - 1; i >= 0; i--) {
			if (get(i) == o)
				return i;
		}
		return -1;
	}
	
	/**
	 * Equivalent to {@link #iterator()}
	 */
	@Override
	public ArrayIterator<T> listIterator() {
		return new ArrayIterator<>(this, 0);
	}
	
	@Override
	public ArrayIterator<T> listIterator(int index) {
		return new ArrayIterator<>(this, index);
	}
	
	@Override
	public Array<T> subList(int fromIndex, int toIndex) {
		Array<T> output = new Array<>();
		copySubList(output, fromIndex, toIndex);
		return output;
	}
	@MCJNativeImpl({"""
		function mcj:localvars/push_var_to_stack {index:"0"}
		function mcj:heap/getfield {name:"values"}
		data modify storage mcj:data stack[-1].target set from storage mcj:data localvars.v1.value
		execute store result score cmp_a mcj_data run data get storage mcj:data localvars.v2.value
		execute store result score cmp_b mcj_data run data get storage mcj:data localvars.v3.value
		function $(~_copysublist)
		""", """
		# _copysublist
		execute if score cmp_a mcj_data = cmp_b mcj_data run return 0
		execute store result storage mcj:data stack[-1].cmp_a int 1 run scoreboard players get cmp_a mcj_data
		function $(~pointer_handler) with storage mcj:data stack[-1]
		scoreboard players add cmp_a mcj_data 1
		function $(~_copysublist)
		""", """
		# pointer_handler
		$data modify storage mcj:data heap.v$(target).value append from storage mcj:data heap.v$(value).value[$(cmp_a)]
		"""})
	private native void copySubList(Array<T> target, int fromIndex, int toIndex);
	
}
