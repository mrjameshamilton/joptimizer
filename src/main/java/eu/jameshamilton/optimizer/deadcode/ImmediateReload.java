package eu.jameshamilton.optimizer.deadcode;

import static eu.jameshamilton.classfile.matcher.InstructionMatchers.loadInstruction;
import static eu.jameshamilton.classfile.matcher.InstructionMatchers.storeInstruction;

import eu.jameshamilton.classfile.matcher.Capture;
import eu.jameshamilton.classfile.matcher.Window;
import eu.jameshamilton.optimizer.Optimization;
import java.lang.classfile.CodeBuilder;
import java.lang.classfile.TypeKind;

/**
 * Collapses an immediate reload of the same local right after a store by duplicating the value
 * before the store.
 *
 * Stack before: ..., value
 * Pattern:      store slot; load slot
 * Rewritten:    dup/dup2; store slot
 */
public class ImmediateReload implements Optimization {
    @Override
    public boolean apply(CodeBuilder builder, Window window) {
        var slot = new Capture<Integer>();
        var kind = new Capture<TypeKind>();

        // ..., value, store <k> <slot>, load <k> <slot>
        if (window.matches(storeInstruction(kind, slot), loadInstruction(kind, slot))) {
            // duplicate the value prior to consuming it with the store
            if (kind.get().slotSize() == 2) {
                builder.dup2();
            } else {
                builder.dup();
            }
            builder.with(window.get(0)); // emit the original store
            return true;
        }

        return false;
    }

    @Override
    public String getName() {
        return "immediate reload";
    }
}
