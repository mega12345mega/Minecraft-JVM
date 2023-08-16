package com.luneruniverse.minecraft.mcj;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Opcodes;

public class MCJNativeImplAnnotationVisitor extends AnnotationVisitor {
	
	private static class ArrayVisitor extends AnnotationVisitor {
		private final List<String> contents;
		public ArrayVisitor() {
			super(Opcodes.ASM9);
			this.contents = new ArrayList<>();
		}
		@Override
		public void visit(String name, Object value) {
			if (value instanceof String str)
				contents.add(str);
			else
				throw new MCJException("Malformed @MCJNativeImpl; invalid type '" + value.getClass().getName() + "'");
		}
		@Override
		public void visitEnd() {
			if (contents.isEmpty())
				throw new MCJException("Malformed @MCJNativeImpl; the 'value' or 'files' argument cannot be empty");
		}
	}
	
	private final File dir;
	private final String functionPath;
	private ArrayVisitor value;
	private ArrayVisitor files;
	
	public MCJNativeImplAnnotationVisitor(File dir, String functionPath) {
		super(Opcodes.ASM9);
		this.dir = dir;
		this.functionPath = functionPath;
	}
	
	@Override
	public AnnotationVisitor visitArray(String name) {
		return switch (name) {
			case "value" -> value = new ArrayVisitor();
			case "files" -> files = new ArrayVisitor();
			default -> throw new MCJException("Malformed @MCJNativeImpl; unexpected '" + name + "'");
		};
	}
	
	@Override
	public void visitEnd() {
		if ((value == null) == (files == null))
			throw new MCJException("Malformed @MCJNativeImpl; there must be exactly one of the 'value' and 'files' arguments");
		
		Map<String, String> values = new HashMap<>();
		
		if (value != null) {
			values.put("entry", value.contents.get(0));
			for (int i = 1; i < value.contents.size(); i++) {
				String[] helperFile = value.contents.get(i).replace("\r", "").split("\n", 2);
				if (helperFile.length == 0 || !helperFile[0].startsWith("# "))
					throw new MCJException("Invalid value in @MCJNativeImpl; the first line must be '# <file name>'");
				String fileName = processFileName(helperFile[0].substring("# ".length()));
				if (values.put(fileName, helperFile.length < 2 ? "" : helperFile[1]) != null)
					throw new MCJException("Duplicate @MCJNativeImpl file names: " + fileName + " (" + functionPath + ")");
			}
		}
		
		if (files != null) {
			try {
				values.put("entry", Files.readString(new File(files.contents.get(0)).toPath()));
				for (int i = 1; i < files.contents.size(); i++) {
					File helperFileRef = new File(files.contents.get(i));
					String helperFile = Files.readString(helperFileRef.toPath()).replace("\r", "");
					
					int extensionIndex = helperFileRef.getName().lastIndexOf('.');
					String fileName = extensionIndex == -1 ? helperFileRef.getName() :
						helperFileRef.getName().substring(0, extensionIndex);
					String fileContents = helperFile;
					
					if (helperFile.startsWith("# ")) {
						int headerEnd = helperFile.indexOf('\n');
						int contentStart = headerEnd + 1;
						if (headerEnd == -1) {
							headerEnd = helperFile.length();
							contentStart = headerEnd;
						}
						fileName = processFileName(helperFile.substring("# ".length(), headerEnd));
						fileContents = helperFile.substring(contentStart);
					}
					
					if (values.put(fileName, fileContents) != null)
						throw new MCJException("Duplicate @MCJNativeImpl file names: " + fileName + " (" + functionPath + ")");
				}
			} catch (IOException e) {
				throw new MCJException("Error reading a @MCJNativeImpl file", e);
			}
		}
		
		values.forEach((fileName, fileContents) -> {
			try {
				Files.writeString(new File(dir, fileName + ".mcfunction").toPath(), fileContents.replace("$(~METHOD_PATH~)",
						functionPath.substring(0, functionPath.length() - 1)));
			} catch (IOException e) {
				throw new MCJException("Error writing a native file", e);
			}
		});
	}
	
	private String processFileName(String fileName) {
		if (fileName.endsWith(".mcfunction")) {
			fileName = fileName.substring(0, fileName.length() - ".mcfunction".length());
			System.out.println("[Warning] You don't have to specify .mcfunction in the file name comment (" + functionPath + fileName + ")");
		}
		return fileName;
	}
	
}
