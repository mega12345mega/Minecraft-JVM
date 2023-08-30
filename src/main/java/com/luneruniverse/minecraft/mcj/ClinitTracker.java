package com.luneruniverse.minecraft.mcj;

import java.util.HashMap;
import java.util.Map;

public class ClinitTracker {
	
	private final Map<String, Map<String, Object>> staticFields;
	
	public ClinitTracker() {
		staticFields = new HashMap<>();
	}
	
	/**
	 * @param clazz package/Class$Nested
	 * @param staticFields name:descriptor -> initial value
	 */
	public void trackStaticFields(String clazz, Map<String, Object> staticFields) {
		this.staticFields.put(clazz, staticFields);
	}
	
	/**
	 * @param clazz package/Class$Nested
	 * @return name:descriptor -> initial value
	 */
	public Map<String, Object> getStaticFields(String clazz) {
		Map<String, Object> output = staticFields.get(clazz);
		if (output == null)
			throw new MCJException("Missing static fields for '" + clazz + "'");
		return output;
	}
	
}
