package com.mad.dev.hsim.signal.scanner;

import lombok.AllArgsConstructor;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

@AllArgsConstructor
public class AnnotationScanner {

    private Object scanObject;

    public List<Method> filterMethod(Class annotationClass) {
        Method[] methods = this.scanObject.getClass().getMethods();
        List<Method> signalMethod = new ArrayList<>();

        for (Method method : methods) {
            if (method.getAnnotation(annotationClass) != null) {
                signalMethod.add(method);
            }
        }
        return signalMethod;
    }

    public boolean hasAnnotationMethod(Class annotationClass) {
        return !this.filterMethod(annotationClass).isEmpty();
    }
}
