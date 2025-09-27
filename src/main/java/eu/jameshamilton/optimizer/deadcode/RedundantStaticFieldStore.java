package eu.jameshamilton.optimizer.deadcode;

import static eu.jameshamilton.classfile.matcher.InstructionMatchers.getstatic;
import static eu.jameshamilton.classfile.matcher.InstructionMatchers.putstatic;

import eu.jameshamilton.classfile.matcher.Capture;
import eu.jameshamilton.classfile.matcher.Window;
import eu.jameshamilton.optimizer.Optimization;
import java.lang.classfile.CodeBuilder;
import java.lang.constant.ClassDesc;

public class RedundantStaticFieldStore implements Optimization {
    @Override
    public boolean apply(CodeBuilder builder, Window window) {
        var owner = new Capture<ClassDesc>();
        var name = new Capture<String>();
        var type = new Capture<ClassDesc>();
        return window.matches(getstatic(owner, name, type), putstatic(owner, name, type));
    }

    @Override
    public String getName() {
        return "redundant static field store";
    }
}
