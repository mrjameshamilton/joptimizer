package eu.jameshamilton.optimizer.deadcode;

import eu.jameshamilton.classfile.matcher.Capture;
import eu.jameshamilton.classfile.matcher.Window;
import eu.jameshamilton.optimizer.Optimization;

import java.lang.classfile.CodeBuilder;
import java.lang.classfile.TypeKind;

import static eu.jameshamilton.classfile.matcher.InstructionMatchers.loadInstruction;
import static eu.jameshamilton.classfile.matcher.InstructionMatchers.storeInstruction;

public class RedundantStore implements Optimization {
    @Override
    public boolean apply(CodeBuilder builder, Window window) {
        var typeKind = new Capture<TypeKind>();
        var capture = new Capture<Integer>();
        return window.matches(
            loadInstruction(typeKind, capture),
            storeInstruction(typeKind, capture)
        );
    }

    @Override
    public String getName() {
        return "redundant store";
    }
}
