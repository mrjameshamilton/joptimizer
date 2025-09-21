package eu.jameshamilton.optimizer.artithmetic;

import eu.jameshamilton.classfile.matcher.Capture;
import eu.jameshamilton.classfile.matcher.CollectionMatcher;
import eu.jameshamilton.classfile.matcher.Window;
import eu.jameshamilton.optimizer.Optimization;

import java.lang.classfile.CodeBuilder;
import java.lang.classfile.Opcode;

import static eu.jameshamilton.classfile.matcher.InstructionMatchers.constantInstruction;
import static eu.jameshamilton.classfile.matcher.InstructionMatchers.ineg;
import static eu.jameshamilton.classfile.matcher.InstructionMatchers.instruction;
import static java.lang.classfile.Opcode.IADD;
import static java.lang.classfile.Opcode.IAND;
import static java.lang.classfile.Opcode.IDIV;
import static java.lang.classfile.Opcode.IMUL;
import static java.lang.classfile.Opcode.IOR;
import static java.lang.classfile.Opcode.IREM;
import static java.lang.classfile.Opcode.ISHL;
import static java.lang.classfile.Opcode.ISHR;
import static java.lang.classfile.Opcode.ISUB;
import static java.lang.classfile.Opcode.IUSHR;
import static java.lang.classfile.Opcode.IXOR;

public class IntegerConstantArithmeticFolder implements Optimization {
    private static final CollectionMatcher<Opcode> integerArithmetic = new CollectionMatcher<>(
        IADD, ISUB, IMUL, IDIV, IREM, IAND, IOR, IXOR, ISHL, ISHR, IUSHR
    );

    @Override
    public boolean apply(CodeBuilder builder, Window window) {
        var c1 = new Capture<Integer>();
        var c2 = new Capture<Integer>();
        var op = new Capture<Opcode>();

        if (window.matches(
            constantInstruction(c1),
            constantInstruction(c2),
            instruction(integerArithmetic.and(op))
        )) {
            var i1 = c1.get();
            var i2 = c2.get();
            var value = switch (op.get()) {
                case IADD -> i1 + i2;
                case ISUB -> i1 - i2;
                case IMUL -> i1 * i2;
                case IDIV -> i2 != 0 ? i1 / i2 : null;
                case IREM -> i2 != 0 ? i1 % i2 : null;
                case IAND -> i1 & i2;
                case IOR -> i1 | i2;
                case IXOR -> i1 ^ i2;
                case ISHL -> i1 << (i2 & 0x1F);
                case ISHR -> i1 >> (i2 & 0x1F);
                case IUSHR -> i1 >>> (i2 & 0x1F);
                default -> null;
            };

            if (value != null) {
                builder.loadConstant(value);
                return true;
            }
        } else if (window.matches(
            constantInstruction(c1.clear()),
            ineg())
        ) {
            builder.loadConstant(-c1.get());
            return true;
        }

        return false;
    }

    @Override
    public String getName() {
        return "integer constant arithmetic folder";
    }
}
