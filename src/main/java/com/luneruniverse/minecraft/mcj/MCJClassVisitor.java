package com.luneruniverse.minecraft.mcj;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import com.luneruniverse.minecraft.mcj.MCJPathProvider.ClassPathProvider;
import com.luneruniverse.minecraft.mcj.MCJPathProvider.MethodPathProvider;

public class MCJClassVisitor extends ClassVisitor {
	
	private final ClassPathProvider provider;
	private String clinitHeader;
	
	public MCJClassVisitor(ClassPathProvider provider) {
		super(Opcodes.ASM9);
		this.provider = provider;
	}
	
	@Override
	public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
		StringBuilder clinitHeader = new StringBuilder("""
				execute if data storage mcj:data classes."$(~NAME~)".initialized run return 0
				data modify storage mcj:data classes."$(~NAME~)".initialized set value 1b
				""".replace("$(~NAME~)", provider.getClassName()));
		if (!MCJUtil.hasOpcode(access, Opcodes.ACC_INTERFACE)) {
			clinitHeader.append("data modify storage mcj:data classes.\"");
			clinitHeader.append(provider.getClassName());
			clinitHeader.append("\".vtables set value ");
			clinitHeader.append(provider.getInheritanceTracker().getVTablesNBT(provider.getClassName(), provider));
			clinitHeader.append("\ndata modify storage mcj:data classes.\"");
			clinitHeader.append(provider.getClassName());
			clinitHeader.append("\".instanceof set value ");
			clinitHeader.append(provider.getInheritanceTracker().getInstanceOfNBT(provider.getClassName()));
			clinitHeader.append('\n');
		}
		for (Map.Entry<String, Object> field : provider.getClinitTracker().getStaticFields(provider.getClassName()).entrySet()) {
			String value = "0";
			if (field.getValue() != null) {
				if (field.getValue() instanceof String str)
					value = '"' + str.replace("\\", "\\\\").replace("\"", "\\\"") + '"';
				else
					value = field.getValue().toString();
			}
			clinitHeader.append("data modify storage mcj:data classes.\"" + provider.getClassName() + "\".fields." +
					field.getKey().substring(0, field.getKey().indexOf(':')) + ".value set value " + value);
			clinitHeader.append('\n');
		}
		if (!superName.equals("java/lang/Object")) {
			clinitHeader.append("function ");
			clinitHeader.append(provider.getActualFunctionPath(
					provider.getImplForTracker().getClassPath(superName) + "/clinit/entry"));
			clinitHeader.append('\n');
		}
		if (interfaces != null) {
			for (String clazz : interfaces) {
				if (provider.getInheritanceTracker().isInterfaceInitSkippable(provider.getImplForTracker().getName(clazz)))
					continue;
				clinitHeader.append("function ");
				clinitHeader.append(provider.getActualFunctionPath(
						provider.getImplForTracker().getClassPath(clazz) + "/clinit/entry"));
				clinitHeader.append('\n');
			}
		}
		this.clinitHeader = clinitHeader.toString();
	}
	
	@Override
	public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
		if (exceptions != null && exceptions.length > 0) {
			throw new MCJException("MCJ cannot compile exceptions! Remove all 'throw(s)'s and 'try's");
		}
		
		if (MCJUtil.hasOpcode(access, Opcodes.ACC_ABSTRACT))
			return null;
		
		if (MCJUtil.hasOpcode(access, Opcodes.ACC_SYNCHRONIZED)) {
			// Ignore; it is not currently possible to start another thread anyway
		}
		
		boolean isMain = provider.isMainClass() &&
				name.equals("main") &&
				MCJUtil.hasOpcodes(access, Opcodes.ACC_PUBLIC, Opcodes.ACC_STATIC) &&
				descriptor.equals("([Ljava/lang/String;)V");
		
		boolean clinit = name.equals("<clinit>");
		String header = (clinit ? clinitHeader : null);
		if (clinit)
			clinitHeader = null;
		
		name = MCJUtil.getMethodName(name, descriptor);
		
		return new MCJMethodVisitor(new MethodPathProvider(provider,
				new File(provider.getClassFunctions(), name),
				provider.getClassFunctionsPath() + "/" + name,
				MCJUtil.getParamCount(descriptor),
				isMain,
				MCJUtil.hasOpcode(access, Opcodes.ACC_STATIC),
				MCJUtil.hasOpcode(access, Opcodes.ACC_NATIVE),
				descriptor.endsWith("V")), header);
	}
	
	@Override
	public void visitEnd() {
		if (clinitHeader == null)
			return;
		
		try {
			provider.writeToFile(new File(provider.getClassFunctions(), "clinit/entry.mcfunction"), clinitHeader);
		} catch (IOException e) {
			throw new MCJException("Error saving clinit to files", e);
		}
	}
	
}
