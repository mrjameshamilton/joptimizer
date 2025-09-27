package eu.jameshamilton.optimizer.deadcode;

import static eu.jameshamilton.classfile.matcher.InstructionMatchers.iconst;
import static eu.jameshamilton.classfile.matcher.InstructionMatchers.if_icmpeq;
import static eu.jameshamilton.classfile.matcher.InstructionMatchers.if_icmpge;
import static eu.jameshamilton.classfile.matcher.InstructionMatchers.if_icmpgt;
import static eu.jameshamilton.classfile.matcher.InstructionMatchers.if_icmple;
import static eu.jameshamilton.classfile.matcher.InstructionMatchers.if_icmplt;
import static eu.jameshamilton.classfile.matcher.InstructionMatchers.if_icmpne;

import eu.jameshamilton.classfile.matcher.Capture;
import eu.jameshamilton.classfile.matcher.Window;
import eu.jameshamilton.optimizer.Optimization;
import java.lang.classfile.CodeBuilder;
import java.lang.classfile.Label;

/**
 * The ZeroComparisonOptimizer class provides an optimization for bytecode sequences that involve
 * comparisons with the integer constant zero. It detects patterns where a comparison with zero is
 * performed using "if_icmp" instructions and replaces them with more specific "if" instructions.
 *
 * <p>The class identifies specific patterns and substitutes them as follows:
 *
 * <p>- Transforms "iconst_0, if_icmpeq" to "ifeq". - Transforms "iconst_0, if_icmpne" to "ifne". -
 * Transforms "iconst_0, if_icmplt" to "iflt". - Transforms "iconst_0, if_icmpgt" to "ifgt". -
 * Transforms "iconst_0, if_icmple" to "ifle". - Transforms "iconst_0, if_icmpge" to "ifge".
 *
 * <p>This optimization reduces redundancy in bytecode and improves performance by simplifying the
 * instruction flow.
 */
public class ZeroComparisonOptimizer implements Optimization {
    @Override
    public boolean apply(CodeBuilder builder, Window window) {
        var label = new Capture<Label>();

        // iconst_0, if_icmpeq -> ifeq
        if (window.matches(iconst(0), if_icmpeq(label))) {
            builder.ifeq(label.get());
            return true;
        }

        // iconst_0, if_icmpne -> ifne
        if (window.matches(iconst(0), if_icmpne(label))) {
            builder.ifne(label.get());
            return true;
        }

        // iconst_0, if_icmplt -> iflt
        if (window.matches(iconst(0), if_icmplt(label))) {
            builder.iflt(label.get());
            return true;
        }

        // iconst_0, if_icmpgt -> ifgt
        if (window.matches(iconst(0), if_icmpgt(label))) {
            builder.ifgt(label.get());
            return true;
        }

        // iconst_0, if_icmple -> ifle
        if (window.matches(iconst(0), if_icmple(label))) {
            builder.ifle(label.get());
            return true;
        }

        // iconst_0, if_icmpge -> ifge
        if (window.matches(iconst(0), if_icmpge(label))) {
            builder.ifge(label.get());
            return true;
        }

        return false;
    }

    @Override
    public String getName() {
        return "zero comparison optimizer";
    }
}
