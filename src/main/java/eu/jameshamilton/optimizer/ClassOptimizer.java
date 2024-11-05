package eu.jameshamilton.optimizer;

import eu.jameshamilton.classfile.matcher.Window;

import java.lang.classfile.ClassFile;
import java.lang.classfile.ClassFileBuilder;
import java.lang.classfile.ClassHierarchyResolver;
import java.lang.classfile.ClassModel;
import java.lang.classfile.CodeBuilder;
import java.lang.classfile.CodeElement;
import java.lang.classfile.Instruction;
import java.lang.classfile.Label;
import java.lang.classfile.attribute.CodeAttribute;
import java.lang.classfile.instruction.ExceptionCatch;
import java.util.Arrays;
import java.util.List;

import static java.lang.classfile.ClassFile.ClassHierarchyResolverOption;
import static java.lang.classfile.ClassFile.DeadCodeOption.KEEP_DEAD_CODE;
import static java.lang.classfile.ClassFile.DeadCodeOption.PATCH_DEAD_CODE;
import static java.lang.classfile.ClassFile.of;
import static java.lang.classfile.ClassTransform.transformingMethods;

@SuppressWarnings("preview")
public class ClassOptimizer {
    private static final int MAX_WINDOW_SIZE = 10;

    private final OptimizationStats stats;
    private final ClassHierarchyResolver resolver;
    private final ClassModel original;

    public ClassOptimizer(OptimizationStats stats, ClassHierarchyResolver resolver, byte[] bytes) {
        this(stats, resolver, ClassFile.of(KEEP_DEAD_CODE, ClassHierarchyResolverOption.of(resolver)).parse(bytes));
    }

    public ClassOptimizer(OptimizationStats stats, ClassHierarchyResolver resolver, ClassModel original) {
        this.stats = stats;
        this.resolver = resolver;
        this.original = original;
    }

    private static int findEndIndex(List<CodeElement> elements, int startIndex, int executableToSkip) {
        int index = startIndex;
        int seen = 0;

        while (seen < executableToSkip && index < elements.size()) {
            CodeElement e = elements.get(index);
            boolean isExecutable = e instanceof Instruction ||
                e instanceof Label ||
                e instanceof ExceptionCatch;

            if (isExecutable) {
                seen++;
            }
            index++;
        }

        return index;
    }

    public byte[] optimize(Optimization... optimizations) {
        return optimize(Arrays.asList(optimizations));
    }

    public byte[] optimize(List<Optimization> optimizations) {
        byte[] optimizedBytes = of(KEEP_DEAD_CODE, ClassHierarchyResolverOption.of(resolver))
            .transform(original,
                transformingMethods(
                    (methodBuilder, method) -> {
                        if (method instanceof CodeAttribute codeModel) {
                            methodBuilder.withCode(codeBuilder -> {
                                optimize(optimizations, codeModel, codeBuilder);
                            });
                        } else {
                            methodBuilder.with(method);
                        }
                    }
                ));

        byte[] originalBytes = ClassFile
            .of(PATCH_DEAD_CODE, ClassHierarchyResolverOption.of(resolver))
            .transform(original, ClassFileBuilder::with);

        if (optimizedBytes.length < originalBytes.length) {
            stats.recordClassOptimized(originalBytes.length - optimizedBytes.length);
        }
        return optimizedBytes;
    }

    private void optimize(List<Optimization> optimizations, CodeAttribute code, CodeBuilder codeBuilder) {
        var elements = code.elementStream().toList();
        var executableElements = elements.stream()
            .filter(el -> el instanceof Instruction || el instanceof Label || el instanceof ExceptionCatch)
            .toList();

        var optimizationsWithStats = optimizations.stream()
            .map(opt -> Optimization.withStats(stats, opt))
            .toList();

        int index = 0;  // index into elements
        int execIndex = 0;  // index into executableElements

        while (index < elements.size()) {
            CodeElement e = elements.get(index);

            if (!(e instanceof Instruction || e instanceof Label || e instanceof ExceptionCatch)) {
                codeBuilder.with(e);
                index++;
                continue;
            }

            var windowSize = Math.min(MAX_WINDOW_SIZE, executableElements.size() - execIndex);
            var window = new Window(executableElements.subList(execIndex, execIndex + windowSize));
            var optimized = false;

            for (var opt : optimizationsWithStats) {
                if (opt.apply(codeBuilder, window)) {
                    optimized = true;
                    break;
                }
            }

            if (optimized) {
                // Skip all elements (both executable and non-executable) that were part of the matched pattern
                index = findEndIndex(elements, index, window.getMatchedCount());
                execIndex += window.getMatchedCount();
            } else {
                codeBuilder.with(e);
                execIndex++;
                index++;
            }
        }
    }
}
