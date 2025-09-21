package eu.jameshamilton.optimizer.deadcode;

import eu.jameshamilton.classfile.matcher.Capture;
import eu.jameshamilton.classfile.matcher.Window;
import eu.jameshamilton.optimizer.Optimization;

import java.lang.classfile.CodeBuilder;

import static eu.jameshamilton.classfile.matcher.InstructionMatchers.iinc;

/**
 * Combines two consecutive {@code iinc} instructions into a single instruction.
 */
public class DoubleIncrementCombiner implements Optimization {

    @Override
    public boolean apply(CodeBuilder builder, Window window) {
        var slot = new Capture<Integer>();
        var a = new Capture<Integer>();
        var b = new Capture<Integer>();
        if (window.matches(
            iinc(slot, a),
            iinc(slot, b)
        )) {
            var sum = a.get() + b.get();
            if (sum == (byte) sum) {
                builder.iinc(slot.get(), sum);
                return true;
            }
        }

        return false;
    }

    @Override
    public String getName() {
        return "double increment combiner";
    }
}
