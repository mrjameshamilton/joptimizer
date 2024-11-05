package eu.jameshamilton.classfile.matcher;

public class Any<T> implements Matcher<T> {
    private Any() {}

    public static <X> Any<X> any() {
        return new Any<>();
    }

    @Override
    public boolean matches(T newValue) {
        return true;
    }

    @Override
    public String toString() {
        return "Any";
    }
}
