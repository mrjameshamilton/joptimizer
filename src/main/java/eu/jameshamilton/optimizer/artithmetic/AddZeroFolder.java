package eu.jameshamilton.optimizer.artithmetic;

import eu.jameshamilton.classfile.matcher.Matcher;
import eu.jameshamilton.classfile.matcher.Window;
import eu.jameshamilton.optimizer.Optimization;

import java.lang.classfile.CodeBuilder;
import java.lang.constant.ConstantDesc;

import static eu.jameshamilton.classfile.matcher.InstructionMatchers.loadConstant;
import static eu.jameshamilton.classfile.matcher.InstructionMatchers.dsub;
import static eu.jameshamilton.classfile.matcher.InstructionMatchers.fsub;
import static eu.jameshamilton.classfile.matcher.InstructionMatchers.iadd;
import static eu.jameshamilton.classfile.matcher.InstructionMatchers.isub;
import static eu.jameshamilton.classfile.matcher.InstructionMatchers.ladd;
import static eu.jameshamilton.classfile.matcher.InstructionMatchers.lsub;

public class AddZeroFolder implements Optimization {
    private static final Matcher<ConstantDesc> NUMBER = c -> c instanceof Number;
    private static final Matcher<ConstantDesc> ZERO = NUMBER.and(c -> c.equals(0) || c.equals(0L) || c.equals(0f) || c.equals(0.0));

    @Override
    public boolean apply(CodeBuilder builder, Window window) {
        return window.matches(
            loadConstant(ZERO),
            isub().or(lsub()).or(fsub()).or(dsub())
                .or(iadd()).or(ladd()) // cannot do dadd/fadd due to -0.0
        );
    }

    @Override
    public String getName() {
        return "add zero folder";
    }
}
