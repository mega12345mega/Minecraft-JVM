package com.luneruniverse.minecraft.mcj;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.Consumer;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import com.luneruniverse.minecraft.mcj.api.MCJExecute;
import com.luneruniverse.minecraft.mcj.api.MCJNativeImpl;

public class MCJMethodVisitor extends MethodVisitor {
	
	private static final String IF_ICMP_TEMPLATE = """
			function mcj:stack/lcmp
			function mcj:intstack/pop_var_from_stack
			execute if data storage mcj:data intstack[-1].$(comparison) run function $(target)
			execute if data storage mcj:data intstack[-1].$(comparison) run return run data remove storage mcj:data intstack[-1]
			#execute if data storage mcj:data intstack[-1].$(comparison) run return run function mcj:intstack/pop
			function mcj:intstack/pop""";
	
	private class MCJLabel {
		private final int index;
		private final Label label;
		private final StringBuilder builder;
		private boolean hasGoto;
		private boolean canCombine; // Can combine with PRECEDING label
		
		public MCJLabel(int index, Label label) {
			this.index = index;
			this.label = label;
			this.builder = new StringBuilder();
			this.hasGoto = false;
			this.canCombine = true;
		}
		
		public void println(String line) {
			if (hasGoto)
				throw new MCJException("Attempted to println on a label with a goto: " + functionPath + "l" + index);
			builder.append(line);
			builder.append('\n');
		}
		public void printGoto(String line) {
			if (!hasGoto)
				throw new MCJException("Attempted to printGoto on a label without a goto: " + functionPath + "l" + index);
			builder.append(line);
			builder.append('\n');
		}
		
		public void markGoto() {
			hasGoto = true;
		}
		public void markJumpedTo() {
			canCombine = false;
		}
		
		public boolean combine(MCJLabel successor) {
			if (!successor.canCombine) {
				if (!hasGoto)
					builder.append("function " + functionPath + "l" + successor.index);
				return false;
			}
			builder.append(successor.builder);
			hasGoto = successor.hasGoto;
			return true;
		}
		
		public void write() {
			try {
				Files.writeString(new File(dir, "l" + index + ".mcfunction").toPath(), builder.toString());
			} catch (IOException e) {
				throw new MCJException("Error saving method label", e);
			}
		}
	}
	
	private class MCJExecuteAnnotationVisitor extends AnnotationVisitor {
		private boolean fallThrough = false;
		public MCJExecuteAnnotationVisitor() {
			super(Opcodes.ASM9);
		}
		@Override
		public void visit(String name, Object value) {
			if (name.equals("value"))
				execute = (String) value;
			else if (name.equals("fallThrough"))
				fallThrough = (Boolean) value;
			else
				throw new MCJException("Malformed @MCJExecute; unknown argument '" + name + "'");
		}
		@Override
		public void visitEnd() {
			if (execute == null)
				throw new MCJException("Malformed @MCJExecute; missing 'value'");
			if ((execute.startsWith("as ") || execute.contains(" as ")) && !fallThrough)
				System.out.println("[Warning] @MCJExecute with 'as' may not function as expected; refer to the 'fallThrough' argument");
		}
	}
	
	private final File dir;
	private final String functionPath;
	private final boolean isNative;
	private boolean isNativeFound;
	private String execute;
	private final Map<Integer, MCJLabel> labels;
	private int labelIndex;
	private MCJLabel curLabel;
	private final WeakHashMap<Label, List<Consumer<MCJLabel>>> labelListeners;
	
	public MCJMethodVisitor(File dir, String functionPath, boolean isNative) {
		super(Opcodes.ASM9);
		this.dir = dir;
		this.functionPath = functionPath;
		this.isNative = isNative;
		this.isNativeFound = false;
		this.execute = null;
		this.labels = new HashMap<>();
		this.labelIndex = -1;
		this.curLabel = null;
		this.labelListeners = new WeakHashMap<>();
		
		try {
			Files.createDirectory(dir.toPath());
		} catch (IOException e) {
			throw new MCJException("Error creating method dir", e);
		}
	}
	
	@Override
	public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
		if (descriptor.equals(MCJNativeImpl.class.descriptorString())) {
			if (!isNative)
				throw new MCJException("@MCJNativeImpl cannot be applied to a non-native method!");
			isNativeFound = true;
			return new MCJNativeImplAnnotationVisitor(dir, functionPath);
		}
		if (descriptor.equals(MCJExecute.class.descriptorString())) {
			if (isNative)
				throw new MCJException("@MCJExecute cannot be applied to a native method!");
			return new MCJExecuteAnnotationVisitor();
		}
		return null;
	}
	
	private void registerLabelListener(Label label, Consumer<MCJLabel> listener) {
		for (MCJLabel knownLabel : labels.values()) {
			if (knownLabel.label == label) {
				listener.accept(knownLabel);
				return;
			}
		}
		labelListeners.computeIfAbsent(label, key -> new ArrayList<>()).add(listener);
	}
	
	@Override
	public void visitLabel(Label label) {
		labelIndex++;
		curLabel = labels.computeIfAbsent(labelIndex, key -> new MCJLabel(labelIndex, label));
		List<Consumer<MCJLabel>> listeners = labelListeners.get(label);
		if (listeners != null) {
			for (Consumer<MCJLabel> listener : listeners)
				listener.accept(curLabel);
		}
	}
	
	@Override
	public void visitFieldInsn(int opcode, String owner, String name, String descriptor) {
		switch (opcode) { // TODO
			case Opcodes.GETFIELD -> curLabel.println("function mcj:heap/getfield {name:\"" + name + "\"}");
			case Opcodes.PUTFIELD -> curLabel.println("function mcj:heap/putfield {name:\"" + name + "\"}");
		}
	}
	
	@Override
	public void visitJumpInsn(int opcode, Label label) {
		if (opcode == Opcodes.GOTO)
			curLabel.markGoto();
		
		MCJLabel curLabel = this.curLabel;
		registerLabelListener(label, targetLabel -> {
			targetLabel.markJumpedTo();
			switch (opcode) {
				case Opcodes.GOTO -> {
					curLabel.printGoto("function " + functionPath + "l" + targetLabel.index);
				}
				case Opcodes.IF_ICMPLT -> {
					curLabel.println(IF_ICMP_TEMPLATE.replace("$(comparison)", "lt").replace("$(target)", functionPath + "l" + targetLabel.index));
				}
			}
		});
	}
	
	@Override
	public void visitIincInsn(int varIndex, int increment) {
		curLabel.println("function mcj:localvars/iinc {index:\"" + varIndex + "\",action:\"" +
				(increment >= 0 ? "add" : "remove") + "\",amount:\"" + Math.abs(increment) + "\"}");
	}
	
	@Override
	public void visitInsn(int opcode) {
		switch (opcode) {
			case Opcodes.DUP -> curLabel.println("function mcj:stack/duplicate");
			case Opcodes.ICONST_M1 -> curLabel.println("function mcj:stack/push_const {value:\"-1\"}");
			case Opcodes.ICONST_0 -> curLabel.println("function mcj:stack/push_const {value:\"0\"}");
			case Opcodes.ICONST_1 -> curLabel.println("function mcj:stack/push_const {value:\"1\"}");
			case Opcodes.ICONST_2 -> curLabel.println("function mcj:stack/push_const {value:\"2\"}");
			case Opcodes.ICONST_3 -> curLabel.println("function mcj:stack/push_const {value:\"3\"}");
			case Opcodes.ICONST_4 -> curLabel.println("function mcj:stack/push_const {value:\"4\"}");
			case Opcodes.ICONST_5 -> curLabel.println("function mcj:stack/push_const {value:\"5\"}");
			case Opcodes.RETURN, Opcodes.ARETURN -> curLabel.println("return");
		}
	}
	
	@Override
	public void visitLdcInsn(Object value) {
		if (value instanceof String str) {
			curLabel.println("function mcj:stack/push_const {value:\"\\\"" + str.replace("\\", "\\\\\\\\").replace("\"", "\\\\\\\"") + "\\\"\"}");
		}
	}
	
	@Override
	public void visitLineNumber(int line, Label start) {
		curLabel.println("# --- Line " + line + " ---");
	}
	
	@Override
	public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
		StringBuilder path = new StringBuilder(functionPath.substring(0, functionPath.indexOf(':') + 1));
		String[] ownerParts = owner.split("/");
		for (int i = 0; i < ownerParts.length - 1; i++) {
			path.append("package_");
			path.append(ownerParts[i]);
			path.append('/');
		}
		path.append("class_");
		path.append(ownerParts[ownerParts.length - 1]);
		if (name.equals("<init>")) {
			path.append("/constructor_");
		} else {
			path.append("/method_");
			path.append(name);
		}
		path.append("/entry");
		String pathStr = path.toString().toLowerCase();
		
		int numArgs = 0;
		boolean parsingClass = false;
		for (char c : descriptor.toCharArray()) {
			if (parsingClass) {
				if (c == ';')
					parsingClass = false;
			} else {
				if (c == '(' || c == '[')
					;
				else if (c == ')')
					break;
				else if (c == 'B' || c == 'C' || c == 'D' || c == 'F' || c == 'I' || c == 'J' || c == 'S' || c == 'Z')
					numArgs++;
				else if (c == 'L') {
					numArgs++;
					parsingClass = true;
				}
			}
		}
		
		boolean hasReturn = descriptor.charAt(descriptor.length() - 1) != 'V';
		
		switch (opcode) {
			case Opcodes.INVOKESTATIC -> curLabel.println("function mcj:stack/invokestatic {method:\"" + pathStr + "\",num_args:\"" + numArgs + "\",has_return:\"" + hasReturn + "\"}");
			case Opcodes.INVOKESPECIAL, Opcodes.INVOKEVIRTUAL -> {
				if (owner.equals("java/lang/Object") && name.equals("<init>") && descriptor.equals("()V"))
					curLabel.println("function mcj:stack/pop");
				else // TODO
					curLabel.println("function mcj:stack/invokestatic {method:\"" + pathStr + "\",num_args:\"" + (numArgs + 1) + "\",has_return:\"" + hasReturn + "\"}");
			}
		}
	}
	
	@Override
	public void visitTryCatchBlock(Label start, Label end, Label handler, String type) {
		throw new MCJException("MCJ cannot compile exceptions! Remove all 'throws's and 'try's");
	}
	
	@Override
	public void visitTypeInsn(int opcode, String type) {
		switch (opcode) {
			case Opcodes.NEW -> curLabel.println("function mcj:heap/malloc");
		}
	}
	
	@Override
	public void visitVarInsn(int opcode, int varIndex) {
		switch (opcode) {
			case Opcodes.ILOAD, Opcodes.ALOAD -> curLabel.println("function mcj:localvars/push_var_to_stack {index:\"" + varIndex + "\"}");
			case Opcodes.ISTORE, Opcodes.ASTORE -> curLabel.println("function mcj:localvars/pop_var_from_stack {index:\"" + varIndex + "\"}");
		}
	}
	
	@Override
	public void visitEnd() {
		if (isNative) {
			if (!isNativeFound) {
				System.out.println("[Warning] Missing @MCJNativeImpl on a native method! "
						+ "Are you going to add the implementation manually? (" + functionPath + ")");
			}
			return;
		}
		
		try {
			// Labels: mcj bug checker & converting to list
			List<MCJLabel> labels = new ArrayList<>();
			for (int i = 0; i < this.labels.size(); i++) {
				MCJLabel label = this.labels.get(i);
				if (label == null || label.index != i) {
					throw new MCJException("Malformed labels map: " +
							this.labels.entrySet().stream().map(entry -> entry.getKey() + ":" + entry.getValue().index)
							.reduce((a, b) -> a + "," + b).orElse("<empty>"));
				}
				labels.add(label);
			}
			
			// Labels: combining
			for (int i = 0; i < labels.size() - 1; i++) {
				MCJLabel label = labels.get(i);
				while (i < labels.size() - 1 && label.combine(labels.get(i + 1)))
					labels.remove(i + 1);
			}
			
			// Entrypoint & combining label 0
			String entrypointValue = "function " + functionPath + "l0";
			if (execute != null)
				entrypointValue = "execute " + execute + " run " + entrypointValue;
			else if (labels.get(0).canCombine)
				entrypointValue = labels.remove(0).builder.toString();
			
			Files.writeString(new File(dir, "entry.mcfunction").toPath(), entrypointValue);
			
			// Labels: writing
			for (MCJLabel label : labels)
				label.write();
		} catch (IOException e) {
			throw new MCJException("Error saving method to files", e);
		}
	}
	
}
