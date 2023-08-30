package com.luneruniverse.minecraft.mcj;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import com.luneruniverse.minecraft.mcj.api.MCJExpandPath;
import com.luneruniverse.minecraft.mcj.api.MCJIgnore;
import com.luneruniverse.minecraft.mcj.api.MCJImplFor;

public class MCJClassInitVisitor extends ClassVisitor {
	
	private final MCJPathProvider provider;
	private final Consumer<String> onName;
	private final Runnable onIgnore;
	private final Map<String, Object> staticFields;
	private String name;
	private String superClass;
	private List<String> superInterfaces;
	private Boolean isInterface;
	private boolean inheritanceTracked;
	private boolean ignored;
	private String namespace;
	private String newPath;
	private boolean expandedPaths;
	
	public MCJClassInitVisitor(MCJPathProvider provider, Consumer<String> onName, Runnable onIgnore) {
		super(Opcodes.ASM9);
		this.provider = provider;
		this.onName = onName;
		this.onIgnore = onIgnore;
		this.staticFields = new HashMap<>();
	}
	
	@Override
	public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
		this.name = name;
		this.superClass = superName;
		this.superInterfaces = (interfaces == null ? new ArrayList<>() : Arrays.asList(interfaces));
		this.isInterface = MCJUtil.hasOpcode(access, Opcodes.ACC_INTERFACE);
		this.namespace = provider.getNamespace();
		this.newPath = name;
		this.expandedPaths = provider.isExpandedPaths();
		
		String thePackage = name.substring(0, name.lastIndexOf('/') + 1);
		if (thePackage.equals("com/luneruniverse/minecraft/mcj/") || // Compile com.luneruniverse.minecraft.mcj.api
				thePackage.startsWith("org/objectweb/asm/")) {
			ignored = true;
			onIgnore.run();
		} else {
			onName.accept(name);
		}
	}
	
	@Override
	public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
		if (ignored)
			return null;
		
		if (descriptor.equals(MCJIgnore.class.descriptorString())) {
			ignored = true;
			onIgnore.run();
		} else if (descriptor.equals(MCJImplFor.class.descriptorString())) {
			return new AnnotationVisitor(Opcodes.ASM9) {
				@Override
				public void visit(String name, Object value) {
					if (name.equals("value") && value instanceof String path) {
						int i = path.indexOf(':');
						if (i != -1) {
							namespace = path.substring(0, i);
							path = path.substring(i + 1);
						}
						if (path.isEmpty())
							path = MCJClassInitVisitor.this.name;
						newPath = path;
					} else
						throw new MCJException("Malformed @MCJImplFor; only 'value' of type 'java.lang.String' is permitted");
				}
			};
		} else if (descriptor.equals(MCJExpandPath.class.descriptorString()))
			expandedPaths = true;
		
		return null;
	}
	
	@Override
	public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
		if (ignored)
			return null;
		
		trackInheritance();
		provider.getInheritanceTracker().trackField(newPath, name, descriptor, access);
		
		if (MCJUtil.hasOpcode(access, Opcodes.ACC_STATIC))
			staticFields.put(name + ":" + descriptor, value);
		
		return null;
	}
	
	@Override
	public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
		if (ignored)
			return null;
		
		trackInheritance();
		if (!name.equals("<clinit>") && !name.equals("<init>"))
			provider.getInheritanceTracker().trackMethod(newPath, name, descriptor, access);
		
		return null;
	}
	
	@Override
	public void visitEnd() {
		if (ignored)
			return;
		
		trackInheritance();
		provider.getImplForTracker().track(name, namespace, newPath, expandedPaths);
		provider.getClinitTracker().trackStaticFields(newPath, staticFields);
	}
	
	private void trackInheritance() {
		if (inheritanceTracked)
			return;
		provider.getInheritanceTracker().trackClass(newPath, superClass, superInterfaces, isInterface);
		inheritanceTracked = true;
	}
	
}
