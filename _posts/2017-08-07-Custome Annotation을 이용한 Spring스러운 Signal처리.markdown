
---
layout:     post
title:      "[SPRING] Custom annotation을 이용한 Signal 처리"
subtitle:   "Linux signal로 Component의 코드 호출하기"
date:       2018-08-07 21:34:00
author:     "hsim"
header-img: ""
---

# [SPRING] Custom annotation을 이용한 Signal 처리

서버 개발을 하다 보면, 아주 예외적인 케이스를 위해서 본인만 호출 할 수 있는 api를 만들곤 한다.
예를들자면
동적인 자원 해제, 호출 되는 서버의 주소 변경, GC의 명시적인 호출 등이 있을 것 같다.

하지만, 이런 API가 외부로 노출 되었을 경우를 생각한다면, 굉장히 큰 취약점을 스스로 노출 시키는 것과 같다.

그렇다면, 이런 API 노출 이외에 '서버 데몬'과 '서버 운영자'간의 대화는 어떤 방식으로 이뤄 질 수있을까?
나는 해답을, C의 Signal Programming 에서 찾아봤다.

java에서도 이런 os의 시그널을 처리 할 수있는 Handler를 제공 해주고 있는데,
나는 불편한 Signal 처리를 Spring Framework위에서 아주 자연스럽고, Spring 스럽게 커스텀해서 쓰는 방법을 포스팅 해볼까한다.

#### 아래 예제는, 특정 Signal이 Process에 전달 되었을때 Config에 저장 된 외부 URL을 다른 URL로 변경하는것을 다룬다.

1. Signal Execute Annotation 추가

```java
import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface SignalExecute {
    String signalName();
}
```

2. Signal이 전달 되었을때 호출되는 Method 위에 어노테이션을 추가 시킨다.

```java
import com.mad.dev.hsim.signal.annotation.SignalExecute;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class ApplicationConfig {

    private String removeServerUrl = "http://remote.server.com";

    @SignalExecute(signalName = "USR2")
    public void changeUrl() {
        this.removeServerUrl = "http://remote2.server.com";
    }

}

```

이제 이 글을 읽는 분들이 어떤걸 만들지 대충 감을 잡으셨을 것 같다.
특정 signal이 전달 되었을때, Spring Component들 안에 있는 SignalExecute라는 Annotation이 붙어있는 Method를 모두 호출 시켜주려고 한다.

3. 간단하게 class의 method에서 annotation을 검색 할 수있는 utility class를 작성한다.

```java

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
```

4. 아래와 같은 코드로 USR2를 핸들링 할 수 있고, SignalExecute annotation 이 달린 모든 method를 호출 시키는 handler를 만든다.

```java

mport com.mad.dev.hsim.signal.annotation.SignalExecute;
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

```

이제 아래와 같이 SIGUSR2를 해당 프로세스 PID로 전달시키면, 아래의 changeUrl 함수가 호출되는 것을 볼 수 있다.

kill -SIGUSR2 29419

```java
    @SignalExecute(signalName = "USR2")
    public void changeUrl() {
        this.removeServerUrl = "http://remote2.server.com";
    }
```


### 이런 식의 Annotation을 이용한 코딩은 생각보다 훨씬 더 유용하다. 중복코드를 제거하고, 필요한 기능들을 빠르게 추가 하는데 많은 도움이 된다.
### 다음 포스팅부터는 Annotation 을 이용한 코드들의 원리와, 더 다양한 활용방법을 소개 하고자 한다.


### 임희섭 ( 다우기술: hsim@daou.co.kr )