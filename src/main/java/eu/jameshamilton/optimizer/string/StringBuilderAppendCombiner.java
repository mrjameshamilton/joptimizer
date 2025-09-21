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
import static eu.jameshamilton.classfile.matcher.InstructionMatchers.constantInstruction;
import static eu.jameshamilton.classfile.matcher.InstructionMatchers.invokevirtual;

public class StringBuilderAppendCombiner implements Optimization {
    private static final Matcher<ClassDesc> stringBufferClass = e -> e.equals(ClassDesc.of("java.lang.StringBuffer"));
    private static final Matcher<ClassDesc> stringBuilderClass = e -> e.equals(ClassDesc.of("java.lang.StringBuilder"));
    private static final Matcher<String> appendName = e -> e.equals("append");

    @Override
    public boolean apply(CodeBuilder codeBuilder, Window window) {
        var constants = new ArrayList<ConstantDesc>();
        var collector = new Collector<>(constants);
        var classDescCapture = new Capture<ClassDesc>();
        var stringBuilder = classDescCapture.and(stringBuilderClass.or(stringBufferClass));

        if (window.matches(
            constantInstruction(collector),
            invokevirtual(stringBuilder, appendName, any()),
            constantInstruction(collector),
            invokevirtual(stringBuilder, appendName, any())
        )) {
            String s = constants.stream()
                .map(ConstantDescUtil::constantDescAsString)
                .collect(Collectors.joining());

            if (s.length() > 65_535) {
                return false;
            }

            codeBuilder
                .loadConstant(s)
                .invokevirtual(
                    classDescCapture.get(),
                    "append",
                    MethodTypeDesc.ofDescriptor("(Ljava/lang/String;)" + classDescCapture.get().descriptorString())
                );

            return true;
        }

        return false;
    }

    @Override
    public String getName() {
        return "StringBuilder append combiner";
    }
}
