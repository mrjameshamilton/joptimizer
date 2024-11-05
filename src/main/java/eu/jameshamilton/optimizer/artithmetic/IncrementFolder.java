package eu.jameshamilton.optimizer.artithmetic;

import eu.jameshamilton.classfile.matcher.Capture;
import eu.jameshamilton.classfile.matcher.Window;
import eu.jameshamilton.optimizer.Optimization;

import java.lang.classfile.CodeBuilder;
import java.lang.classfile.TypeKind;

import static eu.jameshamilton.classfile.matcher.InstructionMatchers.constantInstruction;
import static eu.jameshamilton.classfile.matcher.InstructionMatchers.iadd;
import static eu.jameshamilton.classfile.matcher.InstructionMatchers.loadInstruction;
import static eu.jameshamilton.classfile.matcher.InstructionMatchers.storeInstruction;

@SuppressWarnings("preview")
public class IncrementFolder implements Optimization {
    @Override
    public boolean apply(CodeBuilder builder, Window window) {
        Capture<Integer> constant = new Capture<>();
        Capture<Integer> slot = new Capture<>();
        Capture<TypeKind> kind = new Capture<>();

        if (window.matches(
            loadInstruction(kind, slot),
            // the integer has to be byte-sized, to fit the iinc operand.
            constantInstruction(constant.and(i -> i.byteValue() == i)),
            iadd(),
            storeInstruction(kind, slot))
        ) {
            builder.iinc(slot.get(), constant.get());
            return true;
        }

        return false;
    }

    @Override
    public String getName() {
        return "increment folder";
    }
}
