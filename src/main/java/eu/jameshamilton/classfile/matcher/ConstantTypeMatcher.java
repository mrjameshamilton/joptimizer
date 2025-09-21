package eu.jameshamilton.classfile.matcher;

import java.lang.constant.ConstantDesc;
import java.util.Set;

public class ConstantTypeMatcher<T extends ConstantDesc> implements Matcher<T> {

    public static final Matcher<String> STRING = new ConstantTypeMatcher<>(String.class);
    public static final Matcher<Integer> INTEGER = new ConstantTypeMatcher<>(Integer.class);

    private final Set<Class<?>> types;

    public ConstantTypeMatcher(Class<?>...types) {
        this.types = Set.of(types);
    }

    @Override
    public boolean matches(T element) {
        return types.contains(element.getClass());
    }
}
