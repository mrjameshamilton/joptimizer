package eu.jameshamilton.optimizer.inliner;

import eu.jameshamilton.classfile.matcher.Capture;
import eu.jameshamilton.classfile.matcher.Matcher;
import eu.jameshamilton.classfile.matcher.Window;
import eu.jameshamilton.optimizer.Optimization;

import java.lang.classfile.CodeBuilder;
import java.lang.constant.ClassDesc;
import java.lang.constant.MethodTypeDesc;

import static eu.jameshamilton.classfile.matcher.InstructionMatchers.getstatic;
import static eu.jameshamilton.classfile.matcher.InstructionMatchers.invokevirtual;

public class BooleanFieldInliner implements Optimization {
    private static final Matcher<ClassDesc> javaLangBoolean = e -> e.equals(ClassDesc.of("java.lang.Boolean"));
    private static final Matcher<String> trueOrFalseField = e -> e.equals("TRUE") || e.equals("FALSE");
    private static final Matcher<String> booleanValueMethod = e -> e.equals("booleanValue");
    private static final Matcher<MethodTypeDesc> booleanValueMethodTypeDesc = e -> e.equals(MethodTypeDesc.ofDescriptor("()Z"));

    @Override
    public boolean apply(CodeBuilder codeBuilder, Window window) {
        var fieldName = new Capture<String>();
        if (window.matches(
            getstatic(javaLangBoolean, trueOrFalseField.and(fieldName), javaLangBoolean),
            invokevirtual(javaLangBoolean, booleanValueMethod, booleanValueMethodTypeDesc)
        )) {
            codeBuilder.loadConstant(fieldName.matches("TRUE") ? 1 : 0);
        }

        return window.getMatchedCount() > 0;
    }

    @Override
    public String getName() {
        return "boolean field inliner";
    }
}
