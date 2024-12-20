package eu.jameshamilton.optimizer.artithmetic;

import eu.jameshamilton.classfile.matcher.Capture;
import eu.jameshamilton.classfile.matcher.Window;
import eu.jameshamilton.optimizer.Optimization;

import java.lang.classfile.CodeBuilder;

import static eu.jameshamilton.classfile.matcher.InstructionMatchers.constantInstruction;
import static eu.jameshamilton.classfile.matcher.InstructionMatchers.i2b;
import static eu.jameshamilton.classfile.matcher.InstructionMatchers.i2c;
import static eu.jameshamilton.classfile.matcher.InstructionMatchers.i2d;
import static eu.jameshamilton.classfile.matcher.InstructionMatchers.i2f;
import static eu.jameshamilton.classfile.matcher.InstructionMatchers.i2l;
import static eu.jameshamilton.classfile.matcher.InstructionMatchers.i2s;

@SuppressWarnings("preview")
public class ConstantConversionFolder implements Optimization {
    @Override
    public boolean apply(CodeBuilder builder, Window window) {
        var constant = new Capture<Integer>();

        if (window.matches(constantInstruction(constant.clear()), i2l())) {
            builder.constantInstruction(constant.get().longValue());
        } else if (window.matches(constantInstruction(constant.clear()), i2f())) {
            builder.constantInstruction(constant.get().floatValue());
        } else if (window.matches(constantInstruction(constant.clear()), i2d())) {
            builder.constantInstruction(constant.get().doubleValue());
        } else if (window.matches(constantInstruction(constant.clear()), i2b())) {
            builder.constantInstruction((int) constant.get().byteValue());
        } else if (window.matches(constantInstruction(constant.clear()), i2s().or(i2c()))) {
            builder.constantInstruction((int) constant.get().shortValue());
        }

        return window.getMatchedCount() > 0;
    }

    @Override
    public String getName() {
        return "integer constant conversion folder";
    }
}
