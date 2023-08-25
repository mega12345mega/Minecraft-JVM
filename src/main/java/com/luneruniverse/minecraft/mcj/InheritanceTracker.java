package com.luneruniverse.minecraft.mcj;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.objectweb.asm.Opcodes;

public class InheritanceTracker {
	
	private final ClassTree tree;
	private final Map<String, Map<String, Integer>> fields;
	private final Map<String, Map<String, Integer>> methods;
	
	public InheritanceTracker() {
		tree = new ClassTree();
		fields = new HashMap<>();
		methods = new HashMap<>();
		
		fields.put("java/lang/Object", new HashMap<>());
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
		fields.put(clazz, new HashMap<>());
		methods.put(clazz, new HashMap<>());
	}
	
	/**
	 * @param clazz package/Class$Nested
	 * @param name The field name
	 * @param descriptor The field descriptor
	 * @param access The field access flags
	 */
	public void trackField(String clazz, String name, String descriptor, int access) {
		if (!tree.contains(clazz))
			throw new MCJException("Cannot track the field '" + name + ":" + descriptor + "' in the untracked class '" + clazz + "'");
		fields.get(clazz).put(name + ":" + descriptor, access);
	}
	
	/**
	 * @param clazz package/Class$Nested
	 * @param name The method name
	 * @param descriptor The method descriptor
	 * @param access The method access flags
	 */
	public void trackMethod(String clazz, String name, String descriptor, int access) {
		if (!tree.contains(clazz))
			throw new MCJException("Cannot track the method '" + name + descriptor + "' in the untracked class '" + clazz + "'");
		if (methods.get(clazz).put(name + descriptor, access) != null)
			throw new MCJException("Duplicate method '" + name + descriptor + "' in the class '" + clazz + "'");
	}
	
	/**
	 * @param referrer package/Class$Nested - the class requesting the field
	 * @param clazz package/Class$Nested - the class (or one of its subclasses) holding the field
	 * @param name The field name
	 * @param descriptor The field descriptor
	 * @return The class containing the declaration for the static field in the format package/Class$Nested
	 */
	public String getStaticFieldDec(String referrer, String clazz, String name, String descriptor) {
		Map.Entry<String, Integer> output = getFieldDec(referrer, clazz, name, descriptor);
		if (output == null)
			throw new MCJException("Missing field '" + name + ":" + descriptor + "' in class '" + clazz + "'");
		if (!MCJUtil.hasOpcode(output.getValue(), Opcodes.ACC_STATIC))
			throw new MCJException("Expected field '" + name + ":" + descriptor + "' in class '" + clazz + "' to be static");
		return output.getKey();
	}
	
	/**
	 * @param referrer package/Class$Nested - the class requesting the field
	 * @param clazz package/Class$Nested - the class (or one of its subclasses) holding the field
	 * @param name The field name
	 * @param descriptor The field descriptor
	 * @return The class containing the declaration for the non-static field in the format package/Class$Nested
	 */
	public String getInstanceFieldDec(String referrer, String clazz, String name, String descriptor) {
		Map.Entry<String, Integer> output = getFieldDec(referrer, clazz, name, descriptor);
		if (output == null)
			throw new MCJException("Missing field '" + name + ":" + descriptor + "' in class '" + clazz + "'");
		if (MCJUtil.hasOpcode(output.getValue(), Opcodes.ACC_STATIC))
			throw new MCJException("Expected field '" + name + ":" + descriptor + "' in class '" + clazz + "' to be non-static");
		return output.getKey();
	}
	
	private Map.Entry<String, Integer> getFieldDec(String referrer, String clazz, String name, String descriptor) {
		if (clazz.equals("java/lang/Object"))
			return null;
		Integer access = fields.get(clazz).get(name + ":" + descriptor);
		if (access != null)
			return canAccess(referrer, clazz, access) ? Map.entry(clazz, access) : null;
		
		List<String> superTypes = new ArrayList<>();
		superTypes.add(tree.getSuperClass(clazz));
		superTypes.addAll(tree.getSuperInterfaces(clazz));
		Map.Entry<String, Integer> output = null;
		for (String superType : superTypes) {
			Map.Entry<String, Integer> dec = getFieldDec(referrer, superType, name, descriptor);
			if (dec != null) {
				if (output == null)
					output = dec;
				else if (!output.getKey().equals(dec.getKey())) // IBase(field), IBaseSub extends IBase, Clazz implements IBase, IBaseSub
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
	
	private boolean canAccess(String referrer, String clazz, int access) {
		if (MCJUtil.hasOpcode(access, Opcodes.ACC_PUBLIC))
			return true;
		if (MCJUtil.hasOpcode(access, Opcodes.ACC_PRIVATE)) {
			int i = referrer.indexOf('$');
			if (i != -1)
				referrer = referrer.substring(0, i);
			i = clazz.indexOf('$');
			if (i != -1)
				clazz = clazz.substring(0, i);
			return referrer.equals(clazz); // Same outer class
		}
		if (referrer.substring(0, referrer.indexOf('/') + 1).equals(clazz.substring(0, clazz.indexOf('/')) + 1))
			return true; // Same package
		return MCJUtil.hasOpcode(access, Opcodes.ACC_PROTECTED) &&
				(clazz.equals(referrer) || tree.isSuperClass(clazz, referrer));
	}
	
}
