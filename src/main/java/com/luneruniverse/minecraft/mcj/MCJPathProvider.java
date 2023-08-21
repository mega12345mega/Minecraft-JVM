package com.luneruniverse.minecraft.mcj;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class MCJPathProvider {
	
	public static class ClassPathProvider extends MCJPathProvider {
		private final File classFunctions;
		private final String classFunctionsPath;
		private final String className;
		private final String originalClassName;
		private final boolean isMainClass;
		public ClassPathProvider(MCJPathProvider mainProvider, File classFunctions, String classFunctionsPath,
				String className, String originalClassName, boolean isMainClass) {
			super(mainProvider);
			this.classFunctions = classFunctions;
			this.classFunctionsPath = classFunctionsPath;
			this.className = className;
			this.originalClassName = originalClassName;
			this.isMainClass = isMainClass;
		}
		protected ClassPathProvider(ClassPathProvider provider) {
			super(provider);
			this.classFunctions = provider.classFunctions;
			this.classFunctionsPath = provider.classFunctionsPath;
			this.className = provider.className;
			this.originalClassName = provider.originalClassName;
			this.isMainClass = provider.isMainClass;
		}
		public File getClassFunctions() {
			return classFunctions;
		}
		public String getClassFunctionsPath() {
			return classFunctionsPath;
		}
		public String getClassName() {
			return className;
		}
		public String getOriginalClassName() {
			return originalClassName;
		}
		public boolean isMainClass() {
			return isMainClass;
		}
		@Override
		public ClassPathProvider changeNamespace(String namespace) {
			return new ClassPathProvider(super.changeNamespace(namespace),
					classFunctions, classFunctionsPath, className, originalClassName, isMainClass);
		}
		@Override
		public ClassPathProvider changeExpandedPaths(boolean expandedPaths) {
			return new ClassPathProvider(super.changeExpandedPaths(expandedPaths),
					classFunctions, classFunctionsPath, className, originalClassName, isMainClass);
		}
		public ClassPathProvider changeClassPath(File classFunctions, String classFunctionsPath, String className) {
			return new ClassPathProvider(this, classFunctions, classFunctionsPath, className, originalClassName, isMainClass);
		}
		public String getChangedClassPath(String path) {
			if (!originalClassName.equals(path))
				return path;
			return className;
		}
	}
	
	public static class MethodPathProvider extends ClassPathProvider {
		private final File methodFunctions;
		private final String methodFunctionsPath;
		private final int paramCount;
		private final boolean isMainMethod;
		private final boolean isStaticMethod;
		private final boolean isNativeMethod;
		private final boolean isVoidMethod;
		public MethodPathProvider(ClassPathProvider classProvider, File methodFunctions, String methodFunctionsPath,
				int paramCount, boolean isMainMethod, boolean isStaticMethod, boolean isNativeMethod, boolean isVoidMethod) {
			super(classProvider);
			this.methodFunctions = methodFunctions;
			this.methodFunctionsPath = methodFunctionsPath;
			this.paramCount = paramCount;
			this.isMainMethod = isMainMethod;
			this.isStaticMethod = isStaticMethod;
			this.isNativeMethod = isNativeMethod;
			this.isVoidMethod = isVoidMethod;
		}
		public File getMethodFunctions() {
			return methodFunctions;
		}
		public String getMethodFunctionsPath() {
			return methodFunctionsPath;
		}
		public int getParamCount() {
			return paramCount;
		}
		public boolean isMainMethod() {
			return isMainMethod;
		}
		public boolean isStaticMethod() {
			return isStaticMethod;
		}
		public boolean isNativeMethod() {
			return isNativeMethod;
		}
		public boolean isVoidMethod() {
			return isVoidMethod;
		}
		@Override
		public MethodPathProvider changeNamespace(String namespace) {
			return new MethodPathProvider(super.changeNamespace(namespace),
					methodFunctions, methodFunctionsPath, paramCount, isMainMethod, isStaticMethod, isNativeMethod, isVoidMethod);
		}
		@Override
		public MethodPathProvider changeExpandedPaths(boolean expandedPaths) {
			return new MethodPathProvider(super.changeExpandedPaths(expandedPaths),
					methodFunctions, methodFunctionsPath, paramCount, isMainMethod, isStaticMethod, isNativeMethod, isVoidMethod);
		}
		@Override
		public MethodPathProvider changeClassPath(File classFunctions, String classFunctionsPath, String className) {
			return new MethodPathProvider(super.changeClassPath(classFunctions, classFunctionsPath, className),
					new File(classFunctions, methodFunctions.getName()),
					classFunctionsPath + methodFunctionsPath.substring(methodFunctionsPath.lastIndexOf('/')),
					paramCount, isMainMethod, isStaticMethod, isNativeMethod, isVoidMethod);
		}
	}
	
	
	private final File datapack;
	private final String namespace;
	private final String mainClass; // package/Class$Nested
	private final boolean expandedPaths;
	
	private final File data;
	private final File mcjData;
	private final File mcjFunctions;
	private final File compiledData;
	private final File compiledFunctions;
	
	public MCJPathProvider(File datapack, String namespace, String mainClass, boolean expandedPaths) {
		this.datapack = datapack;
		this.namespace = namespace;
		this.mainClass = mainClass;
		this.expandedPaths = expandedPaths;
		
		this.data = new File(datapack, "data");
		this.mcjData = new File(data, "mcj");
		this.mcjFunctions = new File(mcjData, "functions");
		this.compiledData = new File(data, namespace);
		this.compiledFunctions = new File(compiledData, "functions");
	}
	protected MCJPathProvider(MCJPathProvider provider) {
		this.datapack = provider.datapack;
		this.namespace = provider.namespace;
		this.mainClass = provider.mainClass;
		this.expandedPaths = provider.expandedPaths;
		
		this.data = provider.data;
		this.mcjData = provider.mcjData;
		this.mcjFunctions = provider.mcjFunctions;
		this.compiledData = provider.compiledData;
		this.compiledFunctions = provider.compiledFunctions;
	}
	
	public File getDatapack() {
		return datapack;
	}
	public String getNamespace() {
		return namespace;
	}
	public String getMainClass() {
		return mainClass;
	}
	public boolean isExpandedPaths() {
		return expandedPaths;
	}
	
	public File getData() {
		return data;
	}
	public File getMcjData() {
		return mcjData;
	}
	public File getMcjFunctions() {
		return mcjFunctions;
	}
	public File getCompiledData() {
		return compiledData;
	}
	public File getCompiledFunctions() {
		return compiledFunctions;
	}
	
	public MCJPathProvider changeNamespace(String namespace) {
		return new MCJPathProvider(datapack, namespace, mainClass, expandedPaths);
	}
	public MCJPathProvider changeExpandedPaths(boolean expandedPaths) {
		return new MCJPathProvider(datapack, namespace, mainClass, expandedPaths);
	}
	
	public void writeToActualFile(File file, CharSequence contents) throws IOException {
		Files.createDirectories(file.getAbsoluteFile().getParentFile().toPath());
		Files.writeString(file.toPath(), contents);
	}
	public void writeToFile(File file, CharSequence contents) throws IOException {
		Path path;
		if (expandedPaths) {
			path = file.toPath();
		} else {
			Path datapackRelPath = datapack.toPath().relativize(file.toPath());
			Path datapackRelFunctions = datapackRelPath.subpath(0, 3);
			Path functionsRelPath = datapackRelPath.subpath(3, datapackRelPath.getNameCount());
			String functionsRelPathStr = functionsRelPath.toString();
			functionsRelPathStr = functionsRelPathStr.substring(0, functionsRelPathStr.length() - ".mcfunction".length())
					.replace(functionsRelPath.getFileSystem().getSeparator(), "/");
			path = datapack.toPath().resolve(datapackRelFunctions).resolve(MCJUtil.md5(functionsRelPathStr) + ".mcfunction");
			
			contents = "# " + functionsRelPathStr + "\n" + contents;
		}
		Files.createDirectories(path.getParent());
		Files.writeString(path, contents);
	}
	public String getActualFunctionPath(String path) {
		if (expandedPaths)
			return path;
		int i = path.indexOf(':') + 1;
		return path.substring(0, i) + MCJUtil.md5(path.substring(i));
	}
	
}
