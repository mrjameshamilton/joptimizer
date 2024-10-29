package eu.jameshamilton;

import java.lang.classfile.CodeElement;
import java.lang.classfile.instruction.NopInstruction;
import java.util.List;
import java.util.Optional;

public record Window(List<CodeElement> instructions) {
    public CodeElement get(int n) {
        return instructions.size() > n ? instructions.get(n) : NopInstruction.of();
    }

    @SafeVarargs
    public final boolean matches(int startIndex, Class<? extends CodeElement>... types) {
        if (startIndex + types.length > instructions.size()) {
            return false;
        }

        for (int i = 0; i < types.length; i++) {
            if (!types[i].isInstance(instructions.get(startIndex + i))) {
                return false;
            }
        }
        return true;
    }

    @SafeVarargs
    public final boolean matches(Class<? extends CodeElement>... types) {
        return matches(0, types);
    }

    public boolean matches(int startIndex, Matcher... matchers) {
        if (startIndex + matchers.length > instructions.size()) {
            return false;
        }

        for (int i = 0; i < matchers.length; i++) {
            if (!matchers[i].matches(instructions.get(startIndex + i))) {
                return false;
            }
        }
        return true;
    }

    public boolean matches(Matcher... matchers) {
        return matches(0, matchers);
    }

    public <T extends CodeElement> Optional<T> get(int index, Class<T> type) {
        CodeElement element = get(index);
        return type.isInstance(element) ? Optional.of(type.cast(element)) : Optional.empty();
    }
}
