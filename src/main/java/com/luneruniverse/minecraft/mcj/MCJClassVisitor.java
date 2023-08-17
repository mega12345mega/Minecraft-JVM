package com.luneruniverse.minecraft.mcj;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import com.luneruniverse.minecraft.mcj.api.MCJIgnore;
import com.luneruniverse.minecraft.mcj.api.MCJImplFor;

public class MCJClassVisitor extends ClassVisitor {
	
	private class MCJImplForAnnotationVisitor extends AnnotationVisitor {
		public MCJImplForAnnotationVisitor() {
			super(Opcodes.ASM9);
		}
		@Override
		public void visit(String name, Object value) {
			int i = name.indexOf(':');
			if (i != -1) {
				String namespace = name.substring(0, i);
				name = name.substring(i + 1);
				functions = new File(functions.getAbsoluteFile().getParentFile().getParentFile(), namespace + "/functions");
				try {
					Files.createDirectories(functions.toPath());
				} catch (IOException e) {
					throw new MCJException("Error while creating the functions directory", e);
				}
			}
			init((String) value);
		}
	}
	
	private File functions;
	private String mainClass; // package.Class$Nested
	private String name; // package/Class$Nested
	private boolean isIgnored;
	private boolean isMainClass;
	private File datapackClass;
	private String functionPath;
	private boolean isDirCreated;
	
	public MCJClassVisitor(File functions, String mainClass) {
		super(Opcodes.ASM9);
		this.functions = functions;
		this.mainClass = mainClass;
	}
	
	private void init(String name) {
		String thePackage = name.substring(0, name.lastIndexOf('/') + 1);
		if (thePackage.equals("com/luneruniverse/minecraft/mcj/") || // Compile com.luneruniverse.minecraft.mcj.api
				thePackage.startsWith("org/objectweb/asm/")) {
			isIgnored = true;
			return;
		}
		
		this.name = name;
		if (mainClass.replace('.', '/').equals(name))
			isMainClass = true;
		
		StringBuilder path = new StringBuilder();
		String[] parts = name.toLowerCase().split("/");
		for (int i = 0; i < parts.length - 1; i++) {
			path.append("package_");
			path.append(parts[i]);
			path.append('/');
		}
		path.append("class_");
		path.append(parts[parts.length - 1]);
		path.append('/');
		functionPath = path.toString();
		datapackClass = new File(functions, functionPath);
		functionPath = functions.getParentFile().getName() + ":" + functionPath;
	}
	
	@Override
	public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
		init(name);
	}
	
	@Override
	public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
		if (descriptor.equals(MCJIgnore.class.descriptorString()))
			isIgnored = true;
		if (descriptor.equals(MCJImplFor.class.descriptorString()))
			return new MCJImplForAnnotationVisitor();
		return null;
	}
	
	@Override
	public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
		if (isIgnored)
			return null;
		
		if (!isDirCreated) {
			isDirCreated = true;
			try {
				Files.createDirectories(datapackClass.toPath());
			} catch (IOException e) {
				throw new MCJException("Error while creating class directory", e);
			}
		}
		
		if (exceptions != null && exceptions.length > 0) {
			throw new MCJException("MCJ cannot compile exceptions! Remove all 'throw(s)'s and 'try's");
		}
		
		if (MCJUtil.hasOpcode(access, Opcodes.ACC_SYNCHRONIZED)) {
			// Ignore; it is not currently possible to start another thread anyway
		}
		
		boolean isMain = isMainClass &&
				name.equals("main") &&
				MCJUtil.hasOpcodes(access, Opcodes.ACC_PUBLIC, Opcodes.ACC_STATIC) &&
				descriptor.equals("([Ljava/lang/String;)V");
		
		if (name.equals("<init>"))
			name = "constructor_";
		else
			name = "method_" + name.toLowerCase();
		
		if (isMain) {
			try {
				Files.writeString(new File(functions, "main.mcfunction").toPath(), """
						function mcj:setup
						function mcj:heap/malloc
						function mcj:stack/invokestatic {method:"$(~MAIN~)",num_args:"1",has_return:"false"}
						function mcj:heap/free_all
						""".replace("$(~MAIN~)", functionPath + name + "/entry"));
			} catch (IOException e) {
				throw new MCJException("Error while creating main.mcfunction", e);
			}
		}
		
		return new MCJMethodVisitor(new File(datapackClass, name), functionPath + name + "/", MCJUtil.hasOpcode(access, Opcodes.ACC_NATIVE));
	}
	
}
