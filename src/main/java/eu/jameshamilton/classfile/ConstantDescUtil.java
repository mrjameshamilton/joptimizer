package eu.jameshamilton.classfile;

import java.lang.constant.ClassDesc;
import java.lang.constant.ConstantDesc;
import java.lang.constant.ConstantDescs;
import java.lang.constant.DynamicConstantDesc;
import java.lang.constant.MethodHandleDesc;
import java.lang.constant.MethodTypeDesc;
import java.lang.invoke.TypeDescriptor;

import static java.lang.constant.ClassDesc.of;

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
            case DynamicConstantDesc<?> _, MethodHandleDesc _, MethodTypeDesc _ ->
                throw new UnsupportedOperationException("Unsupported constant type: " + v);
        };
    }

    static TypeDescriptor constantToTypeDesc(ConstantDesc v) {
        return switch (v) {
            case String _ -> of("java.lang.String");
            case ClassDesc cd -> cd;
            case Double _ -> ConstantDescs.CD_double;
            case Float _ -> ConstantDescs.CD_float;
            case Integer _ -> ConstantDescs.CD_int;
            case Long _ -> ConstantDescs.CD_long;
            case DynamicConstantDesc<?> _, MethodHandleDesc _, MethodTypeDesc _ ->
                throw new UnsupportedOperationException("Unsupported constant type: " + v);
        };
    }
}
