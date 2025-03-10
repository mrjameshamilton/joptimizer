package eu.jameshamilton.optimizer.string;

import eu.jameshamilton.classfile.matcher.Capture;
import eu.jameshamilton.classfile.matcher.Matcher;
import eu.jameshamilton.classfile.matcher.Window;
import eu.jameshamilton.optimizer.Optimization;

import java.lang.classfile.CodeBuilder;
import java.lang.constant.ClassDesc;
import java.lang.constant.MethodTypeDesc;

import static eu.jameshamilton.classfile.matcher.InstructionMatchers.constantInstruction;
import static eu.jameshamilton.classfile.matcher.InstructionMatchers.invokevirtual;

@SuppressWarnings("preview")
public class ConstantToStringOptimization implements Optimization {
    private static final Matcher<ClassDesc> stringClass = e -> e.equals(ClassDesc.of("java.lang.String"));
    private static final Matcher<String> toStringName = e -> e.equals("toString");
    private static final Matcher<MethodTypeDesc> toStringDescriptor = e -> e.equals(MethodTypeDesc.ofDescriptor("()Ljava/lang/String;"));

    @Override
    public boolean apply(CodeBuilder builder, Window window) {
        var string = new Capture<String>();
        if (window.matches(
            constantInstruction(string),
            invokevirtual(stringClass, toStringName, toStringDescriptor)
        )) {
            builder.constantInstruction(string.get());
            return true;
        }
        return false;
    }

    @Override
    public String getName() {
        return "string.toString() -> string";
    }
}
