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
public class ConstantStringSubstring implements Optimization {
    private static final Matcher<ClassDesc> stringClass = e -> e.equals(ClassDesc.of("java.lang.String"));
    private static final Matcher<String> substringMethodName = e -> e.equals("substring");
    private static final Matcher<MethodTypeDesc> substringMethodType1 = e -> e.equals(MethodTypeDesc.ofDescriptor("(I)Ljava/lang/String;"));
    private static final Matcher<MethodTypeDesc> substringMethodType2 = e -> e.equals(MethodTypeDesc.ofDescriptor("(II)Ljava/lang/String;"));

    @Override
    public boolean apply(CodeBuilder builder, Window window) {
        var string = new Capture<String>();
        var begin = new Capture<Integer>();
        var end = new Capture<Integer>();
        if (window.matches(
            constantInstruction(string),
            constantInstruction(begin),
            constantInstruction(end).optional(),
            invokevirtual(stringClass, substringMethodName, substringMethodType1.or(substringMethodType2))
        )) {
            if (end.get() != null) {
                if (begin.get() > end.get() ||
                    end.get() > string.get().length() ||
                    begin.get() < 0) {
                    return false;
                }

                builder.constantInstruction(string.get().substring(begin.get(), end.get()));
            } else {
                if (begin.get() < 0) {
                    return false;
                }

                builder.constantInstruction(string.get().substring(begin.get()));
            }

            return true;
        }

        return false;
    }

    @Override
    public String getName() {
        return "constant string substring";
    }
}
