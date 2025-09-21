package eu.jameshamilton.classfile.matcher;

import java.lang.classfile.CodeElement;
import java.lang.classfile.Instruction;
import java.lang.classfile.Label;
import java.lang.classfile.Opcode;
import java.lang.classfile.TypeKind;
import java.lang.classfile.constantpool.ClassEntry;
import java.lang.classfile.instruction.BranchInstruction;
import java.lang.classfile.instruction.ConstantInstruction;
import java.lang.classfile.instruction.FieldInstruction;
import java.lang.classfile.instruction.IncrementInstruction;
import java.lang.classfile.instruction.InvokeInstruction;
import java.lang.classfile.instruction.LoadInstruction;
import java.lang.classfile.instruction.NewObjectInstruction;
import java.lang.classfile.instruction.StoreInstruction;
import java.lang.classfile.instruction.TypeCheckInstruction;
import java.lang.constant.ClassDesc;
import java.lang.constant.ConstantDesc;
import java.lang.constant.MethodTypeDesc;

import static java.lang.classfile.Opcode.CHECKCAST;
import static java.lang.classfile.Opcode.D2I;
import static java.lang.classfile.Opcode.DMUL;
import static java.lang.classfile.Opcode.DNEG;
import static java.lang.classfile.Opcode.DSUB;
import static java.lang.classfile.Opcode.FMUL;
import static java.lang.classfile.Opcode.FNEG;
import static java.lang.classfile.Opcode.FSUB;
import static java.lang.classfile.Opcode.GETSTATIC;
import static java.lang.classfile.Opcode.GOTO;
import static java.lang.classfile.Opcode.GOTO_W;
import static java.lang.classfile.Opcode.IFEQ;
import static java.lang.classfile.Opcode.IFGE;
import static java.lang.classfile.Opcode.IFLE;
import static java.lang.classfile.Opcode.IFNE;
import static java.lang.classfile.Opcode.IINC;
import static java.lang.classfile.Opcode.LMUL;
import static java.lang.classfile.Opcode.LNEG;
import static java.lang.classfile.Opcode.LSUB;
import static java.lang.classfile.Opcode.PUTSTATIC;

public class InstructionMatchers {
    public static <T> Matcher<T> instruction(Opcode opcode) {
        return e -> e instanceof Instruction i && i.opcode() == opcode;
    }

    public static <T> Matcher<T> instruction(Matcher<Opcode> opcode) {
        return e -> e instanceof Instruction i && opcode.matches(i.opcode());
    }

    public static <T> Matcher<T> iinc(Matcher<Integer> slot, Matcher<Integer> amount) {
        return e -> e instanceof IncrementInstruction i &&
            slot.matches(i.slot()) && amount.matches(i.constant());
    }

    // Loading Constants
    public static <T> Matcher<T> aconst_null() {
        return e -> e instanceof ConstantInstruction c && c.constantValue() == null;
    }

    public static <T> Matcher<T> iconst(int value) {
        return e -> e instanceof ConstantInstruction c &&
            c.constantValue() instanceof Integer i && i == value;
    }

    public static <T> Matcher<T> lconst(long value) {
        return e -> e instanceof ConstantInstruction c &&
            c.constantValue() instanceof Long l && l == value;
    }

    public static <T> Matcher<T> fconst(float value) {
        return e -> e instanceof ConstantInstruction c &&
            c.constantValue() instanceof Float f && f == value;
    }

    public static <T> Matcher<T> dconst(double value) {
        return e -> e instanceof ConstantInstruction c &&
            c.constantValue() instanceof Double d && d == value;
    }

    // Loading Local Variables
    public static <T> Matcher<T> iload(int slot) {
        return e -> e instanceof LoadInstruction l &&
            l.opcode() == Opcode.ILOAD && l.slot() == slot;
    }

    public static <T> Matcher<T> lload(int slot) {
        return e -> e instanceof LoadInstruction l &&
            l.opcode() == Opcode.LLOAD && l.slot() == slot;
    }

    public static <T> Matcher<T> fload(int slot) {
        return e -> e instanceof LoadInstruction l &&
            l.opcode() == Opcode.FLOAD && l.slot() == slot;
    }

    public static <T> Matcher<T> dload(int slot) {
        return e -> e instanceof LoadInstruction l &&
            l.opcode() == Opcode.DLOAD && l.slot() == slot;
    }

    public static <T> Matcher<T> aload(Matcher<Integer> slot) {
        return e -> e instanceof LoadInstruction l &&
            l.opcode() == Opcode.ALOAD && slot.matches(l.slot());
    }

    // Storing Local Variables
    public static <T> Matcher<T> istore(int slot) {
        return e -> e instanceof StoreInstruction s &&
            s.opcode() == Opcode.ISTORE && s.slot() == slot;
    }

    public static <T> Matcher<T> lstore(int slot) {
        return e -> e instanceof StoreInstruction s &&
            s.opcode() == Opcode.LSTORE && s.slot() == slot;
    }

    public static <T> Matcher<T> fstore(int slot) {
        return e -> e instanceof StoreInstruction s &&
            s.opcode() == Opcode.FSTORE && s.slot() == slot;
    }

    public static <T> Matcher<T> dstore(int slot) {
        return e -> e instanceof StoreInstruction s &&
            s.opcode() == Opcode.DSTORE && s.slot() == slot;
    }

    public static <T> Matcher<T> astore(int slot) {
        return e -> e instanceof StoreInstruction s &&
            s.opcode() == Opcode.ASTORE && s.slot() == slot;
    }

    // Array Operations
    public static <T> Matcher<T> iaload() {
        return e -> e instanceof Instruction i && i.opcode() == Opcode.IALOAD;
    }

    public static <T> Matcher<T> laload() {
        return e -> e instanceof Instruction i && i.opcode() == Opcode.LALOAD;
    }

    public static <T> Matcher<T> faload() {
        return e -> e instanceof Instruction i && i.opcode() == Opcode.FALOAD;
    }

    public static <T> Matcher<T> daload() {
        return e -> e instanceof Instruction i && i.opcode() == Opcode.DALOAD;
    }

    public static <T> Matcher<T> aaload() {
        return e -> e instanceof Instruction i && i.opcode() == Opcode.AALOAD;
    }

    public static <T> Matcher<T> baload() {
        return e -> e instanceof Instruction i && i.opcode() == Opcode.BALOAD;
    }

    public static <T> Matcher<T> caload() {
        return e -> e instanceof Instruction i && i.opcode() == Opcode.CALOAD;
    }

    public static <T> Matcher<T> saload() {
        return e -> e instanceof Instruction i && i.opcode() == Opcode.SALOAD;
    }

    // Stack Operations
    public static <T> Matcher<T> pop() {
        return e -> e instanceof Instruction i && i.opcode() == Opcode.POP;
    }

    public static <T> Matcher<T> pop2() {
        return e -> e instanceof Instruction i && i.opcode() == Opcode.POP2;
    }

    public static <T> Matcher<T> dup() {
        return e -> e instanceof Instruction i && i.opcode() == Opcode.DUP;
    }

    public static <T> Matcher<T> dup_x1() {
        return e -> e instanceof Instruction i && i.opcode() == Opcode.DUP_X1;
    }

    public static <T> Matcher<T> dup_x2() {
        return e -> e instanceof Instruction i && i.opcode() == Opcode.DUP_X2;
    }

    public static <T> Matcher<T> dup2() {
        return e -> e instanceof Instruction i && i.opcode() == Opcode.DUP2;
    }

    public static <T> Matcher<T> dup2_x1() {
        return e -> e instanceof Instruction i && i.opcode() == Opcode.DUP2_X1;
    }

    public static <T> Matcher<T> dup2_x2() {
        return e -> e instanceof Instruction i && i.opcode() == Opcode.DUP2_X2;
    }

    public static <T> Matcher<T> swap() {
        return e -> e instanceof Instruction i && i.opcode() == Opcode.SWAP;
    }

    // Math Operations
    public static <T> Matcher<T> iadd() {
        return e -> e instanceof Instruction i && i.opcode() == Opcode.IADD;
    }

    public static <T> Matcher<T> ladd() {
        return e -> e instanceof Instruction i && i.opcode() == Opcode.LADD;
    }

    public static <T> Matcher<T> fadd() {
        return e -> e instanceof Instruction i && i.opcode() == Opcode.FADD;
    }

    public static <T> Matcher<T> dadd() {
        return e -> e instanceof Instruction i && i.opcode() == Opcode.DADD;
    }

    public static <T> Matcher<T> isub() {
        return e -> e instanceof Instruction i && i.opcode() == Opcode.ISUB;
    }

    public static <T> Matcher<T> fsub() {
        return instruction(FSUB);
    }

    public static <T> Matcher<T> dsub() {
        return instruction(DSUB);
    }

    public static <T> Matcher<T> lsub() {
        return instruction(LSUB);
    }

    public static <T> Matcher<T> imul() {
        return e -> e instanceof Instruction i && i.opcode() == Opcode.IMUL;
    }

    public static <T> Matcher<T> lmul() {
        return instruction(LMUL);
    }

    public static <T> Matcher<T> dmul() {
        return instruction(DMUL);
    }

    public static <T> Matcher<T> fmul() {
        return instruction(FMUL);
    }

    public static <T> Matcher<T> ineg() {
        return e -> e instanceof Instruction i && i.opcode() == Opcode.INEG;
    }

    public static <T> Matcher<T> lneg() {
        return instruction(LNEG);
    }

    public static <T> Matcher<T> dneg() {
        return instruction(DNEG);
    }

    public static <T> Matcher<T> fneg() {
        return instruction(FNEG);
    }

    // Type Conversion
    public static <T> Matcher<T> i2l() {
        return e -> e instanceof Instruction i && i.opcode() == Opcode.I2L;
    }

    public static <T> Matcher<T> i2f() {
        return e -> e instanceof Instruction i && i.opcode() == Opcode.I2F;
    }

    public static <T> Matcher<T> i2d() {
        return e -> e instanceof Instruction i && i.opcode() == Opcode.I2D;
    }

    public static <T> Matcher<T> i2s() {
        return e -> e instanceof Instruction i && i.opcode() == Opcode.I2S;
    }

    public static <T> Matcher<T> i2c() {
        return e -> e instanceof Instruction i && i.opcode() == Opcode.I2C;
    }

    public static <T> Matcher<T> i2b() {
        return e -> e instanceof Instruction i && i.opcode() == Opcode.I2B;
    }

    public static <T> Matcher<T> l2i() {
        return e -> e instanceof Instruction i && i.opcode() == Opcode.L2I;
    }

    public static <T> Matcher<T> d2i() {
        return instruction(D2I);
    }

    public static <T> Matcher<T> checkcast(Matcher<ClassEntry> classDesc) {
        return e -> e instanceof TypeCheckInstruction i &&
            i.opcode() == CHECKCAST
            && classDesc.matches(i.type());
    }

    public static <T> Matcher<T> nop() {
        return e -> e instanceof Instruction i && i.opcode() == Opcode.NOP;
    }

    public static <T> Matcher<T> getstatic(String owner, String name, String desc) {
        return getstatic(ClassDesc.of(owner), name, ClassDesc.of(desc));
    }

    public static <T> Matcher<T> getstatic(ClassDesc owner, String name, ClassDesc type) {
        return e -> e instanceof FieldInstruction f &&
            f.opcode() == GETSTATIC &&
            f.field().owner().asSymbol().equals(owner) &&
            f.field().name().stringValue().equals(name) &&
            f.field().typeSymbol().equals(type);
    }

    public static <T> Matcher<T> getstatic(Matcher<ClassDesc> owner, Matcher<String> name, Matcher<ClassDesc> type) {
        return e -> e instanceof FieldInstruction f &&
            f.opcode() == GETSTATIC &&
            owner.matches(f.field().owner().asSymbol()) &&
            name.matches(f.field().name().stringValue()) &&
            type.matches(f.field().typeSymbol());
    }

    public static <T> Matcher<T> putstatic(Matcher<ClassDesc> owner, Matcher<String> name, Matcher<ClassDesc> type) {
        return e -> e instanceof FieldInstruction f &&
            f.opcode() == PUTSTATIC &&
            owner.matches(f.field().owner().asSymbol()) &&
            name.matches(f.field().name().stringValue()) &&
            type.matches(f.field().typeSymbol());
    }

    public static <T> Matcher<T> getfield(Matcher<ClassDesc> owner, Matcher<String> name, Matcher<ClassDesc> type) {
        return e -> e instanceof FieldInstruction f &&
            f.opcode() == Opcode.GETFIELD &&
            owner.matches(f.field().owner().asSymbol()) &&
            name.matches(f.field().name().stringValue()) &&
            type.matches(f.field().typeSymbol());
    }

    public static <T> Matcher<T> putfield(Matcher<ClassDesc> owner, Matcher<String> name, Matcher<ClassDesc> type) {
        return e -> e instanceof FieldInstruction f &&
            f.opcode() == Opcode.PUTFIELD &&
            owner.matches(f.field().owner().asSymbol()) &&
            name.matches(f.field().name().stringValue()) &&
            type.matches(f.field().typeSymbol());

    }

    public static <T> Matcher<T> invokevirtual(String owner, String name, String desc) {
        return invokevirtual(ClassDesc.of(owner), name, MethodTypeDesc.ofDescriptor(desc));
    }

    public static <T> Matcher<T> invokevirtual(Matcher<ClassDesc> owner, Matcher<String> name, Matcher<MethodTypeDesc> type) {
        return e -> e instanceof InvokeInstruction i &&
            i.opcode() == Opcode.INVOKEVIRTUAL &&
            owner.matches(i.method().owner().asSymbol()) &&
            name.matches(i.method().name().stringValue()) &&
            type.matches(i.typeSymbol());
    }

    public static <T> Matcher<T> invokevirtual(ClassDesc owner, String name, MethodTypeDesc type) {
        return e -> e instanceof InvokeInstruction i &&
            i.opcode() == Opcode.INVOKEVIRTUAL &&
            i.method().owner().asSymbol().equals(owner) &&
            i.method().name().stringValue().equals(name) &&
            i.typeSymbol().equals(type);
    }

    public static <T> Matcher<T> invokespecial(Matcher<ClassDesc> owner, Matcher<String> name, Matcher<MethodTypeDesc> type) {
        return e -> e instanceof InvokeInstruction i &&
            i.opcode() == Opcode.INVOKESPECIAL &&
            owner.matches(i.method().owner().asSymbol()) &&
            name.matches(i.method().name().stringValue()) &&
            type.matches(i.typeSymbol());
    }

    public static <T> Matcher<T> invokestatic(Matcher<ClassDesc> owner, Matcher<String> name, Matcher<MethodTypeDesc> type) {
        return e -> e instanceof InvokeInstruction i &&
            i.opcode() == Opcode.INVOKESTATIC &&
            owner.matches(i.method().owner().asSymbol()) &&
            name.matches(i.method().name().stringValue()) &&
            type.matches(i.typeSymbol());

    }

    public static <T> Matcher<T> goto_(Matcher<Label> label) {
        return e -> e instanceof BranchInstruction b
            && (b.opcode() == GOTO || b.opcode() == GOTO_W) &&
            label.matches(b.target());
    }

    public static <T> Matcher<T> ifeq(Matcher<Label> labelMatcher) {
        return e -> e instanceof BranchInstruction b &&
            b.opcode() == IFEQ &&
            labelMatcher.matches(b.target());
    }

    public static <T> Matcher<T> ifne(Matcher<Label> labelMatcher) {
        return e -> e instanceof BranchInstruction b &&
            b.opcode() == IFNE &&
            labelMatcher.matches(b.target());
    }

    public static Matcher<CodeElement> label(Matcher<Label> label) {
        return e -> e instanceof Label l &&
            label.matches(l);
    }

    public static <T> Matcher<T> ifle(Matcher<Label> labelMatcher) {
        return e -> e instanceof BranchInstruction b &&
            b.opcode() == IFLE &&
            labelMatcher.matches(b.target());
    }

    public static <T> Matcher<T> ifge(Matcher<Label> labelMatcher) {
        return e -> e instanceof BranchInstruction b &&
            b.opcode() == IFGE &&
            labelMatcher.matches(b.target());
    }

    // Utility matchers with captures
    public static Matcher<CodeElement> loadInstruction(Matcher<TypeKind> type, Matcher<Integer> slot) {
        return e -> e instanceof LoadInstruction l &&
            type.matches(l.typeKind()) &&
            slot.matches(l.slot());
    }

    public static Matcher<CodeElement> storeInstruction(Matcher<TypeKind> type, Matcher<Integer> slot) {
        return e -> e instanceof StoreInstruction s &&
            type.matches(s.typeKind()) &&
            slot.matches(s.slot());
    }

    public static <T extends ConstantDesc, Y extends CodeElement> Matcher<Y> constantInstruction(Matcher<T> value) {
        return e -> {
            try {
                return e instanceof ConstantInstruction c &&
                    value.matches((T) c.constantValue());
            } catch (ClassCastException _) {
                return false;
            }
        };
    }

    public static <T> Matcher<T> newObjectInstruction(ClassDesc classDesc) {
        return newObjectInstruction(e -> e.equals(classDesc));
    }

    public static <T> Matcher<T> newObjectInstruction(Matcher<ClassDesc> classDesc) {
        return e -> e instanceof NewObjectInstruction n && classDesc.matches(n.className().asSymbol());
    }
}
