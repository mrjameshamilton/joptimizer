package eu.jameshamilton.optimizer.deadcode;

import eu.jameshamilton.classfile.matcher.Window;
import eu.jameshamilton.optimizer.Optimization;

import java.lang.classfile.CodeBuilder;

import static eu.jameshamilton.classfile.matcher.InstructionMatchers.nop;

@SuppressWarnings("preview")
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
