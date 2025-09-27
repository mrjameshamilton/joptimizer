package eu.jameshamilton.optimizer.deadcode;

import static eu.jameshamilton.classfile.matcher.InstructionMatchers.storeInstruction;

import eu.jameshamilton.classfile.matcher.Capture;
import eu.jameshamilton.classfile.matcher.Window;
import eu.jameshamilton.optimizer.Optimization;
import java.lang.classfile.CodeBuilder;
import java.lang.classfile.TypeKind;

public class DoubleStore implements Optimization {
    @Override
    public boolean apply(CodeBuilder builder, Window window) {
        Capture<Integer> slot = new Capture<>();
        Capture<TypeKind> kind = new Capture<>();

        if (window.matches(storeInstruction(kind, slot), storeInstruction(kind, slot))) {
            if (kind.get().slotSize() == 2) {
                builder.pop2();
            } else {
                builder.pop();
            }
            builder.with(window.get(0));
            return true;
        }

        return false;
    }

    @Override
    public String getName() {
        return "double store";
    }
}
