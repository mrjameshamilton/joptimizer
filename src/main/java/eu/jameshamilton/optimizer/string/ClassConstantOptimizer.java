package eu.jameshamilton.optimizer.string;

import eu.jameshamilton.classfile.matcher.Capture;
import eu.jameshamilton.classfile.matcher.Window;
import eu.jameshamilton.optimizer.Optimization;

import java.lang.classfile.CodeBuilder;
import java.lang.constant.ClassDesc;
import java.lang.constant.MethodTypeDesc;

import static eu.jameshamilton.classfile.matcher.InstructionMatchers.invokevirtual;
import static eu.jameshamilton.classfile.matcher.InstructionMatchers.loadConstant;

public class ClassConstantOptimizer implements Optimization {
    @Override
    public boolean apply(CodeBuilder builder, Window window) {
        var clazz = new Capture<ClassDesc>();
        
        // SomeClass.class.getName() at compile time
        if (window.matches(
            loadConstant(clazz),
            invokevirtual(ClassDesc.of("java.lang.Class"), "getName", 
                         MethodTypeDesc.ofDescriptor("()Ljava/lang/String;"))
        )) {
            builder.loadConstant(clazz.get().displayName());
            return true;
        }
        
        return false;
    }

    @Override
    public String getName() {
        return "class name constant optimizer";
    }
}