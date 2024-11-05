package eu.jameshamilton.classfile;

import java.lang.constant.ClassDesc;
import java.lang.constant.ConstantDesc;
import java.lang.constant.ConstantDescs;
import java.lang.invoke.TypeDescriptor;

public interface ConstantDescUtil {
    static String constantDescAsString(ConstantDesc v) {
        return switch (v) {
            case String string -> string;
            case ClassDesc cd -> cd.packageName() + "." + cd.displayName();
            case Double aDouble -> Double.toString(aDouble);
            case Float aFloat -> Float.toString(aFloat);
            case Integer integer -> Integer.toString(integer);
            case Long aLong -> Long.toString(aLong);
            case null -> "null";
            default -> throw new UnsupportedOperationException("unsupported type: " + v);
        };
    }

    static TypeDescriptor constantToTypeDesc(ConstantDesc v) {
        return switch (v) {
            case String _ -> ClassDesc.of("java.lang.String");
            case ClassDesc cd -> cd;
            case Double _ -> ConstantDescs.CD_double;
            case Float _ -> ConstantDescs.CD_float;
            case Integer _ -> ConstantDescs.CD_int;
            case Long _ -> ConstantDescs.CD_long;
            default -> throw new UnsupportedOperationException("unsupported type: " + v);
        };
    }
}
