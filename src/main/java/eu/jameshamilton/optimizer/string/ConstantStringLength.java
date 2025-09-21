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

public class ConstantStringLength implements Optimization {
    private static final Matcher<ClassDesc> stringClass = e -> e.equals(ClassDesc.of("java.lang.String"));
    private static final Matcher<String> stringLengthMethodName = e -> e.equals("length");
    private static final Matcher<MethodTypeDesc> stringLengthMethodType = e -> e.equals(MethodTypeDesc.ofDescriptor("()I"));

    @Override
    public boolean apply(CodeBuilder builder, Window window) {
        var s = new Capture<String>();
        if (window.matches(
            loadConstant(s),
            invokevirtual(stringClass, stringLengthMethodName, stringLengthMethodType)
        )) {
            builder.loadConstant(s.get().length());
            return true;
        }

        return false;
    }

    @Override
    public String getName() {
        return "string.length() constant";
    }
}
