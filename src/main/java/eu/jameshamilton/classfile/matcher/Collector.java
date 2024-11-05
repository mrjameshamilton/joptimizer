package eu.jameshamilton.classfile.matcher;

import java.util.Collection;

public class Collector<T> implements Matcher<T> {
    private final Collection<T> collection;

    public Collector(Collection<T> collection) {
        this.collection = collection;
    }

    @Override
    public boolean matches(T element) {
        collection.add(element);
        return true;
    }
}
