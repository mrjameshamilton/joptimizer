package eu.jameshamilton.optimizer.cfg;

import eu.jameshamilton.optimizer.ClassOptimizer;
import eu.jameshamilton.optimizer.OptimizationStats;
import java.lang.classfile.ClassHierarchyResolver;
import java.lang.classfile.CodeElement;
import java.lang.classfile.Instruction;
import java.lang.classfile.Label;
import java.lang.classfile.Opcode;
import java.lang.classfile.PseudoInstruction;
import java.lang.classfile.attribute.CodeAttribute;
import java.lang.classfile.constantpool.ClassEntry;
import java.lang.classfile.instruction.BranchInstruction;
import java.lang.classfile.instruction.ConstantInstruction;
import java.lang.classfile.instruction.ExceptionCatch;
import java.lang.classfile.instruction.FieldInstruction;
import java.lang.classfile.instruction.IncrementInstruction;
import java.lang.classfile.instruction.InvokeInstruction;
import java.lang.classfile.instruction.LabelTarget;
import java.lang.classfile.instruction.LineNumber;
import java.lang.classfile.instruction.LoadInstruction;
import java.lang.classfile.instruction.LocalVariable;
import java.lang.classfile.instruction.LookupSwitchInstruction;
import java.lang.classfile.instruction.NewObjectInstruction;
import java.lang.classfile.instruction.NewReferenceArrayInstruction;
import java.lang.classfile.instruction.ReturnInstruction;
import java.lang.classfile.instruction.StoreInstruction;
import java.lang.classfile.instruction.TableSwitchInstruction;
import java.lang.classfile.instruction.ThrowInstruction;
import java.lang.classfile.instruction.TypeCheckInstruction;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CfgUtil {
    public static void printCfgForClass(
            byte[] classBytes, ClassHierarchyResolver resolver, String filter, String sourceName) {
        var cm = java.lang.classfile.ClassFile.of().parse(classBytes);
        System.out.println("# Class: " + cm.thisClass().asInternalName() + " (" + sourceName + ")");
        cm.methods()
                .forEach(
                        m -> {
                            var name = m.methodName().stringValue();
                            var owner = cm.thisClass().asInternalName().replace('/', '.');
                            var descOpt =
                                    m.findAttribute(java.lang.classfile.Attributes.code())
                                                    .isPresent()
                                            ? m.methodTypeSymbol().descriptorString()
                                            : null;

                            if (filter != null) {
                                // filter format: Owner#method[:descriptor]
                                boolean ok = false;
                                var parts = filter.split("#", 2);
                                if (parts.length == 2) {
                                    var ownerPat = parts[0];
                                    var rest = parts[1];
                                    String methodPat = rest;
                                    String descPat = null;
                                    int colon = rest.indexOf(':');
                                    if (colon >= 0) {
                                        methodPat = rest.substring(0, colon);
                                        descPat = rest.substring(colon + 1);
                                    }
                                    ok =
                                            owner.endsWith(ownerPat)
                                                    && name.equals(methodPat)
                                                    && (descPat == null || descPat.equals(descOpt));
                                }
                                if (!ok) return;
                            }

                            m.findAttribute(java.lang.classfile.Attributes.code())
                                    .ifPresent(
                                            code -> {
                                                // Build elements and temporary ClassOptimizer
                                                // instance just to reuse buildCFG
                                                var stats = new OptimizationStats();
                                                var optimizer =
                                                        new ClassOptimizer(
                                                                stats, resolver, classBytes);
                                                var cfg = buildCFG(code);
                                                System.out.println(
                                                        "## Method: "
                                                                + name
                                                                + (descOpt != null
                                                                        ? " " + descOpt
                                                                        : ""));
                                                System.out.println(cfg);
                                            });
                        });
    }

    /**
     * Builds a Control Flow Graph (CFG) for the given CodeAttribute. The CFG consists of basic
     * blocks and their successor relationships.
     *
     * @param code the CodeAttribute to build the CFG for
     * @return a CFG representation of the code
     */
    private static CFG buildCFG(CodeAttribute code) {
        var elements =
                code.elementStream()
                        .filter(c -> !(c instanceof PseudoInstruction) || (c instanceof LabelTarget))
                        .toList();
        if (elements.isEmpty()) {
            return new CFG(elements, List.of(), List.of(), -1);
        }

        // Step 1: Identify block leaders (positions that start basic blocks)
        var leaders = new java.util.TreeSet<Integer>();
        leaders.add(0); // The first element is always a leader

        // Add exception handler starts as leaders
        for (var handler : code.exceptionHandlers()) {
            for (int i = 0; i < elements.size(); i++) {
                if (elements.get(i) instanceof Label label && label.equals(handler.handler())) {
                    leaders.add(i);
                    break;
                }
            }
        }

        // Scan through elements to find branch targets and instructions after branches
        for (int i = 0; i < elements.size(); i++) {
            var element = elements.get(i);

            if (!(element instanceof Instruction instr)) {
                continue;
            }

            switch (instr.opcode()) {
                case GOTO, GOTO_W -> {
                    addBranchTargetsAsLeaders(elements, instr, leaders);
                }
                case IFEQ,
                        IFNE,
                        IFLT,
                        IFGE,
                        IFGT,
                        IFLE,
                        IF_ICMPEQ,
                        IF_ICMPNE,
                        IF_ICMPLT,
                        IF_ICMPGE,
                        IF_ICMPGT,
                        IF_ICMPLE,
                        IF_ACMPEQ,
                        IF_ACMPNE,
                        IFNULL,
                        IFNONNULL -> {
                    addBranchTargetsAsLeaders(elements, instr, leaders);
                    if (i + 1 < elements.size()) {
                        leaders.add(i + 1);
                    }
                }
                // Switch instructions
                case TABLESWITCH, LOOKUPSWITCH -> {
                    addSwitchTargetsAsLeaders(elements, instr, leaders);
                    if (i + 1 < elements.size()) {
                        leaders.add(i + 1);
                    }
                }
                // Return instructions and throws end basic blocks
                case RETURN, IRETURN, LRETURN, FRETURN, DRETURN, ARETURN, ATHROW -> {
                }
            }
        }

        // Step 2: Create basic blocks
        var leaderList = new ArrayList<>(leaders);
        var blocks = new ArrayList<Block>();
        var labelToBlockMap = new HashMap<Label, Integer>();

        for (int i = 0; i < leaderList.size(); i++) {
            int start = leaderList.get(i);
            int end = (i + 1 < leaderList.size()) ? leaderList.get(i + 1) : elements.size();

            Block block = new Block(start, end, elements);
            blocks.add(block);

            // Map the potential start label to this block index
            for (int j = start; j < end && j < elements.size(); j++) {
                if (elements.get(j) instanceof Label label) {
                    labelToBlockMap.put(label, i);
                    break;
                }
            }
        }

        System.out.println(labelToBlockMap);

        // Step 3: Build successor relationships
        var successors = new ArrayList<List<Integer>>();

        for (int blockIdx = 0; blockIdx < blocks.size(); blockIdx++) {
            var block = blocks.get(blockIdx);
            var blockSuccessors = new ArrayList<Integer>();
            var lastInstr = block.getLastInstruction();

            if (lastInstr instanceof BranchInstruction branchInstruction) {
                addBranchSuccessors(labelToBlockMap, elements, blocks, branchInstruction, blockSuccessors);
                if (branchInstruction.opcode() != Opcode.GOTO &&
                    branchInstruction.opcode() != Opcode.GOTO_W) {
                    // Fall-through to next block
                    if (blockIdx + 1 < blocks.size()) {
                        blockSuccessors.add(blockIdx + 1);
                    }
                }
            } else if (lastInstr instanceof TableSwitchInstruction tableSwitchInstruction) {
                addSwitchSuccessors(labelToBlockMap, elements, blocks, tableSwitchInstruction, blockSuccessors);
            } else if (lastInstr instanceof LookupSwitchInstruction lookupSwitchInstruction) {
                addSwitchSuccessors(labelToBlockMap, elements, blocks, lookupSwitchInstruction, blockSuccessors);
            } else if (lastInstr instanceof ReturnInstruction || lastInstr instanceof ThrowInstruction) {
                // nothing
            } else {
                // Fall-through to next block
                if (blockIdx + 1 < blocks.size()) {
                    blockSuccessors.add(blockIdx + 1);
                }
            }

            successors.add(blockSuccessors);
        }

        return new CFG(elements, blocks, successors, 0);
    }

    private static void addBranchTargetsAsLeaders(
            List<CodeElement> elements, Instruction instr, java.util.Set<Integer> leaders) {
        Label target;
        if (instr instanceof BranchInstruction branchInstr) {
            target = branchInstr.target();
        } else {
            target = null;
        }
        if (target != null) {
            addLabelAsLeader(elements, target, leaders);
        }
    }

    private static void addSwitchTargetsAsLeaders(
            List<CodeElement> elements, Instruction instr, java.util.Set<Integer> leaders) {
        if (instr instanceof TableSwitchInstruction tableSwitch) {
            // Add default target
            addLabelAsLeader(elements, tableSwitch.defaultTarget(), leaders);

            // Add case targets
            for (var switchCase : tableSwitch.cases()) {
                addLabelAsLeader(elements, switchCase.target(), leaders);
            }
        } else if (instr instanceof LookupSwitchInstruction lookupSwitch) {
            // Add default target
            addLabelAsLeader(elements, lookupSwitch.defaultTarget(), leaders);

            // Add case targets
            for (var switchCase : lookupSwitch.cases()) {
                addLabelAsLeader(elements, switchCase.target(), leaders);
            }
        }
    }

    private static void addLabelAsLeader(
            List<CodeElement> elements, Label target, java.util.Set<Integer> leaders) {
        for (int i = 0; i < elements.size(); i++) {
            if (elements.get(i) instanceof Label label && label.equals(target)) {
                leaders.add(i);
                break;
            }
        }
    }

    private static void addBranchSuccessors(
            HashMap<Label, Integer> labelToBlockMap,
            List<CodeElement> elements,
            List<Block> blocks,
            BranchInstruction branchInstr,
            List<Integer> successors) {
        Integer targetBlock = labelToBlockMap.get(branchInstr.target());
        assert targetBlock != null;
        if (!successors.contains(targetBlock)) {
            successors.add(targetBlock);
        }
    }

    private static void addSwitchSuccessors(
            HashMap<Label, Integer> labelToBlockMap,
            List<CodeElement> elements,
            List<Block> blocks,
            Instruction instr,
            List<Integer> successors) {

        if (instr instanceof TableSwitchInstruction tableSwitch) {
            // Add default case
            // addLabelAsSuccessor(elements, blocks, tableSwitch.defaultTarget(), successors);
            addLabelAsSuccessorWithMap(labelToBlockMap, tableSwitch.defaultTarget(), successors);
            // Add case targets
            for (var switchCase : tableSwitch.cases()) {
                // addLabelAsSuccessor(elements, blocks, switchCase.target(), successors);
                addLabelAsSuccessorWithMap(labelToBlockMap, switchCase.target(), successors);
            }
        } else if (instr instanceof LookupSwitchInstruction lookupSwitch) {
            // Add default case
            // addLabelAsSuccessor(elements, blocks, lookupSwitch.defaultTarget(), successors);
            addLabelAsSuccessorWithMap(labelToBlockMap, lookupSwitch.defaultTarget(), successors);
            // Add case targets
            for (var switchCase : lookupSwitch.cases()) {
                // addLabelAsSuccessor(elements, blocks, switchCase.target(), successors);
                addLabelAsSuccessorWithMap(labelToBlockMap, switchCase.target(), successors);
            }
        }
    }

    private static void addLabelAsSuccessor(
            List<CodeElement> elements,
            List<Block> blocks,
            Label target,
            List<Integer> successors) {
        int targetBlock = findBlockContainingLabel(elements, blocks, target);
        if (targetBlock >= 0 && !successors.contains(targetBlock)) {
            successors.add(targetBlock);
        }
    }

    private static void addLabelAsSuccessorWithMap(
            Map<Label, Integer> labelToBlockMap, Label target, List<Integer> successors) {
        Integer targetBlock = labelToBlockMap.get(target);
        if (targetBlock != null && !successors.contains(targetBlock)) {
            successors.add(targetBlock);
        }
    }

    private static int findBlockContainingLabel(
            List<CodeElement> elements, List<Block> blocks, Label target) {
        for (int i = 0; i < elements.size(); i++) {
            if (elements.get(i) instanceof Label label && label.equals(target)) {
                // Find which block contains this position
                for (int blockIdx = 0; blockIdx < blocks.size(); blockIdx++) {
                    var block = blocks.get(blockIdx);
                    if (i >= block.start && i < block.end) {
                        return blockIdx;
                    }
                }
                break;
            }
        }
        return -1;
    }

    // For ad-hoc CFG inspection: run `java ... eu.jameshamilton.optimizer.ClassOptimizer
    // <class-or-jar> [owner#methodName[:descriptor]]`
    static void main(String[] args) throws Exception {
        if (args.length < 1 || args.length > 2) {
            System.err.println("Usage:");
            System.err.println(
                    "  Class: java ... ClassOptimizer path/to/Foo.class [Owner#method[:desc]]");
            System.err.println(
                    "  Jar:   java ... ClassOptimizer path/to/app.jar [Owner#method[:desc]]");
            return;
        }
        var input = java.nio.file.Path.of(args[0]);
        String filter = args.length == 2 ? args[1] : null;

        if (java.nio.file.Files.isDirectory(input)) {
            System.err.println("Provide a .class or .jar file.");
            return;
        }

        if (input.getFileName().toString().endsWith(".jar")) {
            try (var jar = new java.util.jar.JarFile(input.toString())) {
                var resolver =
                        new eu.jameshamilton.classfile.JarClassHierarchyResolver(jar)
                                .orElse(ClassHierarchyResolver.defaultResolver());
                var entries = jar.entries();
                while (entries.hasMoreElements()) {
                    var e = entries.nextElement();
                    if (!e.getName().endsWith(".class")) continue;
                    try (var is = jar.getInputStream(e)) {
                        byte[] bytes = is.readAllBytes();
                        printCfgForClass(bytes, resolver, filter, e.getName());
                    }
                }
            }
        } else if (input.getFileName().toString().endsWith(".class")) {
            byte[] bytes = java.nio.file.Files.readAllBytes(input);
            printCfgForClass(
                    bytes, ClassHierarchyResolver.defaultResolver(), filter, input.toString());
        } else {
            System.err.println("Unsupported file: " + input);
        }
    }

    /**
     * Represents a basic block within a control flow graph (CFG). A basic block is characterized by
     * a contiguous sequence of instructions from a given start index (inclusive) to an end index
     * (exclusive). It may also include an optional starting label, which could be null if no label
     * starts the block.
     */
    // Basic block / CFG structures
    private record Block(int start, int end, Label startLabel, List<Instruction> instructions) {

        // Compact constructor to process elements and create derived fields
        Block(int start, int end, List<CodeElement> elements) {
            this(
                    start,
                    end,
                    findStartLabel(elements, start, end),
                    extractInstructions(elements, start, end));
        }

        // Convenience method to get the last instruction
        public Instruction getLastInstruction() {
            return instructions.getLast();
        }

        // Check if this block starts with the given label
        public boolean startsWithLabel(Label label) {
            return startLabel != null && startLabel.equals(label);
        }

        // Helper method to find the first label in the block
        private static Label findStartLabel(List<CodeElement> elements, int start, int end) {
            for (int i = start; i < end && i < elements.size(); i++) {
                if (elements.get(i) instanceof Label label) {
                    return label;
                }
            }
            return null;
        }

        // Helper method to extract instructions from the block
        private static List<Instruction> extractInstructions(
                List<CodeElement> elements, int start, int end) {
            var blockInstructions = new ArrayList<Instruction>();
            for (int i = start; i < end && i < elements.size(); i++) {
                if (elements.get(i) instanceof Instruction instr) {
                    blockInstructions.add(instr);
                }
            }
            return List.copyOf(blockInstructions);
        }
    }

    private record CFG(
            List<CodeElement> elements,
            List<Block> blocks,
            List<List<Integer>> successors,
            int entryBlock) {

        @Override
        public String toString() {
            var sb = new StringBuilder();
            sb.append("digraph CFG {\n");
            sb.append("  rankdir=TB;\n");
            sb.append("  node [shape=box, fontname=\"Courier\"];\n\n");

            // Create nodes for each basic block
            for (int i = 0; i < blocks.size(); i++) {
                var block = blocks.get(i);
                var blockSuccessors = successors.get(i);
                sb.append("  block").append(i).append(" [label=\"");

                // Add block header
                sb.append("Block ").append(i);
                if (i == entryBlock) {
                    sb.append(" (Entry)");
                }
                sb.append("\\n");

                // Add instructions from the stored instruction list
                for (int instrIdx = 0; instrIdx < block.instructions().size(); instrIdx++) {
                    var instr = block.instructions().get(instrIdx);
                    boolean isLastInstr = (instrIdx == block.instructions().size() - 1);
                    sb.append(formatInstruction(instr, blockSuccessors, isLastInstr));
                    sb.append("\\n");
                }

                // Also show exception catches if they exist in this block
                for (int elemIdx = block.start(); elemIdx < block.end(); elemIdx++) {
                    var element = elements.get(elemIdx);
                    if (element instanceof ExceptionCatch exCatch) {
                        sb.append("catch ")
                                .append(
                                        exCatch.catchType()
                                                .map(ClassEntry::asInternalName)
                                                .orElse("any"));
                        sb.append("\\n");
                    }
                }

                sb.append("\"];\n");
            }

            sb.append("\n");

            // Create edges for control flow
            for (int i = 0; i < successors.size(); i++) {
                var blockSuccessors = successors.get(i);
                for (var successor : blockSuccessors) {
                    sb.append("  block")
                            .append(i)
                            .append(" -> block")
                            .append(successor)
                            .append(";\n");
                }
            }

            sb.append("}");
            return sb.toString();
        }

        private String formatInstruction(
                Instruction instr, List<Integer> blockSuccessors, boolean isLastInstr) {
            var sb = new StringBuilder();
            sb.append(instr.opcode().name().toLowerCase());

            // Add operands/parameters based on instruction type
            switch (instr.opcode()) {
                case LDC, LDC_W, LDC2_W -> {
                    if (instr instanceof ConstantInstruction constInstr) {
                        sb.append(" ").append(formatConstant(constInstr.constantValue()));
                    }
                }
                case BIPUSH, SIPUSH -> {
                    if (instr instanceof ConstantInstruction constInstr) {
                        sb.append(" ").append(constInstr.constantValue());
                    }
                }
                case ILOAD, LLOAD, FLOAD, DLOAD, ALOAD, ISTORE, LSTORE, FSTORE, DSTORE, ASTORE -> {
                    if (instr instanceof LoadInstruction loadInstr) {
                        sb.append(" ").append(loadInstr.slot());
                    } else if (instr instanceof StoreInstruction storeInstr) {
                        sb.append(" ").append(storeInstr.slot());
                    }
                }
                case GETFIELD, PUTFIELD, GETSTATIC, PUTSTATIC -> {
                    if (instr instanceof FieldInstruction fieldInstr) {
                        sb.append(" ")
                                .append(fieldInstr.field().owner().asInternalName())
                                .append(".")
                                .append(fieldInstr.field().name().stringValue());
                    }
                }
                case INVOKEVIRTUAL, INVOKESPECIAL, INVOKESTATIC, INVOKEINTERFACE -> {
                    if (instr instanceof InvokeInstruction invokeInstr) {
                        sb.append(" ")
                                .append(invokeInstr.method().owner().asInternalName())
                                .append(".")
                                .append(invokeInstr.method().name().stringValue());
                    }
                }
                case NEW, ANEWARRAY, CHECKCAST, INSTANCEOF -> {
                    switch (instr) {
                        case TypeCheckInstruction typeInstr ->
                                sb.append(" ").append(typeInstr.type().asInternalName());
                        case NewObjectInstruction newInstr ->
                                sb.append(" ").append(newInstr.className().asInternalName());
                        case NewReferenceArrayInstruction newArrayInstr ->
                                sb.append(" ")
                                        .append(newArrayInstr.componentType().asInternalName());
                        default -> {}
                    }
                }
                case GOTO, GOTO_W -> {
                    // For unconditional branches, there should be exactly one successor
                    if (isLastInstr && !blockSuccessors.isEmpty()) {
                        sb.append(" -> block").append(blockSuccessors.getFirst());
                    } else if (instr instanceof BranchInstruction branchInstr) {
                        sb.append(" -> ").append(branchInstr.target().toString());
                    }
                }
                case IFEQ,
                        IFNE,
                        IFLT,
                        IFGE,
                        IFGT,
                        IFLE,
                        IF_ICMPEQ,
                        IF_ICMPNE,
                        IF_ICMPLT,
                        IF_ICMPGE,
                        IF_ICMPGT,
                        IF_ICMPLE,
                        IF_ACMPEQ,
                        IF_ACMPNE,
                        IFNULL,
                        IFNONNULL -> {
                    // For conditional branches, we need to figure out which successor is the branch
                    // target
                    if (isLastInstr && instr instanceof BranchInstruction branchInstr) {
                        // Find the target block by matching the label
                        int targetBlock = findBlockWithLabel(branchInstr.target());
                        if (targetBlock >= 0) {
                            sb.append(" -> block").append(targetBlock);
                        } else {
                            sb.append(" -> ").append(branchInstr.target().toString());
                        }
                    } else if (instr instanceof BranchInstruction branchInstr) {
                        sb.append(" -> ").append(branchInstr.target());
                    }
                }
                case TABLESWITCH -> {
                    if (instr instanceof TableSwitchInstruction switchInstr) {
                        sb.append(" [")
                                .append(switchInstr.lowValue())
                                .append("-")
                                .append(switchInstr.highValue())
                                .append("] -> {");
                        if (isLastInstr) {
                            sb.append("blocks: ");
                            for (int i = 0; i < blockSuccessors.size(); i++) {
                                if (i > 0) sb.append(", ");
                                sb.append("block").append(blockSuccessors.get(i));
                            }
                        }
                        sb.append("}");
                    }
                }
                case LOOKUPSWITCH -> {
                    if (instr instanceof LookupSwitchInstruction switchInstr) {
                        sb.append(" (").append(switchInstr.cases().size()).append(" cases) -> {");
                        if (isLastInstr) {
                            sb.append("blocks: ");
                            for (int i = 0; i < blockSuccessors.size(); i++) {
                                if (i > 0) sb.append(", ");
                                sb.append("block").append(blockSuccessors.get(i));
                            }
                        }
                        sb.append("}");
                    }
                }
                case IINC, IINC_W -> {
                    if (instr instanceof IncrementInstruction incrementInstruction) {
                        sb.append(incrementInstruction.slot())
                                .append(", ")
                                .append(incrementInstruction.constant());
                    }
                }
            }

            return sb.toString();
        }

        // Helper method to find block containing a specific label
        private int findBlockWithLabel(Label target) {
            for (int i = 0; i < blocks.size(); i++) {
                if (blocks.get(i).startsWithLabel(target)) {
                    return i;
                }
            }
            return -1;
        }

        private String formatConstant(Object constant) {
            if (constant instanceof String str) {
                // Escape quotes and newlines for DOT format
                return "\""
                        + str.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n")
                        + "\"";
            } else {
                return constant.toString();
            }
        }
    }
}
