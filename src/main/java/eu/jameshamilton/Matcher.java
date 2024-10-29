package eu.jameshamilton;

import java.lang.classfile.CodeElement;
import java.lang.classfile.Instruction;
import java.lang.classfile.TypeKind;
import java.lang.classfile.instruction.ConstantInstruction;
import java.lang.classfile.instruction.LoadInstruction;
import java.lang.constant.ConstantDesc;

import static java.lang.classfile.Opcode.IADD;
import static java.lang.classfile.Opcode.ILOAD;
import static java.lang.classfile.Opcode.POP;
import static java.lang.classfile.Opcode.POP2;

@SuppressWarnings("preview")
public interface Matcher0<L> {
    boolean matches(L codeElement);
    
    final class Capture<T> {
        private T value;

        boolean matchOrSet(T newValue) {
            if (value == null) {
                value = newValue;
                return true;
            }
            return value.equals(newValue);
        }
        
        T get() {
            return value;
        }
    }

    final class Any<S> implements Matcher0<S> {
        private Any() {}

        @Override
        public String toString() {
            return "Any";
        }

        public static <X> Any<X> any() {
            return new Any<>();
        }

        @Override
        public boolean matches(S element) {
            return true;
        }
    }


    // Generic constant matcher
    static Matcher0 constantInstruction(Capture<ConstantDesc> value) {
        return e -> e instanceof ConstantInstruction c &&
                   value.matchOrSet(c.constantValue());
    }

    // Specific constant matchers following ClassFile API
    static Matcher0 bipush(Capture<Integer> value) {
        return e -> e instanceof ConstantInstruction c && 
                   c.constantValue() instanceof Integer i &&
                   i.byteValue() == i &&
                   value.matchOrSet(i);
    }

    static Matcher0 sipush(Capture<Integer> value) {
        return e -> e instanceof ConstantInstruction c && 
                   c.constantValue() instanceof Integer i &&
                   i.shortValue() == i &&
                   value.matchOrSet(i);
    }

    static Matcher0 fconst(Capture<Float> value) {
        return e -> e instanceof ConstantInstruction c && 
                   c.constantValue() instanceof Float f &&
                   value.matchOrSet(f);
    }

    static Matcher0 dconst(Capture<Double> value) {
        return e -> e instanceof ConstantInstruction c && 
                   c.constantValue() instanceof Double d &&
                   value.matchOrSet(d);
    }

    static Matcher0 lconst(Capture<Long> value) {
        return e -> e instanceof ConstantInstruction c && 
                   c.constantValue() instanceof Long l &&
                   value.matchOrSet(l);
    }

    static Matcher0 iconst(int value) {
        return e -> e instanceof ConstantInstruction c && 
                   c.constantValue() instanceof Integer i &&
                   i == value;
    }

    static Matcher0 iconst(Capture<Integer> value) {
        return e -> e instanceof ConstantInstruction c && 
                   c.constantValue() instanceof Integer i &&
                   value.matchOrSet(i);
    }

    // Other instruction matchers...
    static Matcher0 iload(Capture<Integer> slot) {
        return e -> e instanceof LoadInstruction l &&
                   l.opcode() == ILOAD &&
                   slot.matchOrSet(l.slot());
    }

    static Matcher0 pop() {
        return e -> e instanceof Instruction p && p.opcode() == POP;
    }

    static Matcher0 pop2() {
        return e -> e instanceof Instruction p && p.opcode() == POP2;
    }

    static Matcher0 iadd() {
        return e -> e instanceof Instruction i && i.opcode() == IADD;
    }

    static Matcher0 loadInstruction(Capture<TypeKind> type, Capture<Integer> slot) {
        return element -> element instanceof LoadInstruction load &&
            type.matchOrSet(load.typeKind()) &&
            slot.matchOrSet(load.slot());
    }

    static Matcher0 loadInstruction(TypeKind type, Capture<Integer> slot) {
        return element -> element instanceof LoadInstruction load &&
            load.typeKind() == type &&
            slot.matchOrSet(load.slot());
    }

    static Matcher0 loadInstruction(Capture<TypeKind> type, int slot) {
        return element -> element instanceof LoadInstruction load &&
            type.matchOrSet(load.typeKind()) &&
            load.slot() == slot;
    }


    static Matcher0<CodeElement> loadInstruction(Capture<TypeKind> type, Matcher0<Integer> slot) {
        return element -> element instanceof LoadInstruction load &&
            type.matchOrSet(load.typeKind()) &&
            slot.matches(load.slot());
    }

    static Matcher0 loadInstruction(TypeKind type, int slot) {
        return element -> element instanceof LoadInstruction load &&
            load.typeKind() == type &&
            load.slot() == slot;
    }

    // Base type matcher
    static Matcher0 loadInstruction() {
        return element -> element instanceof LoadInstruction;
    }


    // Composability
/*    default Matcher or(Matcher other) {...}
    default Matcher not() {...}*/
}