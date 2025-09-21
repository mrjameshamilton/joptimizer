package eu.jameshamilton.classfile.matcher;

import java.lang.classfile.CodeElement;
import java.util.Arrays;
import java.util.List;

public class Window {
    private final List<CodeElement> instructions;
    private int matchedCount = 0;

    public Window(List<CodeElement> instructions) {
        this.instructions = instructions;
    }

    public CodeElement get(int n) {
        return instructions.size() > n ? instructions.get(n) : null;
    }

    public int size() {
        return instructions.size();
    }

    @SafeVarargs
    public final boolean matches(Matcher<CodeElement>... matchers) {
        return matches(0, Arrays.asList(matchers));
    }

    @SafeVarargs
    public final boolean matches(int startIndex, Matcher<CodeElement>... matchers) {
        return matches(startIndex, Arrays.asList(matchers));
    }

    public final boolean matches(List<Matcher<CodeElement>> matchers) {
        return matches(0, matchers);
    }

    public boolean matches(int startIndex, List<Matcher<CodeElement>> matchers) {
        if (startIndex >= instructions.size()) {
            reset();
            return false;
        }

        int currentIndex = startIndex;
        int requiredMatches = 0;

        for (Matcher<CodeElement> matcher : matchers) {
            // Reset any optional matchers before use
            if (matcher instanceof OptionalMatcher<?> optionalMatcher) {
                optionalMatcher.reset();
            }

            if (currentIndex >= instructions.size()) {
                // We've run out of instructions
                if (matcher instanceof OptionalMatcher<?>) {
                    continue; // Optional matchers can be skipped at the end
                } else {
                    reset();
                    return false;
                }
            }

            if (matcher.matches(instructions.get(currentIndex))) {
                if (matcher instanceof OptionalMatcher<?> optMatcher) {
                    if (optMatcher.wasMatched()) {
                        currentIndex++;
                        requiredMatches++;
                    }
                } else {
                    currentIndex++;
                    requiredMatches++;
                }
            } else {
                if (matcher instanceof OptionalMatcher<?>) {
                    // Skip this optional matcher
                } else {
                    reset();
                    return false;
                }
            }
        }

        matchedCount = Math.max(matchedCount, startIndex + requiredMatches);

        return true;
    }


    public void reset() {
        matchedCount = 0;
    }

    public int getMatchedCount() {
        return matchedCount;
    }

    public String toString() {
        return instructions.subList(0, Math.min(10, instructions.size())) + "...";
    }
}