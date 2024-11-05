package eu.jameshamilton.optimizer.deadcode;

import eu.jameshamilton.classfile.matcher.Capture;
import eu.jameshamilton.classfile.matcher.CollectionMatcher;
import eu.jameshamilton.classfile.matcher.Matcher;
import eu.jameshamilton.classfile.matcher.Window;
import eu.jameshamilton.optimizer.Optimization;

import java.lang.classfile.CodeBuilder;
import java.lang.classfile.Opcode;
import java.util.Set;

import static eu.jameshamilton.classfile.matcher.InstructionMatchers.instruction;
import static java.lang.classfile.Opcode.DNEG;
import static java.lang.classfile.Opcode.FNEG;
import static java.lang.classfile.Opcode.INEG;
import static java.lang.classfile.Opcode.LNEG;

@SuppressWarnings("preview")
public class DoubleNegation implements Optimization {
    private static final Matcher<Opcode> NEGATION_OPCODES = new CollectionMatcher<>(Set.of(INEG, LNEG, FNEG, DNEG));

    @Override
    public boolean apply(CodeBuilder builder, Window window) {
        var op = new Capture<Opcode>().and(NEGATION_OPCODES);
        return window.matches(instruction(op), instruction(op));
    }

    @Override
    public String getName() {
        return "double negation";
    }
}
