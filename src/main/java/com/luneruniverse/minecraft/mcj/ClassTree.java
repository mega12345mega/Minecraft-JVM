package com.luneruniverse.minecraft.mcj;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ClassTree {
	
	private final Map<String, String> superClasses;
	private final Map<String, List<String>> subClasses;
	private boolean linked;
	
	public ClassTree() {
		superClasses = new HashMap<>();
		subClasses = new HashMap<>();
		superClasses.put("java/lang/Object", null);
	}
	
	public void link() {
		superClasses.forEach((clazz, superClass) -> {
			if (superClass == null) // java/lang/Object
				return;
			if (!superClasses.containsKey(superClass))
				throw new MCJException("Failed to link ClassTree; missing super-class for '" + superClass + "'");
		});
		linked = true;
	}
	private void checkLinked() {
		if (!linked)
			throw new MCJException("Cannot get information from an unlinked ClassTree!");
	}
	
	public void add(String clazz, String superClass) {
		linked = false;
		if (superClasses.put(clazz, superClass) != null)
			throw new MCJException("Duplicate class: " + clazz);
		subClasses.computeIfAbsent(superClass, key -> new ArrayList<>()).add(clazz);
	}
	
	public String getSuperClass(String clazz) {
		checkLinked();
		return superClasses.get(clazz);
	}
	public List<String> getSuperClassRecursive(String clazz) {
		checkLinked();
		List<String> output = new ArrayList<>();
		while ((clazz = superClasses.get(clazz)) != null)
			output.add(clazz);
		return output;
	}
	
	public List<String> getSubClasses(String clazz) {
		checkLinked();
		return subClasses.computeIfAbsent(clazz, key -> new ArrayList<>());
	}
	public List<String> getSubClassesRecursive(String clazz) {
		checkLinked();
		List<String> output = new ArrayList<>();
		for (String subClass : subClasses.computeIfAbsent(clazz, key -> new ArrayList<>())) {
			output.add(subClass);
			output.addAll(getSubClassesRecursive(subClass));
		}
		return output;
	}
	
}
