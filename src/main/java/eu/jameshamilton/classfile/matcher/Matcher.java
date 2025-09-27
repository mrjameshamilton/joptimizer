package eu.jameshamilton.classfile.matcher;

@FunctionalInterface
public interface Matcher<T> {

    boolean matches(T element);

    default Matcher<T> and(Matcher<T> other) {
        return e -> this.matches(e) && other.matches(e);
    }

    default <X extends T> Matcher<X> or(Matcher<X> other) {
        return e -> this.matches(e) || other.matches(e);
    }

    default Matcher<T> not() {
        return e -> !this.matches(e);
    }

    default Matcher<T> optional() {
        return new OptionalMatcher<>(this);
    }
}
