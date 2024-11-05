package eu.jameshamilton.classfile.matcher;

import java.util.Collection;
import java.util.Set;

public class CollectionMatcher<T> implements Matcher<T> {
    private final Collection<T> values;

    @SafeVarargs
    public CollectionMatcher(T... values) {
        this(Set.of(values));
    }

    public CollectionMatcher(Collection<T> values) {
        this.values = values;
    }

    @Override
    public boolean matches(T element) {
        return values.contains(element);
    }
}
