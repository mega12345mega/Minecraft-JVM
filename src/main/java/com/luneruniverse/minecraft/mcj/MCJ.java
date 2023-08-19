package com.luneruniverse.minecraft.mcj;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.objectweb.asm.ClassReader;

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
		if (args.length >= 4 && args[3].equalsIgnoreCase("-delete") && datapack.exists()) {
			deleteFile(datapack);
		}
		new MCJ(jar, namespace).compileTo(datapack);
	}
	private static void deleteFile(File file) {
		if (file.isDirectory()) {
			for (File subfile : file.listFiles())
				deleteFile(subfile);
		}
		file.delete();
	}
	
	
	private final File jar;
	private final String namespace;
	
	public MCJ(File jar, String namespace) {
		this.jar = jar;
		this.namespace = namespace.toLowerCase();
	}
	
	public void compileTo(File datapack) throws IOException {
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
		
		copyMCJDatapackTo(datapack);
		
		String mainClass = null;
		try (ZipInputStream zipIn = new ZipInputStream(new FileInputStream(jar))) {
			ZipEntry entry;
			while ((entry = zipIn.getNextEntry()) != null) {
				if (!entry.isDirectory() && entry.getName().equals("META-INF/MANIFEST.MF")) {
					String contents = new String(zipIn.readAllBytes()).replace("\r", "");
					for (String line : contents.split("\n")) {
						if (line.startsWith("Main-Class: ")) {
							mainClass = line.substring("Main-Class: ".length());
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
		
		try (ZipInputStream zipIn = new ZipInputStream(new FileInputStream(jar))) {
			ZipEntry entry;
			while ((entry = zipIn.getNextEntry()) != null) {
				if (!entry.isDirectory()) {
					String name = new File(entry.getName()).getName();
					if (name.endsWith(".class") && !name.equals("module-info.class") && !name.equals("package-info.class"))
						compileClass(functions, mainClass, new ClassReader(zipIn), entry.getName());
				}
			}
		}
	}
	private void compileClass(File functions, String mainClass, ClassReader reader, String name) {
		try {
			reader.accept(new MCJClassVisitor(functions, mainClass), 0);
		} catch (MCJException e) {
			throw new MCJException("Error compiling class '" + name + "'", e);
		}
	}
	
	private void copyMCJDatapackTo(File datapack) throws IOException {
		for (String path : new String(MCJ.class.getResourceAsStream("/mcj_datapack.txt").readAllBytes())
				.replace("\r", "").split("\n")) {
			path = path.substring(2).replace('\\', '/');
			File file = new File(datapack, path);
			Files.createDirectories(file.getAbsoluteFile().getParentFile().toPath());
			Files.write(file.toPath(), MCJ.class.getResourceAsStream("/" + path).readAllBytes());
		}
	}
	
}
