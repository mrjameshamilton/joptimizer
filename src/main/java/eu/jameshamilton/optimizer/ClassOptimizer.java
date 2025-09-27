package eu.jameshamilton.optimizer;

import static java.lang.classfile.ClassFile.ClassHierarchyResolverOption;
import static java.lang.classfile.ClassFile.DeadCodeOption.KEEP_DEAD_CODE;
import static java.lang.classfile.ClassFile.DeadCodeOption.PATCH_DEAD_CODE;
import static java.lang.classfile.ClassTransform.transformingMethods;

import eu.jameshamilton.classfile.matcher.Window;
import java.lang.classfile.Attributes;
import java.lang.classfile.ClassFile;
import java.lang.classfile.ClassFileBuilder;
import java.lang.classfile.ClassHierarchyResolver;
import java.lang.classfile.ClassModel;
import java.lang.classfile.CodeBuilder;
import java.lang.classfile.CodeElement;
import java.lang.classfile.Instruction;
import java.lang.classfile.Label;
import java.lang.classfile.TypeAnnotation;
import java.lang.classfile.attribute.CodeAttribute;
import java.lang.classfile.instruction.ExceptionCatch;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * ClassOptimizer is responsible for optimizing the bytecode of a class model using a series of
 * optimization rules. It supports a variety of configurable optimizations and tracks statistics
 * related to the optimization process.
 *
 * <p>The main purpose of this class is to process and transform the bytecode of a given class while
 * preserving its functional behavior, reducing the size of the class, or making it more
 * computationally efficient. This is achieved by applying multiple optimization strategies,
 * selectively controlling the methods and their execution patterns.
 */
public class ClassOptimizer {
    private static final Logger logger = LogManager.getLogger(ClassOptimizer.class);
    private static final int MAX_WINDOW_SIZE = 10;
    private final OptimizationStats stats;
    private final ClassHierarchyResolver resolver;
    private final ClassModel original;

    /**
     * Constructs a new ClassOptimizer, initializing it with the given optimization statistics,
     * class hierarchy resolver, and bytecode data. It parses the provided byte array into a class
     * model to be optimized.
     *
     * @param stats the optimization statistics object used to record metrics and track optimization
     *     progress
     * @param resolver the resolver used for analyzing and working with the class hierarchy
     * @param bytes the byte array representing the class data to be optimized
     */
    public ClassOptimizer(OptimizationStats stats, ClassHierarchyResolver resolver, byte[] bytes) {
        this.stats = stats;
        this.resolver = resolver;
        this.original =
                ClassFile.of(KEEP_DEAD_CODE, ClassHierarchyResolverOption.of(resolver))
                        .parse(bytes);
    }

    /**
     * Optimizes the bytecode using the provided list of {@link Optimization} instances.
     *
     * @param optimizations a variable number of {@link Optimization} instances to apply during the
     *     optimization process
     * @return a byte array representing the optimized class bytecode
     */
    public Optional<byte[]> optimize(Optimization... optimizations) {
        Optional<byte[]> optimizedBytes = generateOptimizedBytes(Arrays.asList(optimizations));

        if (optimizedBytes.isEmpty()) {
            return Optional.empty();
        }

        byte[] originalBytes =
                ClassFile.of(PATCH_DEAD_CODE, ClassHierarchyResolverOption.of(resolver))
                        .transformClass(original, ClassFileBuilder::with);

        if (optimizedBytes.get().length < originalBytes.length) {
            stats.recordClassOptimized(originalBytes.length - optimizedBytes.get().length);
        }

        return optimizedBytes;
    }

    /**
     * Finds the ending index in a list of code elements based on a starting index and the number of
     * executable elements to skip.
     *
     * @param elements the list of {@code CodeElement} objects to search through
     * @param startIndex the starting index in the list
     * @param executableToSkip the number of executable elements to skip
     * @return the index of the element after skipping the specified number of executable elements,
     *     or the size of the list if the end is reached before skipping the required number
     */
    private static int findEndIndex(
            List<CodeElement> elements, int startIndex, int executableToSkip) {
        int index = startIndex;
        int seen = 0;
        while (seen < executableToSkip && index < elements.size()) {
            CodeElement e = elements.get(index);
            if (isExecutableElement(e)) {
                seen++;
            }
            index++;
        }
        return index;
    }

    /**
     * Determines if the provided {@code CodeElement} is an executable element. An element is
     * considered executable if it is an instance of {@code Instruction}, {@code Label}, or {@code
     * ExceptionCatch}.
     *
     * @param element the {@code CodeElement} to check
     * @return {@code true} if the element is executable, {@code false} otherwise
     */
    private static boolean isExecutableElement(CodeElement element) {
        return element instanceof Instruction
                || element instanceof Label
                || element instanceof ExceptionCatch;
    }

    private Optional<byte[]> generateOptimizedBytes(List<Optimization> optimizations) {
        try {
            return Optional.of(
                    ClassFile.of(KEEP_DEAD_CODE, ClassHierarchyResolverOption.of(resolver))
                            .transformClass(
                                    original,
                                    transformingMethods(
                                            (methodBuilder, method) -> {
                                                if (method instanceof CodeAttribute codeModel) {
                                                    methodBuilder.withCode(
                                                            codeBuilder -> {
                                                                processCodeOptimization(
                                                                        optimizations,
                                                                        codeModel,
                                                                        codeBuilder);
                                                            });
                                                } else {
                                                    methodBuilder.with(method);
                                                }
                                            })));
        } catch (IllegalArgumentException e) {
            if (e.getMessage().startsWith("Could not resolve class")) {
                logger.trace(e.getMessage(), e);
            } else {
                throw e;
            }

            return Optional.empty();
        }
    }

    private void processCodeOptimization(
            List<Optimization> optimizations, CodeAttribute code, CodeBuilder codeBuilder) {
        if (hasPositionDependentAnnotations(code)) {
            // Conservatively skip methods with position-dependent annotations
            code.elementStream().forEach(codeBuilder::with);
            return;
        }

        var elements = code.elementStream().toList();
        var executableElements =
                elements.stream().filter(ClassOptimizer::isExecutableElement).toList();

        var optimizationsWithStats =
                optimizations.stream().map(opt -> Optimization.withStats(stats, opt)).toList();

        processCodeElements(elements, executableElements, optimizationsWithStats, codeBuilder);
    }

    private void processCodeElements(
            List<CodeElement> elements,
            List<CodeElement> executableElements,
            List<Optimization> optimizations,
            CodeBuilder codeBuilder) {
        int index = 0; // index into elements
        int execIndex = 0; // index into executableElements

        while (index < elements.size()) {
            CodeElement e = elements.get(index);

            if (!isExecutableElement(e)) {
                codeBuilder.with(e);
                index++;
                continue;
            }

            var windowSize = Math.min(MAX_WINDOW_SIZE, executableElements.size() - execIndex);
            var window = new Window(executableElements.subList(execIndex, execIndex + windowSize));
            var optimized = false;

            for (var opt : optimizations) {
                if (opt.apply(codeBuilder, window)) {
                    optimized = true;
                    break;
                }
            }

            if (optimized) {
                // Skip all elements (both executable and non-executable) that were part of the
                // matched
                // pattern
                index = findEndIndex(elements, index, window.getMatchedCount());
                execIndex += window.getMatchedCount();
            } else {
                codeBuilder.with(e);
                execIndex++;
                index++;
            }
        }
    }

    public boolean hasPositionDependentAnnotations(CodeAttribute code) {
        return code.findAttribute(Attributes.runtimeVisibleTypeAnnotations())
                        .map(
                                attr ->
                                        attr.annotations().stream()
                                                .anyMatch(this::isPositionDependent))
                        .orElse(false)
                || code.findAttribute(Attributes.runtimeInvisibleTypeAnnotations())
                        .map(
                                attr ->
                                        attr.annotations().stream()
                                                .anyMatch(this::isPositionDependent))
                        .orElse(false);
    }

    private boolean isPositionDependent(TypeAnnotation annotation) {
        return switch (annotation.targetInfo().targetType()) {
            case TypeAnnotation.TargetType.LOCAL_VARIABLE,
                    TypeAnnotation.TargetType.RESOURCE_VARIABLE,
                    TypeAnnotation.TargetType.CAST,
                    TypeAnnotation.TargetType.INSTANCEOF,
                    TypeAnnotation.TargetType.NEW,
                    TypeAnnotation.TargetType.CONSTRUCTOR_REFERENCE,
                    TypeAnnotation.TargetType.METHOD_REFERENCE ->
                    true;
            default -> false;
        };
    }
}
