package com.luneruniverse.minecraft.mcj.test;

import com.luneruniverse.minecraft.mcj.api.Array;
import com.luneruniverse.minecraft.mcj.api.IterableMap;
import com.luneruniverse.minecraft.mcj.api.MCJImplFor;
import com.luneruniverse.minecraft.mcj.api.MinecraftServer;

@MCJImplFor("arrays_and_maps:")
public class ArraysAndMaps {
	
	public static void main(String[] args) {
		// Array
		Array<String> array = new Array<>();
		array.add("str1");
		array.add("str2");
		array.add(1, "str1.5");
		
		for (int i = 0; i < array.size(); i++) {
			MinecraftServer.broadcast(array.get(i));
		}
		
		// Map
		IterableMap<String, String> map = new IterableMap<>();
		map.put("lvl1", "foo");
		map.put("lvl2", "bar");
		map.put("lvl3", "foobar");
		
		Array.ArrayIterator<String> i = map.iterator();
		while (i.hasNext()) {
			String key = i.next();
			MinecraftServer.broadcast(key + ": " + (String) map.get(key));
		}
	}
	
}
