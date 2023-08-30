package com.luneruniverse.minecraft.mcj;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class ImplForTracker {
	
	private static record FullPath(String namespace, String name, String path) {
		public FullPath(String namespace, String name) {
			this(namespace, name, namespace + ":" + MCJUtil.formatClassPath(name));
		}
	}
	
	/**
	 * package/Class$Nested -> FullPath
	 */
	private final Map<String, FullPath> pathToFullPath;
	/**
	 * namespace:package_package/class_class/class_nested -> true/false
	 */
	private final Map<String, Boolean> formattedToIsExpanded;
	
	public ImplForTracker() {
		pathToFullPath = new HashMap<>();
		formattedToIsExpanded = new HashMap<>();
	}
	
	/**
	 * @param name package/Class$Nested
	 * @param namespace namespace
	 * @param newName package/Class$Nested
	 */
	public void track(String name, String namespace, String newName, boolean isExpanded) {
		FullPath fullPath = new FullPath(namespace, newName);
		if (pathToFullPath.put(name, fullPath) != null)
			throw new MCJException("Duplicate implementation: " + name);
		if (!name.equals(newName) && pathToFullPath.put(newName, fullPath) != null)
			throw new MCJException("Duplicate implementation: " + newName);
		if (formattedToIsExpanded.put(fullPath.path(), isExpanded) != null)
			throw new MCJException("Duplicate implementation: " + fullPath.path());
	}
	
	/**
	 * @param name package/Class$Nested
	 * @return The namespace for the class implementation
	 */
	public String getNamespace(String name) {
		FullPath output = pathToFullPath.get(name);
		if (output == null)
			throw new MCJException("Missing implementation for '" + name + "'");
		return output.namespace();
	}
	
	/**
	 * @param name package/Class$Nested
	 * @return package/Class$Nested
	 */
	public String getName(String name) {
		FullPath output = pathToFullPath.get(name);
		if (output == null)
			throw new MCJException("Missing implementation for '" + name + "'");
		return output.name();
	}
	
	/**
	 * @param name package/Class$Nested
	 * @return namespace:package_package/class_class/class_nested
	 */
	public String getClassPath(String name) {
		FullPath output = pathToFullPath.get(name);
		if (output == null)
			throw new MCJException("Missing implementation for '" + name + "'");
		return output.path();
	}
	
	/**
	 * @param datapack Datapack's root directory
	 * @param name package/Class$Nested
	 * @return &lt;datapack&gt;/data/namespace/functions/package_package/class_class/class_nested
	 */
	public File getClassFolder(File datapack, String name) {
		String classPath = getClassPath(name);
		return new File(datapack, "data/" + classPath.replace(":", "/functions/"));
	}
	
	/**
	 * @param classPath namespace:package_package/class_class/class_nested
	 * @return If the class is using expanded paths
	 */
	public boolean isExpanded(String classPath) {
		return formattedToIsExpanded.get(classPath);
	}
	
}
