package eu.jameshamilton.optimizer.artithmetic;

import static eu.jameshamilton.classfile.matcher.InstructionMatchers.instruction;
import static eu.jameshamilton.classfile.matcher.InstructionMatchers.loadConstant;
import static java.lang.classfile.Opcode.LADD;
import static java.lang.classfile.Opcode.LAND;
import static java.lang.classfile.Opcode.LDIV;
import static java.lang.classfile.Opcode.LMUL;
import static java.lang.classfile.Opcode.LNEG;
import static java.lang.classfile.Opcode.LOR;
import static java.lang.classfile.Opcode.LREM;
import static java.lang.classfile.Opcode.LSHL;
import static java.lang.classfile.Opcode.LSHR;
import static java.lang.classfile.Opcode.LSUB;
import static java.lang.classfile.Opcode.LUSHR;
import static java.lang.classfile.Opcode.LXOR;

import eu.jameshamilton.classfile.matcher.Capture;
import eu.jameshamilton.classfile.matcher.CollectionMatcher;
import eu.jameshamilton.classfile.matcher.Window;
import eu.jameshamilton.optimizer.Optimization;
import java.lang.classfile.CodeBuilder;
import java.lang.classfile.Opcode;

public class LongConstantArithmeticFolder implements Optimization {

    private static final CollectionMatcher<Opcode> LONG_ARITH =
            new CollectionMatcher<>(
                    LADD, LSUB, LMUL, LDIV, LREM, LAND, LOR, LXOR, LSHL, LSHR, LUSHR);

    @Override
    public boolean apply(CodeBuilder builder, Window window) {
        var c1 = new Capture<Long>();
        var c2 = new Capture<Long>();
        var op = new Capture<Opcode>();

        // ldc2_w a, ldc2_w b, <op>  => fold to constant (when safe)
        if (window.matches(loadConstant(c1), loadConstant(c2), instruction(LONG_ARITH.and(op)))) {
            long a = c1.get();
            long b = c2.get();

            Long value =
                    switch (op.get()) {
                        case LADD -> a + b;
                        case LSUB -> a - b;
                        case LMUL -> a * b;
                        case LDIV -> (b != 0L) ? a / b : null;
                        case LREM -> (b != 0L) ? a % b : null;
                        case LAND -> a & b;
                        case LOR -> a | b;
                        case LXOR -> a ^ b;
                        case LSHL -> a << (int) (b & 0x3F);
                        case LSHR -> a >> (int) (b & 0x3F);
                        case LUSHR -> a >>> (int) (b & 0x3F);
                        default -> null;
                    };

            if (value != null) {
                builder.loadConstant(value);
                return true;
            }
        }

        // ldc2_w a, lneg => ldc2_w -a
        if (window.matches(loadConstant(c1.clear()), instruction(LNEG))) {
            builder.loadConstant(-c1.get());
            return true;
        }

        return false;
    }

    @Override
    public String getName() {
        return "long constant arithmetic folder";
    }
}
