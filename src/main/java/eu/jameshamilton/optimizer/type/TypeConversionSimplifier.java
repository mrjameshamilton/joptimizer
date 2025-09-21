package eu.jameshamilton.optimizer.type;

import eu.jameshamilton.classfile.matcher.Window;
import eu.jameshamilton.optimizer.Optimization;

import java.lang.classfile.CodeBuilder;

import static eu.jameshamilton.classfile.matcher.InstructionMatchers.d2i;
import static eu.jameshamilton.classfile.matcher.InstructionMatchers.i2b;
import static eu.jameshamilton.classfile.matcher.InstructionMatchers.i2c;
import static eu.jameshamilton.classfile.matcher.InstructionMatchers.i2d;
import static eu.jameshamilton.classfile.matcher.InstructionMatchers.i2l;
import static eu.jameshamilton.classfile.matcher.InstructionMatchers.i2s;
import static eu.jameshamilton.classfile.matcher.InstructionMatchers.l2i;

public class TypeConversionSimplifier implements Optimization {
    @Override
    public boolean apply(CodeBuilder builder, Window window) {
        if (window.matches(
            i2b().or(i2c()).or(i2s()),
            i2b()
        )) {
            builder.i2b();
            return true;
        }

        if (window.matches(
            i2c().or(i2s()),
            i2c()
        )) {
            builder.i2c();
            return true;
        }

        if (window.matches(
            i2s().or(i2c()),
            i2s()
        )) {
            builder.i2s();
            return true;
        }

        if (window.matches(i2l(), l2i())) {
            return true;
        }

        if (window.matches(i2d(), d2i())) {
            return true;
        }

        return false;
    }

    @Override
    public String getName() {
        return "type conversion simplifier";
    }
}
