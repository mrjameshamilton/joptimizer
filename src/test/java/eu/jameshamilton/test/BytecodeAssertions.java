package eu.jameshamilton.test;

import java.lang.classfile.*;
import java.lang.classfile.ClassFile.ClassHierarchyResolverOption;
import java.lang.classfile.instruction.ConstantInstruction;
import java.lang.classfile.instruction.InvokeInstruction;
import java.lang.classfile.instruction.LoadInstruction;
import java.lang.classfile.instruction.StoreInstruction;
import java.lang.constant.ClassDesc;
import java.lang.constant.MethodTypeDesc;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

import static java.lang.classfile.ClassFile.ACC_PUBLIC;
import static java.lang.classfile.ClassFile.ACC_STATIC;
import static java.lang.classfile.ClassFile.DeadCodeOption.KEEP_DEAD_CODE;
import static org.junit.jupiter.api.Assertions.*;

@SuppressWarnings("preview")
public class BytecodeAssertions {
    
    public static Given given(ClassHierarchyResolver resolver, Consumer<CodeBuilder> instructions) {
        return new Given(resolver, instructions);
    }
    
    public static class Given {
        private final ClassModel originalClass;

        private Given(ClassHierarchyResolver resolver, Consumer<CodeBuilder> instructions) {

            byte[] classBytes = ClassFile.of().build(
                ClassDesc.of("TestClass"), 
                cb -> cb.withMethod("test", 
                    MethodTypeDesc.of(ClassDesc.ofDescriptor("V")),
                    ACC_PUBLIC | ACC_STATIC,
                    mb -> mb.withCode(instructions))
            );
            
            this.originalClass = ClassFile.of(KEEP_DEAD_CODE, ClassHierarchyResolverOption.of(resolver)).parse(classBytes);
        }

        public When when(Function<ClassModel, ClassModel> transformation) {
            return new When(originalClass, transformation);
        }
    }

    public static class When {
        private final List<CodeElement> optimizedInstructions;

        private When(ClassModel originalClass, Function<ClassModel, ClassModel> transformation) {
            ClassModel optimizedClass = transformation.apply(originalClass);
            this.optimizedInstructions = extractInstructions(optimizedClass);
        }


        public void expect(Consumer<CodeBuilder> expected) {
            byte[] expectedBytes = ClassFile.of().build(
                ClassDesc.of("TestClass"),
                cb -> cb.withMethod("test", 
                    MethodTypeDesc.of(ClassDesc.ofDescriptor("V")),
                    ACC_PUBLIC | ACC_STATIC,
                    mb -> mb.withCode(expected))
            );
            
            List<CodeElement> expectedInstructions = extractInstructions(
                ClassFile.of().parse(expectedBytes));
                
            assertInstructions(expectedInstructions, optimizedInstructions);
        }
    }
    
    private static List<CodeElement> extractInstructions(ClassModel classModel) {
        List<CodeElement> instructions = new ArrayList<>();
        classModel.methods().forEach(method -> {
            if (method.methodName().stringValue().equals("test")) {
                method.findAttribute(Attributes.CODE).ifPresent(code -> {
                    code.forEach(instructions::add);
                });
            }
        });
        return instructions;
    }
    
    private static void assertInstructions(List<CodeElement> expected, List<CodeElement> actual) {
        assertEquals(expected.size(), actual.size(), 
            "Instruction count mismatch\n" +
            "Expected: " + expected + "\n" +
            "Actual: " + actual);
        
        for (int i = 0; i < expected.size(); i++) {
            var exp = expected.get(i);
            var act = actual.get(i);
            assertEquals(exp.toString(), act.toString(),
                "Instruction mismatch at index " + i + "\n" +
                "Expected: " + exp + "\n" +
                "Actual: " + act);
        }
    }
}