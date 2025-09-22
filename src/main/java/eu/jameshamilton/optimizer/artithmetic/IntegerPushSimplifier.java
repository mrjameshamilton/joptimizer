package eu.jameshamilton.optimizer.artithmetic;

import eu.jameshamilton.classfile.matcher.Capture;
import eu.jameshamilton.classfile.matcher.Matcher;
import eu.jameshamilton.classfile.matcher.Window;
import eu.jameshamilton.optimizer.Optimization;

import java.lang.classfile.CodeBuilder;
import java.lang.classfile.Opcode;
import java.lang.classfile.instruction.ConstantInstruction;

import static eu.jameshamilton.classfile.matcher.InstructionMatchers.bipush;
import static eu.jameshamilton.classfile.matcher.InstructionMatchers.instruction;
import static eu.jameshamilton.classfile.matcher.InstructionMatchers.loadConstantInteger;
import static eu.jameshamilton.classfile.matcher.InstructionMatchers.sipush;

public class IntegerPushSimplifier implements Optimization {

    @Override
    public boolean apply(CodeBuilder builder, Window window) {
        var constant = new Capture<Integer>();
        
        // bipush/sipush -> iconst_N (for -1 to 5)
        var singleInstructionRange = constant.and(i -> i >= -1 && i <= 5);

        if (window.matches(
            bipush(singleInstructionRange).or(sipush(singleInstructionRange)))
        ) {

            switch (constant.get()) {
                case -1 -> builder.iconst_m1();
                case 0 -> builder.iconst_0();
                case 1 -> builder.iconst_1();
                case 2 -> builder.iconst_2();
                case 3 -> builder.iconst_3();
                case 4 -> builder.iconst_4();
                case 5 -> builder.iconst_5();
            }

            return true;
        }
        
        // sipush -> bipush (for -128 to 127)  
        if (window.matches(sipush(constant.and(i -> i >= -128 && i <= 127)))) {
            builder.bipush(constant.get());
            return true;
        }
        
        return false;
    }
    
    @Override
    public String getName() {
        return "integer push simplifier";
    }
}