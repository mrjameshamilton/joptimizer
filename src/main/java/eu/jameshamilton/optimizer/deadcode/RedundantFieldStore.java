package eu.jameshamilton.optimizer.deadcode;

import static eu.jameshamilton.classfile.matcher.InstructionMatchers.aload;
import static eu.jameshamilton.classfile.matcher.InstructionMatchers.getfield;
import static eu.jameshamilton.classfile.matcher.InstructionMatchers.putfield;

import eu.jameshamilton.classfile.matcher.Capture;
import eu.jameshamilton.classfile.matcher.Window;
import eu.jameshamilton.optimizer.Optimization;
import java.lang.classfile.CodeBuilder;
import java.lang.constant.ClassDesc;

public class RedundantFieldStore implements Optimization {
    @Override
    public boolean apply(CodeBuilder builder, Window window) {
        var slot = new Capture<Integer>();
        var owner = new Capture<ClassDesc>();
        var name = new Capture<String>();
        var type = new Capture<ClassDesc>();
        return window.matches(
                aload(slot), aload(slot), getfield(owner, name, type), putfield(owner, name, type));
    }

    @Override
    public String getName() {
        return "redundant field store";
    }
}
