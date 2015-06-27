package com.saltar;

import com.google.gson.reflect.TypeToken;
import com.squareup.javapoet.TypeName;

import java.lang.reflect.Type;

import javax.lang.model.element.Element;

public class TypeUtils {
    public static boolean equalTypes(Element element, TypeToken token) {
        return equalTypes(element, token.getType());
    }

    public static boolean equalTypes(Element element, Type type) {
        return TypeName.get(element.asType()).toString().equals(type.getTypeName());
    }

    public static boolean containsType(Element element, Class... classes) {
        for (Class clazz : classes) {
            if (equalTypes(element,clazz)) {
                return true;
            }
        }
        return false;
    }
}