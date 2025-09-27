package eu.jameshamilton.optimizer.normalize;

import static eu.jameshamilton.classfile.matcher.Any.any;
import static eu.jameshamilton.classfile.matcher.InstructionMatchers.iadd;
import static eu.jameshamilton.classfile.matcher.InstructionMatchers.imul;
import static eu.jameshamilton.classfile.matcher.InstructionMatchers.loadConstant;
import static eu.jameshamilton.classfile.matcher.InstructionMatchers.loadInstruction;

import eu.jameshamilton.classfile.matcher.Window;
import eu.jameshamilton.optimizer.Optimization;
import java.lang.classfile.CodeBuilder;

public class SwapConstant implements Optimization {
    @Override
    public boolean apply(CodeBuilder builder, Window window) {
        // normalize, so we can match other constant optimizations
        if (window.matches(loadConstant(any()), loadInstruction(any(), any()), iadd().or(imul()))) {
            builder.with(window.get(1)).with(window.get(0)).with(window.get(2));
            return true;
        }

        return false;
    }

    @Override
    public String getName() {
        return "normalize constant + imul/iadd";
    }
}
