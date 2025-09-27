package eu.jameshamilton.classfile.matcher;

public class Capture<T> implements Matcher<T> {
    private T value;

    @Override
    public boolean matches(T element) {
        return matchOrSet(element);
    }

    public boolean matchOrSet(T newValue) {
        if (value == null) {
            value = newValue;
            return true;
        }
        return value.equals(newValue);
    }

    public T get() {
        return value;
    }

    public Capture<T> clear() {
        this.value = null;
        return this;
    }
}
