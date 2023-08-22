package com.luneruniverse.minecraft.mcj;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.function.UnaryOperator;

public class ImplForTracker {
	
	private static record FullPath(String namespace, String path, String formatted) {
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
	 * @param path package/Class$Nested
	 * @param namespace namespace
	 * @param newPath package/Class$Nested
	 */
	public void track(String path, String namespace, String newPath, boolean isExpanded) {
		FullPath fullPath = new FullPath(namespace, newPath);
		if (pathToFullPath.put(path, fullPath) != null)
			throw new MCJException("Duplicate implementation: " + path);
		if (!path.equals(newPath) && pathToFullPath.put(newPath, fullPath) != null)
			throw new MCJException("Duplicate implementation: " + newPath);
		if (formattedToIsExpanded.put(fullPath.formatted(), isExpanded) != null)
			throw new MCJException("Duplicate implementation: " + fullPath.formatted());
	}
	
	/**
	 * @param path package/Class$Nested
	 * @return namespace:package_package/class_class/class_nested
	 */
	public String getClassPath(String path) {
		FullPath output = pathToFullPath.get(path);
		if (output == null)
			throw new MCJException("Missing implementation for '" + path + "'");
		return output.formatted();
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
	 * @param path package/Class$Nested
	 * @param pathTransformer package/Class$Nested -> package/Class$Nested
	 * @return namespace:package_package/class_class/class_nested
	 */
	public String getClassPath(String path, UnaryOperator<String> pathTransformer) {
		FullPath output = pathToFullPath.get(path);
		if (output == null)
			throw new MCJException("Missing implementation for '" + path + "'");
		return output.namespace() + ":" + MCJUtil.formatClassPath(pathTransformer.apply(output.path()));
	}
	
	/**
	 * @param classPath namespace:package_package/class_class/class_nested
	 * @return If the class is using expanded paths
	 */
	public boolean isExpanded(String classPath) {
		return formattedToIsExpanded.get(classPath);
	}
	
}
