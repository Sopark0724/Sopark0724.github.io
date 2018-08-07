package com.mad.dev.hsim.signal.annotation;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface SignalExecute {
    String signalName();
}
