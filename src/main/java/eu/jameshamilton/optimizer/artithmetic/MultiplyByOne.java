package eu.jameshamilton.optimizer.artithmetic;

import static eu.jameshamilton.classfile.matcher.InstructionMatchers.dmul;
import static eu.jameshamilton.classfile.matcher.InstructionMatchers.fmul;
import static eu.jameshamilton.classfile.matcher.InstructionMatchers.imul;
import static eu.jameshamilton.classfile.matcher.InstructionMatchers.lmul;
import static eu.jameshamilton.classfile.matcher.InstructionMatchers.loadConstant;

import eu.jameshamilton.classfile.matcher.Matcher;
import eu.jameshamilton.classfile.matcher.Window;
import eu.jameshamilton.optimizer.Optimization;
import java.lang.classfile.CodeBuilder;
import java.lang.constant.ConstantDesc;

public class MultiplyByOne implements Optimization {
    private static final Matcher<ConstantDesc> NUMBER = c -> c instanceof Number;
    private static final Matcher<ConstantDesc> ONE =
            NUMBER.and(c -> c.equals(1) || c.equals(1L) || c.equals(1f) || c.equals(1.0));

    @Override
    public boolean apply(CodeBuilder builder, Window window) {
        return window.matches(loadConstant(ONE), imul().or(lmul()).or(dmul()).or(fmul()));
    }

    @Override
    public String getName() {
        return "multiply by one simplifier";
    }
}
