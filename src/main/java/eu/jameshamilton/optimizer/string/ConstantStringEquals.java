package eu.jameshamilton.optimizer.string;

import eu.jameshamilton.classfile.matcher.Capture;
import eu.jameshamilton.classfile.matcher.Matcher;
import eu.jameshamilton.classfile.matcher.Window;
import eu.jameshamilton.optimizer.Optimization;

import java.lang.classfile.CodeBuilder;
import java.lang.constant.ClassDesc;
import java.lang.constant.MethodTypeDesc;

import static eu.jameshamilton.classfile.matcher.InstructionMatchers.loadConstant;
import static eu.jameshamilton.classfile.matcher.InstructionMatchers.invokevirtual;

public class ConstantStringEquals implements Optimization {
    private static final Matcher<ClassDesc> stringClass =
        e -> e.equals(ClassDesc.of("java.lang.String"));
    private static final Matcher<String> substringMethodName =
        e -> e.equals("equals");
    private static final Matcher<MethodTypeDesc> substringMethodType1 =
        e -> e.equals(MethodTypeDesc.ofDescriptor("(Ljava/lang/String;)Z"));

    @Override
    public boolean apply(CodeBuilder builder, Window window) {
        var string = new Capture<String>();
        var string2 = new Capture<String>();

        if (window.matches(
            loadConstant(string),
            loadConstant(string2),
            invokevirtual(stringClass, substringMethodName, substringMethodType1)
        )) {
            builder.loadConstant(string.get().equals(string2.get()) ? 1 : 0);
            return true;
        }

        return false;
    }

    @Override
    public String getName() {
        return "constant string equals";
    }
}
