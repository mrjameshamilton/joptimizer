package eu.jameshamilton.optimizer.deadcode;

import static eu.jameshamilton.classfile.matcher.InstructionMatchers.nop;

import eu.jameshamilton.classfile.matcher.Window;
import eu.jameshamilton.optimizer.Optimization;
import java.lang.classfile.CodeBuilder;

public class NopRemover implements Optimization {

    @Override
    public boolean apply(CodeBuilder builder, Window window) {
        return window.matches(nop());
    }

    @Override
    public String getName() {
        return "nop remover";
    }
}
