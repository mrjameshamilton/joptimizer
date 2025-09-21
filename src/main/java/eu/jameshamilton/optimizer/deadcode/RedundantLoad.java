package eu.jameshamilton.optimizer.deadcode;

import eu.jameshamilton.classfile.matcher.Capture;
import eu.jameshamilton.classfile.matcher.Window;
import eu.jameshamilton.optimizer.Optimization;

import java.lang.classfile.CodeBuilder;
import java.lang.classfile.TypeKind;

import static eu.jameshamilton.classfile.matcher.InstructionMatchers.loadInstruction;

public class RedundantLoad implements Optimization {
    @Override
    public boolean apply(CodeBuilder builder, Window window) {
        // load, load -> load, dup
        var typeKind = new Capture<TypeKind>();
        var capture = new Capture<Integer>();
        boolean matches = window.matches(
            loadInstruction(typeKind, capture),
            loadInstruction(typeKind, capture)
        );
        if (matches) {
            builder.with(window.get(0));
            if (typeKind.get().slotSize() == 1) {
                builder.dup();
            } else {
                builder.dup2();
            }
        }
        return matches;
    }

    @Override
    public String getName() {
        return "redundant load";
    }
}
