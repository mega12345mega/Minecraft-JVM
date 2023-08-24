package com.luneruniverse.minecraft.mcj;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.objectweb.asm.ClassReader;

import com.luneruniverse.minecraft.mcj.MCJPathProvider.ClassPathProvider;

/**
 * Check for macros missing the starting $ with this RegEx: /^\s*[^$\s].*\$\([^~].*$/gm<br>
 * Check for returns missing a value with this RegEx: /return(?! )/g
 */
public class MCJ {
	
	public static void main(String[] args) throws IOException {
		if (args.length < 3) {
			System.out.println("Usage: java -jar MCJ.jar file/to/compile.jar path/to/world/datapacks/datapack namespace [-delete]");
			return;
		}
		
		File jar = new File(args[0]);
		if (!jar.exists()) {
			System.out.println("Unable to find file: " + jar.getAbsolutePath());
			return;
		}
		
		File datapack = new File(args[1]).getAbsoluteFile();
		if (!datapack.getParentFile().exists()) {
			System.out.println("Unable to find datapacks folder: " + datapack.getParentFile().getAbsolutePath());
			return;
		}
		
		String namespace = args[2];
		if (!namespace.equals(namespace.toLowerCase())) {
			System.out.println("Namespaces must be lowercase");
			return;
		}
		if (namespace.equals("mcj")) {
			System.out.println("The namespace cannot be 'mcj', since this conflicts with part of the native API");
			return;
		}
		
		Set<String> flags = new HashSet<>();
		for (int i = 3; i < args.length; i++) {
			flags.add(args[i].toLowerCase());
		}
		if (flags.contains("-delete") && datapack.exists()) {
			deleteFile(datapack);
		}
		boolean expandedPaths = flags.contains("-expandedpaths");
		boolean sysout = flags.contains("-sysout");
		
		new MCJ(jar, namespace, expandedPaths).compileTo(datapack, line -> {
			if (sysout)
				System.out.println(line);
		});
	}
	private static void deleteFile(File file) throws IOException {
		if (file.isDirectory()) {
			for (File subfile : file.listFiles())
				deleteFile(subfile);
		}
		Files.delete(file.toPath());
	}
	
	
	private final File jar;
	private final String namespace;
	private final boolean expandedPaths;
	
	public MCJ(File jar, String namespace, boolean expandedPaths) {
		this.jar = jar;
		this.namespace = namespace.toLowerCase();
		this.expandedPaths = expandedPaths;
	}
	
	public void compileTo(File datapack, Consumer<String> logger) throws IOException {
		logger.accept("Compiling " + jar.getAbsolutePath() + " -> " + datapack.getAbsolutePath());
		long startTime = System.currentTimeMillis();
		
		Files.createDirectory(datapack.toPath());
		File functions = new File(datapack, "data/" + namespace + "/functions");
		Files.createDirectories(functions.toPath());
		
		File packMcMetaFile = new File(datapack, "pack.mcmeta");
		Files.writeString(packMcMetaFile.toPath(), """
				{
					"pack": {
						"pack_format": 16,
						"description": "Compiled by Minecraft JVM (MCJ)",
						"credit": "Compiled by Minecraft JVM (MCJ), a tool created by mega12345mega!"
					}
				}
				""");
		
		logger.accept("Initializing: Finding META-INF/MANIFEST.MF");
		String mainClass = null;
		try (ZipInputStream zipIn = new ZipInputStream(new FileInputStream(jar))) {
			ZipEntry entry;
			while ((entry = zipIn.getNextEntry()) != null) {
				if (!entry.isDirectory() && entry.getName().equals("META-INF/MANIFEST.MF")) {
					String contents = new String(zipIn.readAllBytes()).replace("\r", "");
					for (String line : contents.split("\n")) {
						if (line.startsWith("Main-Class: ")) {
							mainClass = line.substring("Main-Class: ".length()).replace('.', '/');
							break;
						}
					}
					break;
				}
			}
		}
		if (mainClass == null) {
			throw new MCJException("Missing META-INF/MANIFEST.MF with Main-Class");
		}
		
		MCJPathProvider provider = new MCJPathProvider(datapack, namespace, mainClass, expandedPaths,
				new ImplForTracker(), new InheritanceTracker(), new ClinitTracker());
		Map<String, String> entryClassNames = new HashMap<>();
		Set<String> ignoredEntries = new HashSet<>();
		
		logger.accept("Initializing: Loading class information");
		processJar((in, entry) -> {
			try {
				new ClassReader(in).accept(new MCJClassInitVisitor(provider,
						path -> entryClassNames.put(entry.getName(), path),
						() -> ignoredEntries.add(entry.getName())), 0);
			} catch (Exception e) {
				throw new MCJException("Error initializing class '" + entry.getName() + "'", e);
			}
		});
		provider.getInheritanceTracker().link();
		
		// Refers to information from the MCJClassInitVisitor
		logger.accept("Compiling: Copying in the default MCJ data");
		copyMCJDatapackTo(provider);
		
		logger.accept("Compiling: Provided .jar");
		processJar((in, entry) -> {
			if (ignoredEntries.contains(entry.getName()))
				return;
			try {
				logger.accept(" - " + entry.getName());
				
				String name = entryClassNames.get(entry.getName());
				MCJPathProvider renamedProvider = provider.changeNamespace(provider.getImplForTracker().getNamespace(name));
				ClassPathProvider classProvider = new ClassPathProvider(renamedProvider,
						provider.getImplForTracker().getClassFolder(provider.getDatapack(), name),
						provider.getImplForTracker().getClassPath(name),
						provider.getImplForTracker().getName(name),
						renamedProvider.getMainClass().equals(name));
				
				new ClassReader(in).accept(new MCJClassVisitor(classProvider), 0);
			} catch (Exception e) {
				throw new MCJException("Error compiling class '" + entry.getName() + "'", e);
			}
		});
		
		long endTime = System.currentTimeMillis();
		logger.accept("Completed: " + (endTime - startTime) + "ms");
	}
	private void processJar(BiConsumer<ZipInputStream, ZipEntry> classProcessor) throws IOException {
		try (ZipInputStream zipIn = new ZipInputStream(new FileInputStream(jar))) {
			ZipEntry entry;
			while ((entry = zipIn.getNextEntry()) != null) {
				if (!entry.isDirectory()) {
					String name = new File(entry.getName()).getName();
					if (name.endsWith(".class") && !name.equals("module-info.class") && !name.equals("package-info.class"))
						classProcessor.accept(zipIn, entry);
				}
			}
		}
	}
	
	private void copyMCJDatapackTo(MCJPathProvider provider) throws IOException {
		for (String path : new String(MCJ.class.getResourceAsStream("/mcj_datapack.txt").readAllBytes())
				.replace("\r", "").split("\n")) {
			path = path.substring(2).replace('\\', '/');
			File file = new File(provider.getDatapack(), path);
			Files.createDirectories(file.getAbsoluteFile().getParentFile().toPath());
			Files.writeString(file.toPath(),
					MCJUtil.processNativeFile(provider, new String(MCJ.class.getResourceAsStream("/" + path).readAllBytes())));
		}
	}
	
}
