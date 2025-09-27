package eu.jameshamilton.optimizer.deadcode;

import static eu.jameshamilton.classfile.matcher.InstructionMatchers.goto_;
import static eu.jameshamilton.classfile.matcher.InstructionMatchers.label;

import eu.jameshamilton.classfile.matcher.Capture;
import eu.jameshamilton.classfile.matcher.Window;
import eu.jameshamilton.optimizer.Optimization;
import java.lang.classfile.CodeBuilder;
import java.lang.classfile.Label;

public class GotoNextRemover implements Optimization {
    @Override
    public boolean apply(CodeBuilder builder, Window window) {
        // Remove goto to next instruction.
        var label = new Capture<Label>();
        if (window.matches(goto_(label), label(label))) {
            builder.labelBinding(label.get());
            return true;
        }

        return false;
    }

    @Override
    public String getName() {
        return "goto next remover";
    }
}
