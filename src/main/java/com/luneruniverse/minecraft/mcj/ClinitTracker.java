package com.luneruniverse.minecraft.mcj;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ClinitTracker {
	
	private final Map<String, List<String>> uninitStaticFields;
	
	public ClinitTracker() {
		uninitStaticFields = new HashMap<>();
	}
	
	/**
	 * @param clazz package/Class$Nested
	 * @param uninitStaticFields [name:descriptor
	 */
	public void trackUninitStaticFields(String clazz, List<String> uninitStaticFields) {
		this.uninitStaticFields.put(clazz, uninitStaticFields);
	}
	
	/**
	 * @param clazz package/Class$Nested
	 * @return [name:descriptor
	 */
	public List<String> getUninitStaticFields(String clazz) {
		List<String> output = uninitStaticFields.get(clazz);
		if (output == null)
			throw new MCJException("Missing uninitialized static fields for '" + clazz + "'");
		return output;
	}
	
}
