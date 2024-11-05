package eu.jameshamilton.optimizer;

import eu.jameshamilton.classfile.matcher.Capture;
import eu.jameshamilton.classfile.matcher.CollectionMatcher;
import eu.jameshamilton.classfile.matcher.Window;
import eu.jameshamilton.optimizer.artithmetic.AddZeroFolder;
import eu.jameshamilton.optimizer.artithmetic.ConstantConversionFolder;
import eu.jameshamilton.optimizer.artithmetic.IncrementFolder;
import eu.jameshamilton.optimizer.artithmetic.IntegerConstantArithmeticFolder;
import eu.jameshamilton.optimizer.artithmetic.MultiplyByOne;
import eu.jameshamilton.optimizer.deadcode.ConditionalJumpNextRemover;
import eu.jameshamilton.optimizer.deadcode.DoubleNegation;
import eu.jameshamilton.optimizer.deadcode.DoubleStore;
import eu.jameshamilton.optimizer.deadcode.GotoNextRemover;
import eu.jameshamilton.optimizer.deadcode.NopRemover;
import eu.jameshamilton.optimizer.deadcode.PopRemover;
import eu.jameshamilton.optimizer.deadcode.RedundantLoad;
import eu.jameshamilton.optimizer.deadcode.RedundantStore;
import eu.jameshamilton.optimizer.inliner.BooleanFieldInliner;
import eu.jameshamilton.optimizer.normalize.SwapConstant;
import eu.jameshamilton.optimizer.string.ConstantToStringOptimization;
import eu.jameshamilton.optimizer.string.StringBuilderAppendCombiner;
import eu.jameshamilton.optimizer.string.StringBuilderConstructorAppend;
import eu.jameshamilton.optimizer.string.StringBuilderOptimizer;

import java.lang.classfile.CodeBuilder;
import java.lang.classfile.Opcode;
import java.lang.classfile.constantpool.ClassEntry;
import java.lang.constant.ClassDesc;
import java.util.List;

import static eu.jameshamilton.classfile.matcher.InstructionMatchers.aload;
import static eu.jameshamilton.classfile.matcher.InstructionMatchers.checkcast;
import static eu.jameshamilton.classfile.matcher.InstructionMatchers.constantInstruction;
import static eu.jameshamilton.classfile.matcher.InstructionMatchers.d2i;
import static eu.jameshamilton.classfile.matcher.InstructionMatchers.getfield;
import static eu.jameshamilton.classfile.matcher.InstructionMatchers.getstatic;
import static eu.jameshamilton.classfile.matcher.InstructionMatchers.i2b;
import static eu.jameshamilton.classfile.matcher.InstructionMatchers.i2c;
import static eu.jameshamilton.classfile.matcher.InstructionMatchers.i2d;
import static eu.jameshamilton.classfile.matcher.InstructionMatchers.i2l;
import static eu.jameshamilton.classfile.matcher.InstructionMatchers.i2s;
import static eu.jameshamilton.classfile.matcher.InstructionMatchers.instruction;
import static eu.jameshamilton.classfile.matcher.InstructionMatchers.l2i;
import static eu.jameshamilton.classfile.matcher.InstructionMatchers.putfield;
import static eu.jameshamilton.classfile.matcher.InstructionMatchers.putstatic;
import static java.lang.classfile.Opcode.DADD;
import static java.lang.classfile.Opcode.DDIV;
import static java.lang.classfile.Opcode.DMUL;
import static java.lang.classfile.Opcode.DREM;
import static java.lang.classfile.Opcode.DSUB;

@SuppressWarnings("preview")
public interface Optimization {

    List<Optimization> optimizations = List.of(
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
        named("Double arithmetic constant", new Optimization() {
            private static final CollectionMatcher<Opcode> doubleArithmetic = new CollectionMatcher<>(
                DADD, DSUB, DMUL, DDIV, DREM
            );

            @Override
            public boolean apply(CodeBuilder builder, Window window) {
                var c1 = new Capture<Double>();
                var c2 = new Capture<Double>();
                var op = new Capture<Opcode>();

                if (window.matches(
                    constantInstruction(c1),
                    constantInstruction(c2),
                    instruction(doubleArithmetic.and(op))
                )) {
                    var i1 = c1.get();
                    var i2 = c2.get();
                    var value = switch (op.get()) {
                        case DADD -> i1 + i2;
                        case DSUB -> i1 - i2;
                        case DMUL -> i1 * i2;
                        case DDIV -> i2 != 0 ? i1 / i2 : null;
                        case DREM -> i2 != 0 ? i1 % i2 : null;
                        default -> null;
                    };

                    if (value != null) {
                        builder.constantInstruction(value);
                        return true;
                    }
                }

                return false;
            }
        }),
        new DoubleNegation(),
        named("field get/put", (_, window) -> {
            var slot = new Capture<Integer>();
            var owner = new Capture<ClassDesc>();
            var name = new Capture<String>();
            var type = new Capture<ClassDesc>();
            return window.matches(
                aload(slot),
                aload(slot),
                getfield(owner, name, type),
                putfield(owner, name, type)
            );
        }),
        named("static field get/put", (_, window) -> {
            var owner = new Capture<ClassDesc>();
            var name = new Capture<String>();
            var type = new Capture<ClassDesc>();
            return window.matches(
                getstatic(owner, name, type),
                putstatic(owner, name, type)
            );
        }),
        named("byte conversion", (builder, window) -> {
            if (window.matches(
                i2b().or(i2c()).or(i2s()),
                i2b()
            )) {
                builder.i2b();
                return true;
            }
            return false;
        }),
        named("char conversion", (builder, window) -> {
            if (window.matches(
                i2c().or(i2s()),
                i2c()
            )) {
                builder.i2c();
                return true;
            }
            return false;
        }),
        named("short conversion", (builder, window) -> {
            if (window.matches(
                i2s().or(i2c()),
                i2s()
            )) {
                builder.i2s();
                return true;
            }
            return false;
        }),
        named("i2l l2i", (_, window) -> window.matches(i2l(), l2i())),
        named("i2d d2i", (_, window) -> window.matches(i2d(), d2i())),
        named("checkcast checkcast", (_, window) -> {
            var type = new Capture<ClassEntry>();
            return window.matches(checkcast(type), checkcast(type));
        }),
        new StringBuilderOptimizer(),
        new StringBuilderAppendCombiner(),
        new ConstantToStringOptimization(),
        new StringBuilderConstructorAppend()
    );

    static Optimization named(String name, Optimization optimization) {
        return new Optimization() {
            @Override
            public boolean apply(CodeBuilder builder, Window window) {
                return optimization.apply(builder, window);
            }

            @Override
            public String getName() {
                return name;
            }
        };
    }

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

    default String getName() {
        return getClass().getSimpleName();
    }

}
