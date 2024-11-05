import eu.jameshamilton.optimizer.ClassOptimizer;
import eu.jameshamilton.optimizer.Optimization;
import eu.jameshamilton.optimizer.OptimizationStats;
import eu.jameshamilton.optimizer.artithmetic.IntegerConstantArithmeticFolder;
import eu.jameshamilton.optimizer.artithmetic.MultiplyByOne;
import eu.jameshamilton.optimizer.deadcode.NopRemover;
import eu.jameshamilton.optimizer.normalize.AddSubConstant;
import eu.jameshamilton.optimizer.string.ConstantToStringOptimization;
import eu.jameshamilton.optimizer.string.StringBuilderConstructorAppend;
import org.junit.jupiter.api.Test;

import java.lang.classfile.ClassFile;
import java.lang.classfile.ClassHierarchyResolver;
import java.lang.classfile.ClassModel;
import java.lang.constant.ClassDesc;
import java.lang.constant.MethodTypeDesc;
import java.util.Map;

import static eu.jameshamilton.test.BytecodeAssertions.given;
import static java.lang.classfile.ClassFile.DeadCodeOption.KEEP_DEAD_CODE;
import static java.lang.classfile.ClassFile.DeadCodeOption.PATCH_DEAD_CODE;
import static java.lang.constant.ClassDesc.*;
import static java.lang.constant.MethodTypeDesc.ofDescriptor;

@SuppressWarnings("preview")
class OptimizationTest {
    private final OptimizationStats stats = new OptimizationStats();
    private final ClassHierarchyResolver resolver = ClassHierarchyResolver.defaultResolver();

    @Test
    public void testNopRemoval() {
        given(resolver, code -> code
            .nop()
            .nop()
            .return_()
        )
        .when(classModel -> optimize(classModel, new NopRemover()))
        .expect(code -> code
            .return_()
        );
    }

    @Test
    public void testStringToString() {
        given(resolver, code -> code
            .constantInstruction("string")
            .invokevirtual(of("java.lang.String"), "toString", ofDescriptor("()Ljava/lang/String;"))
            .return_()
        )
        .when(classModel -> optimize(classModel, new ConstantToStringOptimization()))
        .expect(code -> code
            .constantInstruction("string")
            .return_()
        );
    }

    @Test
    public void testStringBuilderConstructorAppend() {
        var constants = Map.of(
            "Hello World", "Ljava/lang/String;",
            42, "I",
            42L, "J",
            42.0, "D",
            42.0f, "F"
        );

        ClassDesc stringBuilder = of("java.lang.StringBuilder");
        constants.forEach((key, value) -> {
            given(resolver, code -> code
                .newObjectInstruction(stringBuilder)
                .dup()
                .invokespecial(stringBuilder, "<init>", MethodTypeDesc.ofDescriptor("()V"))
                .constantInstruction(key)
                .invokevirtual(stringBuilder, "append", MethodTypeDesc.ofDescriptor("(" + value + ")Ljava/lang/StringBuilder;")))
            .when(classModel -> optimize(classModel, new StringBuilderConstructorAppend()))
            .expect(code -> code
                .newObjectInstruction(stringBuilder)
                .dup()
                .constantInstruction(key)
                .invokespecial(stringBuilder, "<init>", MethodTypeDesc.ofDescriptor("(" + value + ")V")));
        });
    }

    @Test
    public void multipleByOne() {
        given(resolver, code -> {
            code
                .constantInstruction(5)
                .istore(0)
                .iload(0)
                .constantInstruction(1)
                .imul();
        })
        .when(classModel -> optimize(classModel, new MultiplyByOne()))
        .expect(code -> code
            .constantInstruction(5)
            .istore(0)
            .iload(0));
    }

    @Test
    public void normalizeConstantAdd() {
        given(resolver, code -> {
            code
                .iconst_0()
                .istore(1)
                .constantInstruction(1)
                .iload(1)
                .constantInstruction(2)
                .iadd()
                .isub();
        })
        .when(code -> optimize(code, new AddSubConstant()))
        .expect(code -> {
            code
                .iconst_0()
                .istore(1)
                .constantInstruction(1)
                .constantInstruction(2)
                .isub()
                .iload(1)
                .isub();
        });
    }

    @Test
    public void normalizeConstantAddCombine() {
        given(resolver, code -> code
            .iconst_0()
            .istore(1)
            .constantInstruction(1)
            .iload(1)
            .constantInstruction(2)
            .iadd()
            .isub())
        .when(code -> {
            var round1 = optimize(code, new AddSubConstant());
            return optimize(round1, new IntegerConstantArithmeticFolder());
        })
        .expect(code -> code
            .iconst_0()
            .istore(1)
            .constantInstruction(-1)
            .iload(1)
            .isub());
    }

    private ClassModel optimize(ClassModel classModel, Optimization...optimizations) {
        byte[] bytes = new ClassOptimizer(stats, resolver, classModel).optimize(optimizations);
        return ClassFile.of(KEEP_DEAD_CODE).parse(bytes);
    }
}