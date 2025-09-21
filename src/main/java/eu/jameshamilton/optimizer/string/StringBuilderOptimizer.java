package eu.jameshamilton.optimizer.string;

import eu.jameshamilton.classfile.ConstantDescUtil;
import eu.jameshamilton.classfile.matcher.Capture;
import eu.jameshamilton.classfile.matcher.Collector;
import eu.jameshamilton.classfile.matcher.Matcher;
import eu.jameshamilton.classfile.matcher.Window;
import eu.jameshamilton.optimizer.Optimization;

import java.lang.classfile.CodeBuilder;
import java.lang.constant.ClassDesc;
import java.lang.constant.ConstantDesc;
import java.lang.constant.MethodTypeDesc;
import java.util.ArrayList;
import java.util.stream.Collectors;

import static eu.jameshamilton.classfile.matcher.Any.any;
import static eu.jameshamilton.classfile.matcher.InstructionMatchers.loadConstant;
import static eu.jameshamilton.classfile.matcher.InstructionMatchers.dup;
import static eu.jameshamilton.classfile.matcher.InstructionMatchers.invokespecial;
import static eu.jameshamilton.classfile.matcher.InstructionMatchers.invokevirtual;
import static eu.jameshamilton.classfile.matcher.InstructionMatchers.newObjectInstruction;

public class StringBuilderOptimizer implements Optimization {
    private static final Matcher<ClassDesc> stringBufferClass = e -> e.equals(ClassDesc.of("java.lang.StringBuffer"));
    private static final Matcher<ClassDesc> stringBuilderClass = e -> e.equals(ClassDesc.of("java.lang.StringBuilder"));
    private static final Matcher<String> constructor = e -> e.equals("<init>");
    private static final Matcher<MethodTypeDesc> defaultConstructor = e -> e.equals(MethodTypeDesc.ofDescriptor("()V"));
    private static final Matcher<MethodTypeDesc> stringConstructor = e -> e.equals(MethodTypeDesc.ofDescriptor("(Ljava/lang/String;)V"));
    private static final Matcher<String> appendName = e -> e.equals("append");
    private static final Matcher<String> toStringName = e -> e.equals("toString");
    private static final Matcher<MethodTypeDesc> toStringDescriptor = e -> e.equals(MethodTypeDesc.ofDescriptor("()Ljava/lang/String;"));

    @Override
    public boolean apply(CodeBuilder codeBuilder, Window window) {
        var constants = new ArrayList<ConstantDesc>();
        var collector = new Collector<>(constants);
        var stringBuilderOrBufferClass = new Capture<ClassDesc>().and(stringBufferClass.or(stringBuilderClass));

        if (window.matches(
            newObjectInstruction(stringBuilderOrBufferClass),
            dup(),
            loadConstant(collector).optional(),
            invokespecial(stringBuilderOrBufferClass, constructor, defaultConstructor.or(stringConstructor))
        )) {

            int startIndex = window.getMatchedCount();
            do {
                if (window.matches(startIndex,
                    loadConstant(collector),
                    invokevirtual(stringBuilderOrBufferClass, appendName, any()))
                ) {
                    startIndex = window.getMatchedCount();
                }
            } while (window.getMatchedCount() > 0);

            if (window.matches(startIndex,
                invokevirtual(stringBuilderOrBufferClass, toStringName, toStringDescriptor)
            )) {
                String s = constants.stream()
                    .map(ConstantDescUtil::constantDescAsString)
                    .collect(Collectors.joining());

                if (s.length() > 65_535) {
                    return false;
                }

                codeBuilder.loadConstant(s);

                return true;
            }
        }

        return false;
    }

    @Override
    public String getName() {
        return "string builder optimizer";
    }
}
