package eu.jameshamilton.optimizer.deadcode;

import eu.jameshamilton.classfile.matcher.Window;
import eu.jameshamilton.optimizer.Optimization;

import java.lang.classfile.CodeBuilder;

import static eu.jameshamilton.classfile.matcher.InstructionMatchers.dup;
import static eu.jameshamilton.classfile.matcher.InstructionMatchers.loadInstruction;
import static eu.jameshamilton.classfile.matcher.InstructionMatchers.pop;
import static eu.jameshamilton.classfile.matcher.InstructionMatchers.pop2;

public class PopRemover implements Optimization {
    @Override
    public boolean apply(CodeBuilder builder, Window window) {
        if (window.matches(pop(), pop())) {
            builder.pop2();
            return true;
        }

        if (window.matches(dup(), pop())) {
            return true;
        }

        if (window.matches(loadInstruction(t -> t.slotSize() == 1, pop()))) {
            return true;
        }

        if (window.matches(loadInstruction(t -> t.slotSize() == 2, pop2()))) {
            return true;
        }

        return false;
    }

    @Override
    public String getName() {
        return "pop remover";
    }
}
