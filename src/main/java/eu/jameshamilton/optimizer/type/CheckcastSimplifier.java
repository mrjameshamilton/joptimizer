package eu.jameshamilton.optimizer.type;

import eu.jameshamilton.classfile.matcher.Capture;
import eu.jameshamilton.classfile.matcher.Window;
import eu.jameshamilton.optimizer.Optimization;

import java.lang.classfile.CodeBuilder;
import java.lang.classfile.constantpool.ClassEntry;

import static eu.jameshamilton.classfile.matcher.InstructionMatchers.checkcast;

public class CheckcastSimplifier implements Optimization {
    @Override
    public boolean apply(CodeBuilder builder, Window window) {
        var type = new Capture<ClassEntry>();
        return window.matches(checkcast(type), checkcast(type));
    }

    @Override
    public String getName() {
        return "checkcast simplifier";
    }
}
