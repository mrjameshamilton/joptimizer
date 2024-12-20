package eu.jameshamilton.optimizer;

import eu.jameshamilton.classfile.matcher.Window;
import eu.jameshamilton.optimizer.artithmetic.AddZeroFolder;
import eu.jameshamilton.optimizer.artithmetic.ConstantConversionFolder;
import eu.jameshamilton.optimizer.artithmetic.DoubleConstantArithmeticFolder;
import eu.jameshamilton.optimizer.artithmetic.IncrementFolder;
import eu.jameshamilton.optimizer.artithmetic.IntegerConstantArithmeticFolder;
import eu.jameshamilton.optimizer.artithmetic.MultiplyByOne;
import eu.jameshamilton.optimizer.deadcode.ConditionalJumpNextRemover;
import eu.jameshamilton.optimizer.deadcode.DoubleNegation;
import eu.jameshamilton.optimizer.deadcode.DoubleStore;
import eu.jameshamilton.optimizer.deadcode.GotoNextRemover;
import eu.jameshamilton.optimizer.deadcode.NopRemover;
import eu.jameshamilton.optimizer.deadcode.PopRemover;
import eu.jameshamilton.optimizer.deadcode.RedundantFieldStore;
import eu.jameshamilton.optimizer.deadcode.RedundantLoad;
import eu.jameshamilton.optimizer.deadcode.RedundantStaticFieldStore;
import eu.jameshamilton.optimizer.deadcode.RedundantStore;
import eu.jameshamilton.optimizer.inliner.BooleanFieldInliner;
import eu.jameshamilton.optimizer.normalize.AddSubConstant;
import eu.jameshamilton.optimizer.normalize.SwapConstant;
import eu.jameshamilton.optimizer.string.ConstantStringEquals;
import eu.jameshamilton.optimizer.string.ConstantStringLength;
import eu.jameshamilton.optimizer.string.ConstantStringSubstring;
import eu.jameshamilton.optimizer.string.ConstantToStringOptimization;
import eu.jameshamilton.optimizer.string.StringBuilderAppendCombiner;
import eu.jameshamilton.optimizer.string.StringBuilderConstructorAppend;
import eu.jameshamilton.optimizer.string.StringBuilderOptimizer;
import eu.jameshamilton.optimizer.type.CheckcastSimplifier;
import eu.jameshamilton.optimizer.type.TypeConversionSimplifier;

import java.lang.classfile.CodeBuilder;
import java.util.List;

@SuppressWarnings("preview")
public interface Optimization {

    List<Optimization> optimizations = List.of(
        new AddSubConstant(),
        new SwapConstant(),
        new NopRemover(),
        new ConstantConversionFolder(),
        new IntegerConstantArithmeticFolder(),
        new PopRemover(),
        new RedundantStore(),
        new RedundantLoad(),
        new IncrementFolder(),
        new DoubleStore(),
        new BooleanFieldInliner(),
        new GotoNextRemover(),
        new ConditionalJumpNextRemover(),
        new AddZeroFolder(),
        new MultiplyByOne(),
        new DoubleConstantArithmeticFolder(),
        new DoubleNegation(),
        new RedundantFieldStore(),
        new RedundantStaticFieldStore(),
        new TypeConversionSimplifier(),
        new CheckcastSimplifier(),
        new StringBuilderOptimizer(),
        new StringBuilderAppendCombiner(),
        new ConstantToStringOptimization(),
        new StringBuilderConstructorAppend(),
        new ConstantStringLength(),
        new ConstantStringSubstring(),
        new ConstantStringEquals()
    );

    static Optimization withStats(OptimizationStats stats, Optimization optimization) {
        return new Optimization() {
            @Override
            public boolean apply(CodeBuilder builder, Window window) {
                String name = optimization.getName();
                stats.recordAttempt(name);

                boolean success = optimization.apply(builder, window);
                if (success) {
                    stats.recordTentativeSuccess(name);
                }

                return success;
            }

            @Override
            public String getName() {
                return optimization.getName();
            }
        };
    }

    boolean apply(CodeBuilder builder, Window window);

    String getName();
}
