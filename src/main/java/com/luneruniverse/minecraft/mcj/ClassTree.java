package com.luneruniverse.minecraft.mcj;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class ClassTree {
	
	/**
	 * package/Class$Nested -> isInterface
	 */
	private final Map<String, Boolean> classes;
	private final Map<String, String> superClasses;
	private final Map<String, List<String>> superInterfaces;
	private final Map<String, List<String>> subTypes;
	private boolean linked;
	
	public ClassTree() {
		classes = new HashMap<>();
		superClasses = new HashMap<>();
		superInterfaces = new HashMap<>();
		subTypes = new HashMap<>();
		
		classes.put("java/lang/Object", false);
		superClasses.put("java/lang/Object", null);
		superInterfaces.put("java/lang/Object", new ArrayList<>());
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
	
	public void add(String clazz, String superClass, List<String> superInterfaces, boolean isInterface) {
		linked = false;
		if (classes.put(clazz, isInterface) != null)
			throw new MCJException("Duplicate class: " + clazz);
		this.superClasses.put(clazz, superClass);
		this.superInterfaces.put(clazz, superInterfaces);
		subTypes.computeIfAbsent(superClass, key -> new ArrayList<>()).add(clazz);
		for (String superInterface : superInterfaces)
			subTypes.computeIfAbsent(superInterface, key -> new ArrayList<>()).add(clazz);
	}
	
	public boolean contains(String clazz) {
		return classes.containsKey(clazz);
	}
	public Set<String> getTypes() {
		return new HashSet<>(superClasses.keySet());
	}
	public Set<String> getClasses() {
		return classes.entrySet().stream().filter(entry -> entry.getValue() == false)
				.map(Map.Entry::getKey).collect(Collectors.toSet());
	}
	public Set<String> getInterfaces() {
		return classes.entrySet().stream().filter(entry -> entry.getValue() == true)
				.map(Map.Entry::getKey).collect(Collectors.toSet());
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
		String superClass = superClasses.get(clazz);
		if (superClass != null)
			output.addAll(getSuperInterfacesRecursive(superClass));
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
	
	public boolean isSuperClass(String superClass, String subClass) {
		checkLinked();
		while ((subClass = superClasses.get(subClass)) != null) {
			if (superClass.equals(subClass))
				return true;
		}
		return false;
	}
	public boolean isSuperInterface(String superInterface, String subInterface) {
		return getSuperInterfacesRecursive(subInterface).contains(superInterface);
	}
	
}
