package com.mad.dev.hsim.signal.handler;

import com.mad.dev.hsim.signal.annotation.SignalExecute;
import com.mad.dev.hsim.signal.scanner.AnnotationScanner;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;
import sun.misc.Signal;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Component
@Slf4j
public class SignalHandler implements sun.misc.SignalHandler, ApplicationListener<ContextRefreshedEvent> {

    @Autowired
    private Object[] beans;

    private List signalBeans;

    @Override
    public void handle(Signal signal) {
        log.info("signal : " + signal.getName());
        this.signalBeans.forEach(bean ->{
            List<Method> exeMethod = new AnnotationScanner(bean).filterMethod(SignalExecute.class);
            if(!exeMethod.isEmpty()){
                exeMethod.forEach(method -> {
                    SignalExecute signalExecute = method.getAnnotation(SignalExecute.class);
                    if(!signal.getName().equals(signalExecute.signalName())){ return; }
                    try {
                        method.invoke(bean);
                    } catch (IllegalAccessException | InvocationTargetException e) {
                        log.error("signal method invoke error : {}", method.getName(), signal.getName());
                    }
                });
            }
        });
    }

    @Override
    public void onApplicationEvent(ContextRefreshedEvent contextRefreshedEvent) {
        this.signalBeans = Arrays.stream(this.beans).filter(bean -> new AnnotationScanner(bean).hasAnnotationMethod(SignalExecute.class)).collect(Collectors.toList());
        String[] signals = {"USR2"};
        Arrays.stream(signals).forEach(signal ->{
            try {
                Signal.handle(new Signal(signal), this);
            }catch(IllegalArgumentException e){
                log.error("signal handle err : " + e.getMessage());
            }
        });
    }
}
