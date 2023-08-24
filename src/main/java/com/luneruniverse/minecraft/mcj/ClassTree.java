package com.luneruniverse.minecraft.mcj;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ClassTree {
	
	private final Map<String, String> superClasses;
	private final Map<String, List<String>> superInterfaces;
	private final Map<String, List<String>> subTypes;
	private boolean linked;
	
	public ClassTree() {
		superClasses = new HashMap<>();
		superInterfaces = new HashMap<>();
		subTypes = new HashMap<>();
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
	
	public void add(String clazz, String superClass, List<String> superInterfaces) {
		linked = false;
		if (superClasses.put(clazz, superClass) != null)
			throw new MCJException("Duplicate class: " + clazz);
		this.superInterfaces.put(clazz, superInterfaces);
		subTypes.computeIfAbsent(superClass, key -> new ArrayList<>()).add(clazz);
		for (String superInterface : superInterfaces)
			subTypes.computeIfAbsent(superInterface, key -> new ArrayList<>()).add(clazz);
	}
	
	public boolean contains(String clazz) {
		return superClasses.containsKey(clazz);
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
	
	public List<String> getSuperInterfaces(String clazz) {
		checkLinked();
		return superInterfaces.get(clazz);
	}
	public Set<String> getSuperInterfacesRecursive(String clazz) {
		checkLinked();
		Set<String> output = new HashSet<>();
		for (String superInterface : superInterfaces.get(clazz)) {
			output.add(superInterface);
			output.addAll(getSuperInterfacesRecursive(superInterface));
		}
		return output;
	}
	
	public List<String> getSubTypes(String clazz) {
		checkLinked();
		return subTypes.computeIfAbsent(clazz, key -> new ArrayList<>());
	}
	public Set<String> getSubTypesRecursive(String clazz) {
		checkLinked();
		Set<String> output = new HashSet<>();
		for (String subType : subTypes.computeIfAbsent(clazz, key -> new ArrayList<>())) {
			output.add(subType);
			output.addAll(getSubTypesRecursive(subType));
		}
		return output;
	}
	
}
