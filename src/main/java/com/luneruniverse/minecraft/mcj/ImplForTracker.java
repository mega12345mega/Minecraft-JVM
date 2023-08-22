package com.luneruniverse.minecraft.mcj;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class ImplForTracker {
	
	/**
	 * package/Class$Nested -> namespace:package_package/class_class/class_nested
	 */
	private final Map<String, String> pathToFullPath;
	/**
	 * namespace:package_package/class_class/class_nested -> true/false
	 */
	private final Map<String, Boolean> fullPathToIsExpanded;
	
	public ImplForTracker() {
		pathToFullPath = new HashMap<>();
		fullPathToIsExpanded = new HashMap<>();
	}
	
	/**
	 * @param path package/Class$Nested
	 * @param namespace namespace
	 * @param newPath package/Class$Nested
	 */
	public void track(String path, String namespace, String newPath, boolean isExpanded) {
		String fullPath = namespace + ":" + MCJUtil.formatClassPath(newPath);
		if (pathToFullPath.put(path, fullPath) != null)
			throw new MCJException("Duplicate implementation: " + path);
		if (!path.equals(newPath) && pathToFullPath.put(newPath, fullPath) != null)
			throw new MCJException("Duplicate implementation: " + newPath);
		if (fullPathToIsExpanded.put(fullPath, isExpanded) != null)
			throw new MCJException("Duplicate implementation: " + fullPath);
	}
	
	/**
	 * @param path package/Class$Nested
	 * @return namespace:package_package/class_class/class_nested
	 */
	public String getClassPath(String path) {
		String output = pathToFullPath.get(path);
		if (output == null)
			throw new MCJException("Missing implementation for '" + path + "'");
		return output;
	}
	
	/**
	 * @param datapack Datapack's root directory
	 * @param path package/Class$Nested
	 * @return &lt;datapack&gt;/data/namespace/functions/package_package/class_class/class_nested
	 */
	public File getClassFolder(File datapack, String path) {
		String classPath = getClassPath(path);
		return new File(datapack, "data/" + classPath.replace(":", "/functions/"));
	}
	
	/**
	 * @param classPath namespace:package_package/class_class/class_nested
	 * @return If the class is using expanded paths
	 */
	public boolean isExpanded(String classPath) {
		return fullPathToIsExpanded.get(classPath);
	}
	
}
