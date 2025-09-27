package eu.jameshamilton.optimizer.string;

import static eu.jameshamilton.classfile.ConstantDescUtil.constantToTypeDesc;
import static eu.jameshamilton.classfile.matcher.Any.any;
import static eu.jameshamilton.classfile.matcher.InstructionMatchers.dup;
import static eu.jameshamilton.classfile.matcher.InstructionMatchers.invokespecial;
import static eu.jameshamilton.classfile.matcher.InstructionMatchers.invokevirtual;
import static eu.jameshamilton.classfile.matcher.InstructionMatchers.loadConstant;
import static eu.jameshamilton.classfile.matcher.InstructionMatchers.newObjectInstruction;

import eu.jameshamilton.classfile.matcher.Capture;
import eu.jameshamilton.classfile.matcher.Matcher;
import eu.jameshamilton.classfile.matcher.Window;
import eu.jameshamilton.optimizer.Optimization;
import java.lang.classfile.CodeBuilder;
import java.lang.constant.ClassDesc;
import java.lang.constant.ConstantDesc;
import java.lang.constant.MethodTypeDesc;

public class StringBuilderConstructorAppend implements Optimization {
    private static final Matcher<ClassDesc> stringBufferClass =
            e -> e.equals(ClassDesc.of("java.lang.StringBuffer"));
    private static final Matcher<ClassDesc> stringBuilderClass =
            e -> e.equals(ClassDesc.of("java.lang.StringBuilder"));
    private static final Matcher<String> constructor = e -> e.equals("<init>");
    private static final Matcher<MethodTypeDesc> defaultConstructor =
            e -> e.equals(MethodTypeDesc.ofDescriptor("()V"));
    private static final Matcher<String> appendName = e -> e.equals("append");

    @Override
    public boolean apply(CodeBuilder builder, Window window) {
        var constants = new Capture<ConstantDesc>();
        Capture<ClassDesc> classDescCapture = new Capture<>();
        var stringBuilderOrBufferClass =
                classDescCapture.and(stringBufferClass.or(stringBuilderClass));

        if (window.matches(
                newObjectInstruction(stringBuilderOrBufferClass),
                dup(),
                invokespecial(stringBuilderOrBufferClass, constructor, defaultConstructor),
                loadConstant(constants),
                invokevirtual(stringBuilderOrBufferClass, appendName, any()))) {
            builder.new_(classDescCapture.get())
                    .dup()
                    .loadConstant(constants.get())
                    .invokespecial(
                            classDescCapture.get(),
                            "<init>",
                            MethodTypeDesc.ofDescriptor(
                                    "("
                                            + constantToTypeDesc(constants.get()).descriptorString()
                                            + ")V"));

            return true;
        }

        return false;
    }

    @Override
    public String getName() {
        return "stringbuilder constructor simplify";
    }
}
