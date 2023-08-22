package com.luneruniverse.minecraft.mcj;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class InheritanceTracker {
	
	private final ClassTree tree;
	private final Map<String, List<String>> methods;
	
	public InheritanceTracker() {
		tree = new ClassTree();
		methods = new HashMap<>();
	}
	
	public void link() {
		tree.link();
	}
	
	/**
	 * @param clazz package/Class$Nested
	 * @param superClass package/Class$Nested
	 * @param superInterfaces [package/Class$Nested
	 * @param name The method name
	 * @param descriptor The method descriptor
	 */
	public void track(String clazz, String superClass, String[] superInterfaces, String name, String descriptor) {
		methods.computeIfAbsent(clazz, key -> {
			tree.add(clazz, superClass);
			return new ArrayList<>();
		}).add(name + descriptor);
	}
	
	/**
	 * @param clazz package/Class$Nested
	 * @param name The method name
	 * @param descriptor The method descriptor
	 * @return If the method is inherited from a superclass or overridden by a subclass
	 */
	public boolean isPolymorphic(String clazz, String name, String descriptor) {
		String nameAndDesc = name + descriptor;
		return containsMethod(tree.getSuperClassRecursive(clazz), nameAndDesc) ||
				containsMethod(tree.getSubClassesRecursive(clazz), nameAndDesc);
	}
	
	/**
	 * @param clazz package/Class$Nested
	 * @param name The method name
	 * @param descriptor The method descriptor
	 * @return The class containing the implementation for the static method in the format package/Class$Nested
	 */
	public String getStaticMethodImpl(String clazz, String name, String descriptor) {
		String nameAndDesc = name + descriptor;
		String curClass = clazz;
		do {
			if (containsMethod(curClass, nameAndDesc))
				return curClass;
		} while ((curClass = tree.getSuperClass(curClass)) != null);
		throw new MCJException("Missing method '" + nameAndDesc + "' in class '" + clazz + "'");
	}
	
	private boolean containsMethod(List<String> classes, String nameAndDesc) {
		for (String clazz : classes) {
			if (containsMethod(clazz, nameAndDesc))
				return true;
		}
		return false;
	}
	private boolean containsMethod(String clazz, String nameAndDesc) {
		List<String> classMethods = methods.get(clazz);
		return classMethods != null && classMethods.contains(nameAndDesc);
	}
	
}
