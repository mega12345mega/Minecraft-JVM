package com.luneruniverse.minecraft.mcj;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.objectweb.asm.Opcodes;

public class InheritanceTracker {
	
	private final ClassTree tree;
	private final Map<String, List<String>> fields;
	private final Map<String, Map<String, Integer>> methods;
	
	public InheritanceTracker() {
		tree = new ClassTree();
		fields = new HashMap<>();
		methods = new HashMap<>();
		
		fields.put("java/lang/Object", new ArrayList<>());
		methods.put("java/lang/Object", new HashMap<>());
	}
	
	public void link() {
		tree.link();
	}
	
	/**
	 * @param clazz package/Class$Nested
	 * @param superClass package/Class$Nested
	 * @param superInterfaces [package/Class$Nested
	 */
	public void trackClass(String clazz, String superClass, List<String> superInterfaces) {
		tree.add(clazz, superClass, superInterfaces);
		fields.put(clazz, new ArrayList<>());
		methods.put(clazz, new HashMap<>());
	}
	
	/**
	 * @param clazz package/Class$Nested
	 * @param name The field name
	 * @param descriptor The field descriptor
	 */
	public void trackField(String clazz, String name, String descriptor) {
		if (!tree.contains(clazz))
			throw new MCJException("Cannot track the field '" + name + ":" + descriptor + "' in the untracked class '" + clazz + "'");
		fields.get(clazz).add(name + ":" + descriptor);
	}
	
	/**
	 * @param clazz package/Class$Nested
	 * @param name The method name
	 * @param descriptor The method descriptor
	 */
	public void trackMethod(String clazz, String name, String descriptor, int access) {
		if (!tree.contains(clazz))
			throw new MCJException("Cannot track the method '" + name + descriptor + "' in the untracked class '" + clazz + "'");
		if (methods.get(clazz).put(name + descriptor, access) != null)
			throw new MCJException("Duplicate method '" + name + descriptor + "' in the class '" + clazz + "'");
	}
	
	/**
	 * @param clazz package/Class$Nested
	 * @param name The field name
	 * @param descriptor The field descriptor
	 * @return The class containing the declaration for the static field in the format package/Class$Nested
	 */
	public String getStaticFieldDec(String clazz, String name, String descriptor) {
		String output = getStaticFieldDecInternal(clazz, name, descriptor);
		if (output == null)
			throw new MCJException("Missing field '" + name + ":" + descriptor + "' in class '" + clazz + "'");
		return output;
	}
	private String getStaticFieldDecInternal(String clazz, String name, String descriptor) {
		if (clazz.equals("java/lang/Object"))
			return null;
		if (fields.get(clazz).contains(name + ":" + descriptor))
			return clazz;
		
		List<String> superTypes = new ArrayList<>();
		superTypes.add(tree.getSuperClass(clazz));
		superTypes.addAll(tree.getSuperInterfaces(clazz));
		String output = null;
		for (String superType : superTypes) {
			String dec = getStaticFieldDecInternal(superType, name, descriptor);
			if (dec != null) {
				if (output == null)
					output = dec;
				else if (!output.equals(dec)) // IBase(field), IBaseSub extends IBase, Clazz implements IBase, IBaseSub
					throw new MCJException("Ambiguous field '" + name + ":" + descriptor + "' in class '" + clazz + "'");
			}
		}
		return output;
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
				containsMethod(tree.getSuperInterfacesRecursive(clazz), nameAndDesc) ||
				containsMethod(tree.getSubTypesRecursive(clazz), nameAndDesc);
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
			if (methods.get(curClass).containsKey(nameAndDesc))
				return curClass;
		} while ((curClass = tree.getSuperClass(curClass)) != null);
		throw new MCJException("Missing method '" + nameAndDesc + "' in class '" + clazz + "'");
	}
	
	/**
	 * @param clazz package/Class$Nested
	 * @return If the interface does not contain any methods that are both default and non-static
	 */
	public boolean isInterfaceInitSkippable(String clazz) {
		return methods.get(clazz).entrySet().stream().noneMatch(entry ->
				!MCJUtil.hasOpcode(entry.getValue(), Opcodes.ACC_ABSTRACT) &&
				!MCJUtil.hasOpcode(entry.getValue(), Opcodes.ACC_STATIC));
	}
	
	private boolean containsMethod(Collection<String> classes, String nameAndDesc) {
		for (String clazz : classes) {
			if (methods.get(clazz).containsKey(nameAndDesc))
				return true;
		}
		return false;
	}
	
}
