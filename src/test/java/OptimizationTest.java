import eu.jameshamilton.optimizer.ClassOptimizer;
import eu.jameshamilton.optimizer.Optimization;
import eu.jameshamilton.optimizer.OptimizationStats;
import eu.jameshamilton.optimizer.artithmetic.IntegerConstantArithmeticFolder;
import eu.jameshamilton.optimizer.artithmetic.MultiplyByOne;
import eu.jameshamilton.optimizer.deadcode.NopRemover;
import eu.jameshamilton.optimizer.normalize.AddSubConstant;
import eu.jameshamilton.optimizer.string.ConstantStringEquals;
import eu.jameshamilton.optimizer.string.ConstantStringLength;
import eu.jameshamilton.optimizer.string.ConstantStringSubstring;
import eu.jameshamilton.optimizer.string.ConstantToStringOptimization;
import eu.jameshamilton.optimizer.string.StringBuilderConstructorAppend;
import org.junit.jupiter.api.Test;

import java.lang.classfile.ClassFile;
import java.lang.classfile.ClassFileBuilder;
import java.lang.classfile.ClassHierarchyResolver;
import java.lang.classfile.ClassModel;
import java.lang.constant.ClassDesc;
import java.lang.constant.MethodTypeDesc;
import java.util.Map;

import static eu.jameshamilton.test.BytecodeAssertions.given;
import static java.lang.classfile.ClassFile.DeadCodeOption.KEEP_DEAD_CODE;
import static java.lang.constant.ClassDesc.of;
import static java.lang.constant.MethodTypeDesc.ofDescriptor;

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
            .loadConstant("string")
            .invokevirtual(of("java.lang.String"), "toString", ofDescriptor("()Ljava/lang/String;"))
            .return_()
        )
        .when(classModel -> optimize(classModel, new ConstantToStringOptimization()))
        .expect(code -> code
            .loadConstant("string")
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
                .new_(stringBuilder)
                .dup()
                .invokespecial(stringBuilder, "<init>", MethodTypeDesc.ofDescriptor("()V"))
                .loadConstant(key)
                .invokevirtual(stringBuilder, "append", MethodTypeDesc.ofDescriptor("(" + value + ")Ljava/lang/StringBuilder;")))
            .when(classModel -> optimize(classModel, new StringBuilderConstructorAppend()))
            .expect(code -> code
                .new_(stringBuilder)
                .dup()
                .loadConstant(key)
                .invokespecial(stringBuilder, "<init>", MethodTypeDesc.ofDescriptor("(" + value + ")V")));
        });
    }

    @Test
    public void multipleByOne() {
        given(resolver, code -> {
            code
                .loadConstant(5)
                .istore(0)
                .iload(0)
                .loadConstant(1)
                .imul();
        })
        .when(classModel -> optimize(classModel, new MultiplyByOne()))
        .expect(code -> code
            .loadConstant(5)
            .istore(0)
            .iload(0));
    }

    @Test
    public void normalizeConstantAdd() {
        given(resolver, code -> {
            code
                .iconst_0()
                .istore(1)
                .loadConstant(1)
                .iload(1)
                .loadConstant(2)
                .iadd()
                .isub();
        })
        .when(code -> optimize(code, new AddSubConstant()))
        .expect(code -> {
            code
                .iconst_0()
                .istore(1)
                .loadConstant(1)
                .loadConstant(2)
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
            .loadConstant(1)
            .iload(1)
            .loadConstant(2)
            .iadd()
            .isub())
        .when(code -> {
            var round1 = optimize(code, new AddSubConstant());
            return optimize(round1, new IntegerConstantArithmeticFolder());
        })
        .expect(code -> code
            .iconst_0()
            .istore(1)
            .loadConstant(-1)
            .iload(1)
            .isub());
    }

    @Test
    public void constantStringLength() {
        given(resolver, code -> code
            .loadConstant("Hello World")
            .invokevirtual(ClassDesc.of("java.lang.String"), "length", MethodTypeDesc.ofDescriptor("()I"))
        )
            .when(code -> optimize(code, new ConstantStringLength()))
            .expect(code -> code
                .loadConstant(11)
            );
    }


    @Test
    public void constantStringSubstring1() {
        given(resolver, code -> code
            .loadConstant("Hello World")
            .loadConstant(6)
            .invokevirtual(ClassDesc.of("java.lang.String"), "substring", MethodTypeDesc.ofDescriptor("(I)Ljava/lang/String;"))
        )
            .when(code -> optimize(code, new ConstantStringSubstring()))
            .expect(code -> code
                .loadConstant("World")
            );
    }

    @Test
    public void constantStringSubstring2() {
        given(resolver, code -> code
            .loadConstant("Hello World")
            .loadConstant(0)
            .loadConstant(5)
            .invokevirtual(ClassDesc.of("java.lang.String"), "substring", MethodTypeDesc.ofDescriptor("(II)Ljava/lang/String;"))
        )
            .when(code -> optimize(code, new ConstantStringSubstring()))
            .expect(code -> code
                .loadConstant("Hello")
            );
    }

    @Test
    public void constantStringToString() {
        given(resolver, code -> code
            .loadConstant("Hello World")
            .invokevirtual(ClassDesc.of("java.lang.String"), "toString", MethodTypeDesc.ofDescriptor("()Ljava/lang/String;"))
        )
            .when(code -> optimize(code, new ConstantToStringOptimization()))
            .expect(code -> code
                .loadConstant("Hello World")
            );
    }

    @Test
    public void constantStringEquals() {
        given(resolver, code -> code
            .loadConstant("Hello World")
            .loadConstant("Hello World")
            .invokevirtual(ClassDesc.of("java.lang.String"), "equals", MethodTypeDesc.ofDescriptor("(Ljava/lang/String;)Z"))
        )
            .when(code -> optimize(code, new ConstantStringEquals()))
            .expect(code -> code
                .loadConstant(1)
            );
    }

    @Test
    public void constantStringNotEquals() {
        given(resolver, code -> code
            .loadConstant("Hello World")
            .loadConstant("Hello Worl")
            .invokevirtual(ClassDesc.of("java.lang.String"), "equals", MethodTypeDesc.ofDescriptor("(Ljava/lang/String;)Z"))
        )
            .when(code -> optimize(code, new ConstantStringEquals()))
            .expect(code -> code
                .loadConstant(0)
            );
    }

    private ClassModel optimize(ClassModel classModel, Optimization...optimizations) {
        byte[] inputBytes = ClassFile.of(KEEP_DEAD_CODE).transformClass(classModel, ClassFileBuilder::with);
        byte[] bytes = new ClassOptimizer(stats, resolver, inputBytes).optimize(optimizations).get();
        return ClassFile.of(KEEP_DEAD_CODE).parse(bytes);
    }
}