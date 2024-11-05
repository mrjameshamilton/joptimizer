package eu.jameshamilton.classfile.matcher;

public class OptionalMatcher<T> implements Matcher<T> {
    private final Matcher<T> matcher;
    private boolean wasMatched = false;

    public OptionalMatcher(Matcher<T> matcher) {
        this.matcher = matcher;
    }

    @Override
    public boolean matches(T element) {
        wasMatched = matcher.matches(element);
        return true;
    }

    public boolean wasMatched() {
        return wasMatched;
    }

    public void reset() {
        wasMatched = false;
    }
}
