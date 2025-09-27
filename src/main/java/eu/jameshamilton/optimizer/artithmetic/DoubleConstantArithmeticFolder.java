package eu.jameshamilton.optimizer.artithmetic;

import static eu.jameshamilton.classfile.matcher.InstructionMatchers.instruction;
import static eu.jameshamilton.classfile.matcher.InstructionMatchers.loadConstant;
import static java.lang.classfile.Opcode.DADD;
import static java.lang.classfile.Opcode.DDIV;
import static java.lang.classfile.Opcode.DMUL;
import static java.lang.classfile.Opcode.DREM;
import static java.lang.classfile.Opcode.DSUB;

import eu.jameshamilton.classfile.matcher.Capture;
import eu.jameshamilton.classfile.matcher.CollectionMatcher;
import eu.jameshamilton.classfile.matcher.Window;
import eu.jameshamilton.optimizer.Optimization;
import java.lang.classfile.CodeBuilder;
import java.lang.classfile.Opcode;

public class DoubleConstantArithmeticFolder implements Optimization {
    private static final CollectionMatcher<Opcode> doubleArithmetic =
            new CollectionMatcher<>(DADD, DSUB, DMUL, DDIV, DREM);

    @Override
    public boolean apply(CodeBuilder builder, Window window) {
        var c1 = new Capture<Double>();
        var c2 = new Capture<Double>();
        var op = new Capture<Opcode>();

        if (window.matches(
                loadConstant(c1), loadConstant(c2), instruction(doubleArithmetic.and(op)))) {
            var i1 = c1.get();
            var i2 = c2.get();
            var value =
                    switch (op.get()) {
                        case DADD -> i1 + i2;
                        case DSUB -> i1 - i2;
                        case DMUL -> i1 * i2;
                        case DDIV -> i2 != 0 ? i1 / i2 : null;
                        case DREM -> i2 != 0 ? i1 % i2 : null;
                        default -> null;
                    };

            if (value != null) {
                builder.loadConstant(value);
                return true;
            }
        }

        return false;
    }

    @Override
    public String getName() {
        return "double constant arithmetic folder";
    }
}
