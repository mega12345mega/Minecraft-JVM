package com.luneruniverse.minecraft.mcj;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.objectweb.asm.Opcodes;

public class InheritanceTracker {
	
	public static record MethodCall(boolean overriden, boolean isPublic,
			/** overriden ? &ltpublic&gt; | package : class */ String classOrPackage) {}
	
	private class VTables {
		
		private final String clazz;
		private final Map<String, String> publicTable;
		private final Map<String, Map<String, String>> packageTables;
		private final Map<String, Map<Boolean, Boolean>> overridenStatus;
		
		public VTables(String clazz) {
			this.clazz = clazz;
			publicTable = new HashMap<>();
			packageTables = new HashMap<>();
			overridenStatus = new HashMap<>();
		}
		
		public VTables generateTables() {
			List<String> superClasses = tree.getSuperClassRecursive(clazz);
			superClasses.add(0, clazz);
			Map<String, String> prevMethods = new HashMap<>();
			for (String iterClass : superClasses) {
				Map<String, String> packageTable = packageTables.computeIfAbsent(
						iterClass.substring(0, iterClass.lastIndexOf('/') + 1), key -> new HashMap<>());
				for (Map.Entry<String, Integer> method : methods.get(iterClass).entrySet()) {
					if (MCJUtil.hasOpcode(method.getValue(), Opcodes.ACC_PRIVATE) ||
							MCJUtil.hasOpcode(method.getValue(), Opcodes.ACC_STATIC))
						continue;
					String prevMethod = prevMethods.put(method.getKey(), iterClass);
					if (packageTable.containsKey(method.getKey()))
						continue;
					boolean override = (prevMethod == null ? false : canAccess(prevMethod, iterClass, method.getValue()));
					String implClass = (override ? publicTable.get(method.getKey()) : iterClass);
					packageTable.put(method.getKey(), implClass);
					if (MCJUtil.hasOpcode(method.getValue(), Opcodes.ACC_PROTECTED) ||
							MCJUtil.hasOpcode(method.getValue(), Opcodes.ACC_PUBLIC)) {
						publicTable.putIfAbsent(method.getKey(), implClass);
					}
				}
			}
			Map<String, String> interfaceMethods = new HashMap<>();
			for (String iterClass : tree.getSuperInterfacesRecursive(clazz)) {
				for (Map.Entry<String, Integer> method : methods.get(iterClass).entrySet()) {
					if (MCJUtil.hasOpcode(method.getValue(), Opcodes.ACC_STATIC))
						continue;
					interfaceMethods.compute(method.getKey(), (nameAndDesc, prevInterface) ->
							prevInterface == null || tree.isSuperInterface(prevInterface, iterClass) ? iterClass : prevInterface);
				}
			}
			for (Map.Entry<String, String> method : interfaceMethods.entrySet()) {
				publicTable.putIfAbsent(method.getKey(), method.getValue());
			}
			return this;
		}
		
		public boolean isPublic(String name, String descriptor) {
			return publicTable.containsKey(name + descriptor);
		}
		
		public boolean isOverriden(String name, String descriptor, boolean isPublic) {
			String nameAndDesc = name + descriptor;
			return overridenStatus.computeIfAbsent(nameAndDesc, key -> new HashMap<>()).computeIfAbsent(isPublic, key -> {
				Integer access = methods.get(clazz).get(nameAndDesc);
				if (access != null && MCJUtil.hasOpcode(access, Opcodes.ACC_PRIVATE))
					return false;
				String packageName = isPublic ? null : clazz.substring(0, clazz.lastIndexOf('/') + 1);
				for (String subType : tree.getSubTypesRecursive(clazz)) {
					VTables tables = vtables.get(subType);
					if (isPublic) {
						if (!tables.publicTable.get(nameAndDesc).equals(clazz))
							return true;
					} else {
						Map<String, String> packageTable = tables.packageTables.get(packageName);
						if (packageTable != null && !packageTable.get(nameAndDesc).equals(clazz))
							return true;
					}
				}
				return false;
			});
		}
		
		// Assumes isOverriden returns false
		public String getMethodImpl(String referrerPackage, String name, String descriptor, boolean isPublic) {
			String nameAndDesc = name + descriptor;
			if (isPublic)
				return publicTable.get(nameAndDesc);
			Integer access = methods.get(clazz).get(nameAndDesc);
			if (access != null && MCJUtil.hasOpcode(access, Opcodes.ACC_PRIVATE))
				return clazz;
			return packageTables.get(referrerPackage).get(nameAndDesc);
		}
		
	}
	
	private final ClassTree tree;
	private final Map<String, Map<String, Integer>> fields;
	private final Map<String, Map<String, Integer>> methods;
	private final Map<String, VTables> vtables;
	
	public InheritanceTracker() {
		tree = new ClassTree();
		fields = new HashMap<>();
		methods = new HashMap<>();
		vtables = new HashMap<>();
		
		fields.put("java/lang/Object", new HashMap<>());
		methods.put("java/lang/Object", new HashMap<>());
	}
	
	public void link() {
		tree.link();
		
		vtables.clear();
		for (String clazz : tree.getTypes())
			vtables.put(clazz, new VTables(clazz).generateTables());
	}
	
	/**
	 * @param clazz package/Class$Nested
	 * @param superClass package/Class$Nested
	 * @param superInterfaces [package/Class$Nested
	 * @param isInterface If the class is an interface
	 */
	public void trackClass(String clazz, String superClass, List<String> superInterfaces, boolean isInterface) {
		tree.add(clazz, superClass, superInterfaces, isInterface);
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
	 * @param name The method name
	 * @param descriptor The method descriptor
	 * @return The class containing the implementation for the non-static method in the format package/Class$Nested
	 */
	public String getSpecialMethodImpl(String clazz, String name, String descriptor) {
		if (name.equals("<init>"))
			return clazz;
		VTables tables = vtables.get(clazz);
		boolean isPublic = tables.isPublic(name, descriptor);
		String implClass = tables.getMethodImpl(clazz.substring(0, clazz.lastIndexOf('/') + 1), name, descriptor, isPublic);
		if (implClass == null)
			throw new MCJException("Missing method '" + name + descriptor + "' in class '" + clazz + "'");
		return implClass;
	}
	
	/**
	 * @param referrer package/Class$Nested - the class requesting the method
	 * @param clazz package/Class$Nested - the class (or one of its subclasses) holding the method
	 * @param name The method name
	 * @param descriptor The method descriptor
	 * @return Information about how to call the method
	 */
	public MethodCall getInstanceMethodImpl(String referrer, String clazz, String name, String descriptor) {
		VTables tables = vtables.get(clazz);
		boolean isPublic = tables.isPublic(name, descriptor);
		String referrerPackage = referrer.substring(0, referrer.lastIndexOf('/') + 1);
		if (tables.isOverriden(name, descriptor, isPublic))
			return new MethodCall(true, isPublic, isPublic ? "<public>" : referrerPackage);
		String implClass = tables.getMethodImpl(referrerPackage, name, descriptor, isPublic);
		if (implClass == null)
			throw new MCJException("Missing method '" + name + descriptor + "' in class '" + clazz + "'");
		return new MethodCall(false, isPublic, implClass);
	}
	
	/**
	 * @param clazz package/Class$Nested
	 * @param provider The provider
	 * @return {"&lt;public&gt;":{"name":"path",...},"package":{"name":"path",...},...}
	 */
	public String getVTablesNBT(String clazz, MCJPathProvider provider) {
		VTables tables = vtables.get(clazz);
		StringBuilder output = new StringBuilder("{\"<public>\":{");
		for (Map.Entry<String, String> method : tables.publicTable.entrySet())
			appendMethodNBT(output, method, provider);
		if (tables.publicTable.isEmpty())
			output.append('}');
		else
			output.setCharAt(output.length() - 1, '}');
		for (Map.Entry<String, Map<String, String>> iterPackage : tables.packageTables.entrySet()) {
			output.append(",\"");
			output.append(iterPackage.getKey());
			output.append("\":{");
			for (Map.Entry<String, String> method : iterPackage.getValue().entrySet())
				appendMethodNBT(output, method, provider);
			if (iterPackage.getValue().isEmpty())
				output.append('}');
			else
				output.setCharAt(output.length() - 1, '}');
		}
		output.append('}');
		return output.toString();
	}
	private void appendMethodNBT(StringBuilder output, Map.Entry<String, String> method, MCJPathProvider provider) {
		output.append('"');
		output.append(method.getKey());
		output.append("\":\"");
		int descStart = method.getKey().indexOf('(');
		String name = method.getKey().substring(0, descStart);
		String descriptor = method.getKey().substring(descStart);
		output.append(provider.getActualFunctionPath(provider.getImplForTracker().getClassPath(method.getValue()) +
				"/" + MCJUtil.getMethodName(name, descriptor) + "/entry"));
		output.append("\",");
	}
	
	/**
	 * @param clazz package/Class$Nested
	 * @return ["class",...]
	 */
	public String getInstanceOfNBT(String clazz) {
		StringBuilder output = new StringBuilder("[");
		output.append("{value:\"");
		output.append(clazz);
		output.append("\"}");
		for (String superClass : tree.getSuperClassRecursive(clazz)) {
			output.append(",{value:\"");
			output.append(superClass);
			output.append("\"}");
		}
		output.append(']');
		return output.toString();
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
		if (referrer.substring(0, referrer.lastIndexOf('/') + 1).equals(clazz.substring(0, clazz.lastIndexOf('/')) + 1))
			return true; // Same package
		return MCJUtil.hasOpcode(access, Opcodes.ACC_PROTECTED) &&
				(clazz.equals(referrer) || tree.isSuperClass(clazz, referrer));
	}
	
}
