package com.luneruniverse.minecraft.mcj;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import com.luneruniverse.minecraft.mcj.MCJPathProvider.MethodPathProvider;
import com.luneruniverse.minecraft.mcj.api.MCJEntrypoint;
import com.luneruniverse.minecraft.mcj.api.MCJExecute;
import com.luneruniverse.minecraft.mcj.api.MCJNativeImpl;

public class MCJMethodVisitor extends MethodVisitor {
	
	private static final int NULLPTR = 0; // Matches value in mcj:heap/_newarray
	
	private static final String IFCMP0_TEMPLATE = """
			execute store result score cmp_a mcj_data run data get storage mcj:data stack[-1].value
			function mcj:stack/pop
			data modify storage mcj:data intstack append value {}
			execute $(condition) score cmp_a mcj_data matches $(range) run data modify storage mcj:data intstack[-1].matches set value 1b
			execute if data storage mcj:data intstack[-1].matches run function $(target)
			execute if data storage mcj:data intstack[-1].matches run return run data remove storage mcj:data intstack[-1]
			#execute if data storage mcj:data intstack[-1].matches run return run function mcj:intstack/pop
			function mcj:intstack/pop""";
	
	private static final String IF_ICMP_TEMPLATE = """
			function mcj:stack/setup_icmp
			function mcj:intstack/pop_var_from_stack
			execute if data storage mcj:data intstack[-1].$(comparison) run function $(target)
			execute if data storage mcj:data intstack[-1].$(comparison) run return run data remove storage mcj:data intstack[-1]
			#execute if data storage mcj:data intstack[-1].$(comparison) run return run function mcj:intstack/pop
			function mcj:intstack/pop""";
	
	private class MCJLabel {
		private class MCJLabelPosition {
			private int index;
			private boolean hasGoto;
			private MCJLabelPosition(int index, boolean hasGoto) {
				this.index = index;
				this.hasGoto = hasGoto;
			}
			public void println(String line) {
				if (hasGoto)
					throw new MCJException("Attempted to println on a label with a goto: " + provider.getMethodFunctionsPath() + "/l" + MCJLabel.this.index);
				int index = this.index;
				builder.insert(index, line + '\n');
				positions.forEach((pos, value) -> {
					if (pos.index >= index)
						pos.index += line.length() + 1;
				});
			}
			public void printGoto(String line) {
				int index = this.index;
				builder.insert(index, line + '\n');
				MCJLabel.this.hasGoto = true;
				positions.forEach((pos, value) -> {
					if (pos.index >= index) {
						pos.hasGoto = true;
						pos.index += line.length() + 1;
					}
				});
			}
		}
		
		private final int index;
		private final Label label;
		private final StringBuilder builder;
		private final WeakHashMap<MCJLabelPosition, Boolean> positions;
		private boolean hasGoto;
		private boolean canCombine; // Can combine with PRECEDING label
		
		public MCJLabel(int index, Label label) {
			this.index = index;
			this.label = label;
			this.builder = new StringBuilder();
			this.positions = new WeakHashMap<>();
			this.hasGoto = false;
			this.canCombine = true;
		}
		
		public void println(String line) {
			if (hasGoto)
				throw new MCJException("Attempted to println on a label with a goto: " + provider.getMethodFunctionsPath() + "/l" + index);
			builder.append(line);
			builder.append('\n');
		}
		public void printGoto(String line) {
			hasGoto = true;
			builder.append(line);
			builder.append('\n');
		}
		public MCJLabelPosition printHereLater() {
			MCJLabelPosition output = new MCJLabelPosition(builder.length(), hasGoto);
			positions.put(output, true);
			builder.append("# Removed by MCJLabel\n"); // Ensure other calls to printHereLater don't try to print to the same index
			return output;
		}
		
		public void markGoto() {
			hasGoto = true;
		}
		public void markJumpedTo() {
			canCombine = false;
		}
		
		public String getFunctionPath() {
			return provider.getActualFunctionPath(provider.getMethodFunctionsPath() + "/l" + index);
		}
		
		public boolean combine(MCJLabel successor) {
			if (!successor.canCombine) {
				if (!hasGoto)
					builder.append("function " + successor.getFunctionPath());
				return false;
			}
			builder.append(successor.builder);
			hasGoto = successor.hasGoto;
			return true;
		}
		
		public String getContents() {
			return builder.toString().replace("# Removed by MCJLabel\n", "");
		}
		public void write() {
			try {
				provider.writeToFile(new File(provider.getMethodFunctions(), "l" + index + ".mcfunction"), getContents());
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
	
	private class MCJEntrypointAnnotationVisitor extends AnnotationVisitor {
		public MCJEntrypointAnnotationVisitor() {
			super(Opcodes.ASM9);
		}
		@Override
		public void visit(String name, Object value) {
			if (!name.equals("value"))
				throw new MCJException("Malformed @MCJEntrypoint; unknown argument '" + name + "'");
			
			entrypoint = (String) value;
			if (entrypoint.endsWith(".mcfunction"))
				System.out.println("[Warning] You don't have to specify .mcfunction in @MCJEntrypoint (" + provider.getMethodFunctionsPath() + ")");
			else
				entrypoint += ".mcfunction";
		}
	}
	
	private final MethodPathProvider provider;
	private boolean isNativeFound;
	private String execute;
	private String entrypoint;
	private final Map<Integer, MCJLabel> labels;
	private int labelIndex;
	private MCJLabel curLabel;
	private final WeakHashMap<Label, List<Consumer<MCJLabel>>> labelListeners;
	private final String[] parameters;
	
	public MCJMethodVisitor(MethodPathProvider provider) {
		super(Opcodes.ASM9);
		this.provider = provider;
		this.isNativeFound = false;
		this.execute = null;
		this.entrypoint = null;
		this.labels = new HashMap<>();
		this.labelIndex = -1;
		this.curLabel = null;
		this.labelListeners = new WeakHashMap<>();
		this.parameters = new String[provider.getParamCount()];
	}
	
	@Override
	public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
		if (descriptor.equals(MCJNativeImpl.class.descriptorString())) {
			if (!provider.isNativeMethod())
				throw new MCJException("@MCJNativeImpl cannot be applied to a non-native method!");
			isNativeFound = true;
			return new MCJNativeImplAnnotationVisitor(provider);
		}
		if (descriptor.equals(MCJExecute.class.descriptorString())) {
			if (provider.isNativeMethod())
				throw new MCJException("@MCJExecute cannot be applied to a native method!");
			return new MCJExecuteAnnotationVisitor();
		}
		if (descriptor.equals(MCJEntrypoint.class.descriptorString())) {
			if (!provider.isStaticMethod())
				throw new MCJException("@MCJEntrypoint cannot be a applied to a non-static method!");
			return new MCJEntrypointAnnotationVisitor();
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
	private void registerLabelsListener(List<Label> labels, Consumer<List<MCJLabel>> listener) {
		MCJLabel[] output = new MCJLabel[labels.size()];
		AtomicInteger numFound = new AtomicInteger(0);
		for (int i = 0; i < labels.size(); i++) {
			final int finalI = i;
			registerLabelListener(labels.get(i), label -> {
				output[finalI] = label;
				if (numFound.incrementAndGet() == labels.size())
					listener.accept(Arrays.asList(output));
			});
		}
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
		switch (opcode) { // TODO GETSTATIC, PUTSTATIC, field resolution doesn't handle superclasses
//			case Opcodes.GETSTATIC -> ;
//			case Opcodes.PUTSTATIC -> ;
			case Opcodes.GETFIELD -> curLabel.println("function mcj:heap/getfield {name:\"" + name + "\"}");
			case Opcodes.PUTFIELD -> curLabel.println("function mcj:heap/putfield {name:\"" + name + "\"}");
			default -> throw new MCJException("Unsupported opcode: " + opcode);
		}
	}
	
	@Override
	public void visitFrame(int type, int numLocal, Object[] local, int numStack, Object[] stack) {
		// TODO Auto-generated method stub
		super.visitFrame(type, numLocal, local, numStack, stack);
	}
	
	@Override
	public void visitIincInsn(int varIndex, int increment) {
		curLabel.println("function mcj:localvars/iinc {index:\"" + varIndex + "\",action:\"" +
				(increment >= 0 ? "add" : "remove") + "\",amount:\"" + Math.abs(increment) + "\"}");
	}
	
	@Override
	public void visitInsn(int opcode) {
		// TODO DUP_X2, DUP2, DUP2_X1, DUP2_X2
		/*
		 TODO DIV AND MOD USE floorDiv and floorMod FOR SCOREBOARDS INSTEAD OF / and %. :/
   *     LADD, FADD, DADD, LSUB, FSUB, DSUB, LMUL, FMUL, DMUL, LDIV,
   *     FDIV, DDIV, LREM, FREM, DREM, LNEG, FNEG, DNEG, ISHL, LSHL, ISHR, LSHR, IUSHR,
   *     LUSHR, IAND, LAND, IOR, LOR, IXOR, LXOR,
		 */
		// TODO I2F, L2I, L2F, L2D, F2I, F2L, D2I, D2L, D2F, I2B, I2C, I2S
		// TODO FCMPL, FCMPG, DCMPL, DCMPG
		switch (opcode) {
			case Opcodes.NOP -> {}
			case Opcodes.ACONST_NULL -> curLabel.println("function mcj:stack/push_const {value:\"" + NULLPTR + "\"}");
			case Opcodes.ICONST_M1 -> curLabel.println("function mcj:stack/push_const {value:\"-1\"}");
			case Opcodes.ICONST_0 -> curLabel.println("function mcj:stack/push_const {value:\"0\"}");
			case Opcodes.ICONST_1 -> curLabel.println("function mcj:stack/push_const {value:\"1\"}");
			case Opcodes.ICONST_2 -> curLabel.println("function mcj:stack/push_const {value:\"2\"}");
			case Opcodes.ICONST_3 -> curLabel.println("function mcj:stack/push_const {value:\"3\"}");
			case Opcodes.ICONST_4 -> curLabel.println("function mcj:stack/push_const {value:\"4\"}");
			case Opcodes.ICONST_5 -> curLabel.println("function mcj:stack/push_const {value:\"5\"}");
			case Opcodes.LCONST_0 -> curLabel.println("function mcj:stack/push_const {value:\"0\"}");
			case Opcodes.LCONST_1 -> curLabel.println("function mcj:stack/push_const {value:\"1\"}");
			case Opcodes.FCONST_0 -> curLabel.println("function mcj:stack/push_const {value:\"0\"}");
			case Opcodes.FCONST_1 -> curLabel.println("function mcj:stack/push_const {value:\"1\"}");
			case Opcodes.FCONST_2 -> curLabel.println("function mcj:stack/push_const {value:\"2\"}");
			case Opcodes.DCONST_0 -> curLabel.println("function mcj:stack/push_const {value:\"0\"}");
			case Opcodes.DCONST_1 -> curLabel.println("function mcj:stack/push_const {value:\"1\"}");
			case Opcodes.IALOAD -> curLabel.println("function mcj:heap/aload");
			case Opcodes.LALOAD -> curLabel.println("function mcj:heap/aload");
			case Opcodes.FALOAD -> curLabel.println("function mcj:heap/aload");
			case Opcodes.DALOAD -> curLabel.println("function mcj:heap/aload");
			case Opcodes.AALOAD -> curLabel.println("function mcj:heap/aload");
			case Opcodes.BALOAD -> curLabel.println("function mcj:heap/aload");
			case Opcodes.CALOAD -> curLabel.println("function mcj:heap/aload");
			case Opcodes.SALOAD -> curLabel.println("function mcj:heap/aload");
			case Opcodes.IASTORE -> curLabel.println("function mcj:heap/astore");
			case Opcodes.LASTORE -> curLabel.println("function mcj:heap/astore");
			case Opcodes.FASTORE -> curLabel.println("function mcj:heap/astore");
			case Opcodes.DASTORE -> curLabel.println("function mcj:heap/astore");
			case Opcodes.AASTORE -> curLabel.println("function mcj:heap/astore");
			case Opcodes.BASTORE -> curLabel.println("function mcj:heap/astore");
			case Opcodes.CASTORE -> curLabel.println("function mcj:heap/astore");
			case Opcodes.SASTORE -> curLabel.println("function mcj:heap/astore");
			case Opcodes.POP -> curLabel.println("function mcj:stack/pop");
			case Opcodes.POP2 -> curLabel.println("function mcj:stack/pop\nfunction mcj:stack/pop");
			case Opcodes.DUP -> curLabel.println("function mcj:stack/dup");
			case Opcodes.DUP_X1 -> curLabel.println("function mcj:stack/dup_x1");
//			case Opcodes.DUP_X2 -> ;
//			case Opcodes.DUP2 -> ;
//			case Opcodes.DUP2_X1 -> ;
//			case Opcodes.DUP2_X2 -> ;
			case Opcodes.SWAP -> curLabel.println("function mcj:stack/swap");
			case Opcodes.IADD -> curLabel.println("function mcj:stack/imath {op:\"+=\"}");
//			case Opcodes.LADD -> ;
//			case Opcodes.FADD -> ;
//			case Opcodes.DADD -> ;
			case Opcodes.ISUB -> curLabel.println("function mcj:stack/imath {op:\"-=\"}");
//			case Opcodes.LSUB -> ;
//			case Opcodes.FSUB -> ;
//			case Opcodes.DSUB -> ;
			case Opcodes.IMUL -> curLabel.println("function mcj:stack/imath {op:\"*=\"}");
//			case Opcodes.LMUL -> ;
//			case Opcodes.FMUL -> ;
//			case Opcodes.DMUL -> ;
			case Opcodes.IDIV -> curLabel.println("function mcj:stack/imath {op:\"/=\"}");
//			case Opcodes.LDIV -> ;
//			case Opcodes.FDIV -> ;
//			case Opcodes.DDIV -> ;
			case Opcodes.IREM -> curLabel.println("function mcj:stack/imath {op:\"%=\"}");
//			case Opcodes.LREM -> ;
//			case Opcodes.FREM -> ;
//			case Opcodes.DREM -> ;
			case Opcodes.INEG -> curLabel.println("function mcj:stack/ineg");
//			case Opcodes.LNEG -> ;
//			case Opcodes.FNEG -> ;
//			case Opcodes.DNEG -> ;
//			case Opcodes.ISHL -> ;
//			case Opcodes.LSHL -> ;
//			case Opcodes.ISHR -> ;
//			case Opcodes.LSHR -> ;
//			case Opcodes.IUSHR -> ;
//			case Opcodes.LUSHR -> ;
//			csae Opcodes.IAND -> ;
//			case Opcodes.LAND -> ;
//			case Opcodes.IOR -> ;
//			case Opcodes.LOR -> ;
//			case Opcodes.IXOR -> ;
//			case Opcodes.LXOR -> ;
			case Opcodes.I2L -> {}
//			case Opcodes.I2F -> ;
			case Opcodes.I2D -> {}
//			case Opcodes.L2I -> ;
//			case Opcodes.L2F -> ;
//			case Opcodes.L2D -> ;
//			case Opcodes.F2I -> ;
//			case Opcodes.F2L -> ;
			case Opcodes.F2D -> {}
//			case Opcodes.D2I -> ;
//			case Opcodes.D2L -> ;
//			case Opcodes.D2F -> ;
//			case Opcodes.I2B -> ;
//			case Opcodes.I2C -> ;
//			case Opcodes.I2S -> ;
			case Opcodes.LCMP -> curLabel.println("function mcj:stack/lcmp");
//			case Opcodes.FCMPL -> ;
//			case Opcodes.FCMPG -> ;
//			case Opcodes.DCMPL -> ;
//			case Opcodes.DCMPG -> ;
			case Opcodes.IRETURN -> curLabel.printGoto("return 0");
			case Opcodes.LRETURN -> curLabel.printGoto("return 0");
			case Opcodes.FRETURN -> curLabel.printGoto("return 0");
			case Opcodes.DRETURN -> curLabel.printGoto("return 0");
			case Opcodes.ARETURN -> curLabel.printGoto("return 0");
			case Opcodes.RETURN -> curLabel.printGoto("return 0");
			case Opcodes.ARRAYLENGTH -> curLabel.println("function mcj:heap/arraylength");
			case Opcodes.ATHROW -> throw new MCJException("MCJ cannot compile exceptions! Remove all 'throw(s)'s and 'try's");
			case Opcodes.MONITORENTER -> {} // Ignore; it is not currently possible to start another thread anyway
			case Opcodes.MONITOREXIT -> {} // Ignore; it is not currently possible to start another thread anyway
			default -> throw new MCJException("Unsupported opcode: " + opcode);
		}
	}
	
	@Override
	public void visitIntInsn(int opcode, int operand) {
		switch (opcode) { // TODO T_FLOAT, T_DOUBLE
			case Opcodes.BIPUSH -> curLabel.println("function mcj:stack/push_const {value:\"" + operand + "\"}");
			case Opcodes.SIPUSH -> curLabel.println("function mcj:stack/push_const {value:\"" + operand + "\"}");
			case Opcodes.NEWARRAY -> {switch (operand) {
				case Opcodes.T_BOOLEAN -> curLabel.println("function mcj:heap/newarray");
				case Opcodes.T_CHAR -> curLabel.println("function mcj:heap/newarray");
//				case Opcodes.T_FLOAT -> ;
//				case Opcodes.T_DOUBLE -> ;
				case Opcodes.T_BYTE -> curLabel.println("function mcj:heap/newarray");
				case Opcodes.T_SHORT -> curLabel.println("function mcj:heap/newarray");
				case Opcodes.T_INT -> curLabel.println("function mcj:heap/newarray");
				case Opcodes.T_LONG -> curLabel.println("function mcj:heap/newarray");
				default -> throw new MCJException("Unsupported array type: " + operand);
			};}
			default -> throw new MCJException("Unsupported opcode: " + opcode);
		}
	}
	
	@Override
	public void visitInvokeDynamicInsn(String name, String descriptor, Handle bootstrapMethodHandle,
			Object... bootstrapMethodArguments) {
		// TODO
		throw new MCJException("Unsupported opcode: " + Opcodes.INVOKEDYNAMIC);
	}
	
	@Override
	public void visitJumpInsn(int opcode, Label label) {
		if (opcode == Opcodes.GOTO)
			curLabel.markGoto();
		
		MCJLabel.MCJLabelPosition pos = curLabel.printHereLater();
		registerLabelListener(label, targetLabel -> {
			targetLabel.markJumpedTo();
			switch (opcode) {
				case Opcodes.IFEQ -> pos.println(IFCMP0_TEMPLATE.replace("$(condition)", "if")
						.replace("$(range)", "0").replace("$(target)", targetLabel.getFunctionPath()));
				case Opcodes.IFNE -> pos.println(IFCMP0_TEMPLATE.replace("$(condition)", "unless")
						.replace("$(range)", "0").replace("$(target)", targetLabel.getFunctionPath()));
				case Opcodes.IFLT -> pos.println(IFCMP0_TEMPLATE.replace("$(condition)", "if")
						.replace("$(range)", "..-1").replace("$(target)", targetLabel.getFunctionPath()));
				case Opcodes.IFGE -> pos.println(IFCMP0_TEMPLATE.replace("$(condition)", "if")
						.replace("$(range)", "0..").replace("$(target)", targetLabel.getFunctionPath()));
				case Opcodes.IFGT -> pos.println(IFCMP0_TEMPLATE.replace("$(condition)", "if")
						.replace("$(range)", "1..").replace("$(target)", targetLabel.getFunctionPath()));
				case Opcodes.IFLE -> pos.println(IFCMP0_TEMPLATE.replace("$(condition)", "if")
						.replace("$(range)", "..0").replace("$(target)", targetLabel.getFunctionPath()));
				case Opcodes.IF_ICMPEQ -> pos.println(IF_ICMP_TEMPLATE.replace("$(comparison)", "eq")
						.replace("$(target)", targetLabel.getFunctionPath()));
				case Opcodes.IF_ICMPNE -> pos.println(IF_ICMP_TEMPLATE.replace("$(comparison)", "ne")
						.replace("$(target)", targetLabel.getFunctionPath()));
				case Opcodes.IF_ICMPLT -> pos.println(IF_ICMP_TEMPLATE.replace("$(comparison)", "lt")
						.replace("$(target)", targetLabel.getFunctionPath()));
				case Opcodes.IF_ICMPGE -> pos.println(IF_ICMP_TEMPLATE.replace("$(comparison)", "ge")
						.replace("$(target)", targetLabel.getFunctionPath()));
				case Opcodes.IF_ICMPGT -> pos.println(IF_ICMP_TEMPLATE.replace("$(comparison)", "gt")
						.replace("$(target)", targetLabel.getFunctionPath()));
				case Opcodes.IF_ICMPLE -> pos.println(IF_ICMP_TEMPLATE.replace("$(comparison)", "le")
						.replace("$(target)", targetLabel.getFunctionPath()));
				case Opcodes.IF_ACMPEQ -> pos.println(IF_ICMP_TEMPLATE.replace("$(comparison)", "eq")
						.replace("$(target)", targetLabel.getFunctionPath()));
				case Opcodes.IF_ACMPNE -> pos.println(IF_ICMP_TEMPLATE.replace("$(comparison)", "ne")
						.replace("$(target)", targetLabel.getFunctionPath()));
				case Opcodes.GOTO -> pos.printGoto("function " + targetLabel.getFunctionPath());
				case Opcodes.JSR -> throw new MCJException("Unsupported deprecated opcode: " + opcode);
				case Opcodes.IFNULL -> pos.println(IFCMP0_TEMPLATE.replace("$(condition)", "if")
						.replace("$(range)", "0").replace("$(target)", targetLabel.getFunctionPath()));
				case Opcodes.IFNONNULL -> pos.println(IFCMP0_TEMPLATE.replace("$(condition)", "unless")
						.replace("$(range)", "0").replace("$(target)", targetLabel.getFunctionPath()));
				default -> throw new MCJException("Unsupported opcode: " + opcode);
			}
		});
	}
	
	@Override
	public void visitLdcInsn(Object value) { // TODO Float, Double
		if (value instanceof Integer num) {
			curLabel.println("function mcj:stack/push_const {value:\"" + num + "\"}");
		}/* else if (value instanceof Float num) {
			
		}*/ else if (value instanceof Long num) {
			curLabel.println("function mcj:stack/push_const {value:\"" + num + "l\"}"); // TODO longs take up two localvars?
		}/* else if (value instanceof Double num) {
			
		}*/ else if (value instanceof String str) {
			curLabel.println("function mcj:stack/push_const {value:\"\\\"" + str.replace("\\", "\\\\\\\\").replace("\"", "\\\\\\\"") + "\\\"\"}");
		} else {
			throw new MCJException("Unsupported constant: " + value.getClass().getName() + ": " + value);
		}
	}
	
	@Override
	public void visitLineNumber(int line, Label start) {
		curLabel.println("# --- Line " + line + " ---");
	}
	
	@Override
	public void visitLocalVariable(String name, String descriptor, String signature, Label start, Label end, int index) {
		if (index < parameters.length)
			parameters[index] = name;
	}
	
	@Override
	public void visitLookupSwitchInsn(Label dflt, int[] keys, Label[] labels) {
		curLabel.markGoto();
		
		MCJLabel.MCJLabelPosition pos = curLabel.printHereLater();
		List<Label> allLabels = new ArrayList<>(1 + labels.length);
		allLabels.add(dflt);
		for (Label label : labels)
			allLabels.add(label);
		
		registerLabelsListener(allLabels, mcjLabels -> {
			StringBuilder labelFunctionList = new StringBuilder("data modify storage mcj:data switch_label set value {");
			
			MCJLabel mcjDflt = mcjLabels.get(0);
			mcjDflt.markJumpedTo();
			labelFunctionList.append("\"default\":\"");
			labelFunctionList.append(mcjDflt.getFunctionPath());
			labelFunctionList.append("\",");
			
			for (int i = 1; i < mcjLabels.size(); i++) {
				MCJLabel label = mcjLabels.get(i);
				label.markJumpedTo();
				
				labelFunctionList.append("\"f");
				labelFunctionList.append(keys[i - 1]);
				labelFunctionList.append("\":\"");
				labelFunctionList.append(label.getFunctionPath());
				labelFunctionList.append("\",");
			}
			
			labelFunctionList.setCharAt(labelFunctionList.length() - 1, '}');
			pos.println(labelFunctionList.toString());
			pos.println("function mcj:stack/lookupswitch with storage mcj:data stack[-1]");
		});
	}
	
	@Override
	public void visitMaxs(int maxStack, int maxLocals) {
		// Ignore
	}
	
	@Override
	public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
		StringBuilder path = new StringBuilder(provider.getNamespace());
		path.append(':');
		path.append(MCJUtil.formatClassPath(provider.getChangedClassPath(owner)));
		if (name.equals("<init>")) {
			path.append("/constructor");
		} else {
			path.append("/method_");
			path.append(name);
		}
		path.append('_');
		path.append(MCJUtil.md5(name + descriptor));
		path.append("/entry");
		String pathStr = provider.getActualFunctionPath(path.toString().toLowerCase());
		
		int numArgs = MCJUtil.getParamCount(descriptor);
		boolean hasReturn = descriptor.charAt(descriptor.length() - 1) != 'V';
		
		switch (opcode) { // TODO unordered
			case Opcodes.INVOKESTATIC -> curLabel.println("function mcj:stack/invokestatic {method:\"" + pathStr + "\",num_args:\"" + numArgs + "\",has_return:\"" + hasReturn + "\"}");
			case Opcodes.INVOKEVIRTUAL, Opcodes.INVOKESPECIAL, Opcodes.INVOKEINTERFACE -> {
				if (owner.equals("java/lang/Object") && name.equals("<init>") && descriptor.equals("()V"))
					curLabel.println("function mcj:stack/pop");
				else // TODO method resolution doesn't handle superclasses/interfaces
					curLabel.println("function mcj:stack/invokestatic {method:\"" + pathStr + "\",num_args:\"" + (numArgs + 1) + "\",has_return:\"" + hasReturn + "\"}");
			}
			default -> throw new MCJException("Unsupported opcode: " + opcode);
		}
	}
	
	@Override
	public void visitMultiANewArrayInsn(String descriptor, int numDimensions) {
		curLabel.println("function mcj:heap/multianewarray {numDimensions:\"" + numDimensions + "\"}");
	}
	
	@Override
	public void visitParameter(String name, int access) {
		// Ignore
	}
	
	@Override
	public void visitTableSwitchInsn(int min, int max, Label dflt, Label... labels) {
		curLabel.markGoto();
		
		MCJLabel.MCJLabelPosition pos = curLabel.printHereLater();
		List<Label> allLabels = new ArrayList<>(1 + labels.length);
		allLabels.add(dflt);
		for (Label label : labels)
			allLabels.add(label);
		
		registerLabelsListener(allLabels, mcjLabels -> {
			StringBuilder labelFunctionList = new StringBuilder("data modify storage mcj:data switch_label set value [");
			
			for (MCJLabel label : mcjLabels) {
				label.markJumpedTo();
				labelFunctionList.append('"');
				labelFunctionList.append(label.getFunctionPath());
				labelFunctionList.append("\",");
			}
			labelFunctionList.setCharAt(labelFunctionList.length() - 1, ']');
			pos.println(labelFunctionList.toString());
			
			pos.println("execute store result score math_a mcj_data run data get storage mcj:data stack[-1].value");
			int shift = -min + 1;
			if (shift > 0)
				pos.println("scoreboard players add math_a mcj_data " + shift);
			else if (shift < 0)
				pos.println("scoreboard players remove math_a mcj_data " + (-shift));
			pos.println("execute store result storage mcj:data stack[-1].value int 1 run scoreboard players get math_a mcj_data");
			pos.println("function mcj:stack/tableswitch with storage mcj:data stack[-1]");
		});
	}
	
	@Override
	public void visitTryCatchBlock(Label start, Label end, Label handler, String type) {
		throw new MCJException("MCJ cannot compile exceptions! Remove all 'throw(s)'s and 'try's");
	}
	
	@Override
	public void visitTypeInsn(int opcode, String type) {
		switch (opcode) { // TODO INSTANCEOF
			case Opcodes.NEW -> curLabel.println("function mcj:heap/malloc");
			case Opcodes.ANEWARRAY -> curLabel.println("function mcj:heap/newarray");
			case Opcodes.CHECKCAST -> {}
//			case Opcodes.INSTANCEOF -> ;
			default -> throw new MCJException("Unsupported opcode: " + opcode);
		}
	}
	
	@Override
	public void visitVarInsn(int opcode, int varIndex) {
		switch (opcode) {
			case Opcodes.ILOAD -> curLabel.println("function mcj:localvars/push_var_to_stack {index:\"" + varIndex + "\"}");
			case Opcodes.LLOAD -> curLabel.println("function mcj:localvars/push_var_to_stack {index:\"" + varIndex + "\"}");
			case Opcodes.FLOAD -> curLabel.println("function mcj:localvars/push_var_to_stack {index:\"" + varIndex + "\"}");
			case Opcodes.DLOAD -> curLabel.println("function mcj:localvars/push_var_to_stack {index:\"" + varIndex + "\"}");
			case Opcodes.ALOAD -> curLabel.println("function mcj:localvars/push_var_to_stack {index:\"" + varIndex + "\"}");
			case Opcodes.ISTORE -> curLabel.println("function mcj:localvars/pop_var_from_stack {index:\"" + varIndex + "\"}");
			case Opcodes.LSTORE -> curLabel.println("function mcj:localvars/pop_var_from_stack {index:\"" + varIndex + "\"}");
			case Opcodes.FSTORE -> curLabel.println("function mcj:localvars/pop_var_from_stack {index:\"" + varIndex + "\"}");
			case Opcodes.DSTORE -> curLabel.println("function mcj:localvars/pop_var_from_stack {index:\"" + varIndex + "\"}");
			case Opcodes.ASTORE -> curLabel.println("function mcj:localvars/pop_var_from_stack {index:\"" + varIndex + "\"}");
			case Opcodes.RET -> throw new MCJException("Unsupported deprecated opcode: " + opcode);
			default -> throw new MCJException("Unsupported opcode: " + opcode);
		}
	}
	
	@Override
	public void visitEnd() {
		try {
			// Default entrypoint namespace:main
			if (provider.isMainMethod() && entrypoint == null) {
				try {
					provider.writeToActualFile(new File(provider.getCompiledFunctions(), "main.mcfunction"), """
							function mcj:setup
							function mcj:stack/push_const {value:"0"}
							function mcj:heap/newarray
							function mcj:stack/invokestatic {method:"$(~MAIN~)",num_args:"1",has_return:"false"}
							execute unless data storage mcj:data debug run function mcj:heap/free_all
							""".replace("$(~MAIN~)", provider.getActualFunctionPath(provider.getMethodFunctionsPath() + "/entry")));
				} catch (IOException e) {
					throw new MCJException("Error while creating main.mcfunction", e);
				}
			}
			
			// Custom entrypoint
			if (entrypoint != null) {
				StringBuilder args = new StringBuilder();
				for (String parameter : parameters) {
					args.append("$data modify storage mcj:data stack append value {value:$(");
					args.append(parameter);
					args.append(")}\n");
				}
				
				provider.writeToActualFile(new File(provider.getCompiledFunctions(), entrypoint), """
						function mcj:setup
						$(~ARGS~)function mcj:stack/invokestatic {method:"$(~MAIN~)",num_args:"$(~NUM_ARGS~)",has_return:"$(~HAS_RETURN~)"}
						execute unless data storage mcj:data debug run function mcj:heap/free_all
						""".replace("$(~MAIN~)", provider.getActualFunctionPath(provider.getMethodFunctionsPath() + "/entry"))
						.replace("$(~ARGS~)", args).replace("$(~NUM_ARGS~)", "" + parameters.length)
						.replace("$(~HAS_RETURN~)", provider.isVoidMethod() ? "false" : "true"));
			}
		} catch (IOException e) {
			throw new MCJException("Error saving method to files", e);
		}
		
		if (provider.isNativeMethod()) {
			if (!isNativeFound) {
				System.out.println("[Warning] Missing @MCJNativeImpl on a native method! "
						+ "Are you going to add the implementation manually? (" + provider.getMethodFunctionsPath() + ")");
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
			
			// Entry & combining label 0
			String entryValue = "function " + provider.getActualFunctionPath(provider.getMethodFunctionsPath() + "/l0");
			if (execute != null)
				entryValue = "execute " + execute + " run " + entryValue;
			else if (labels.get(0).canCombine)
				entryValue = labels.remove(0).getContents();
			
			provider.writeToFile(new File(provider.getMethodFunctions(), "entry.mcfunction"), entryValue);
			
			// Labels: writing
			for (MCJLabel label : labels)
				label.write();
		} catch (IOException e) {
			throw new MCJException("Error saving method to files", e);
		}
	}
	
}
