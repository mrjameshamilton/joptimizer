package eu.jameshamilton.optimizer.deadcode;

import eu.jameshamilton.classfile.matcher.Capture;
import eu.jameshamilton.classfile.matcher.Window;
import eu.jameshamilton.optimizer.Optimization;

import java.lang.classfile.CodeBuilder;
import java.lang.classfile.Label;

import static eu.jameshamilton.classfile.matcher.InstructionMatchers.ifeq;
import static eu.jameshamilton.classfile.matcher.InstructionMatchers.ifge;
import static eu.jameshamilton.classfile.matcher.InstructionMatchers.ifle;
import static eu.jameshamilton.classfile.matcher.InstructionMatchers.ifne;
import static eu.jameshamilton.classfile.matcher.InstructionMatchers.label;

public class ConditionalJumpNextRemover implements Optimization {
    @Override
    public boolean apply(CodeBuilder builder, Window window) {
        // Remove conditional jump to next instruction.
        var label = new Capture<Label>();
        if (window.matches(
            ifeq(label.clear()).or(ifne(label.clear())).or(ifle(label.clear())).or(ifge(label.clear())),
            label(label)
        )) {
            builder
                .pop()
                .labelBinding(label.get());

            return true;
        }

        return false;
    }

    @Override
    public String getName() {
        return "conditional jump next remover";
    }
}
