package eu.jameshamilton.optimizer.normalize;

import eu.jameshamilton.classfile.matcher.Capture;
import eu.jameshamilton.classfile.matcher.Window;
import eu.jameshamilton.optimizer.Optimization;

import java.lang.classfile.CodeBuilder;

import static eu.jameshamilton.classfile.matcher.Any.any;
import static eu.jameshamilton.classfile.matcher.InstructionMatchers.constantInstruction;
import static eu.jameshamilton.classfile.matcher.InstructionMatchers.iadd;
import static eu.jameshamilton.classfile.matcher.InstructionMatchers.instruction;
import static eu.jameshamilton.classfile.matcher.InstructionMatchers.isub;

@SuppressWarnings("preview")
public class AddSubConstant implements Optimization {
    @Override
    public boolean apply(CodeBuilder builder, Window window) {
        var a = new Capture<Integer>();
        var b = new Capture<Integer>();
        if (window.matches(
            constantInstruction(a),
            instruction(any()),
            constantInstruction(b),
            iadd(),
            isub()
        )) {
            builder
                .constantInstruction(a.get())
                .constantInstruction(b.get())
                .isub()
                .with(window.get(1))
                .isub();
            return true;
        }

        return false;
    }

    @Override
    public String getName() {
        return "a - (y+b) => (a-b) - y";
    }
}
