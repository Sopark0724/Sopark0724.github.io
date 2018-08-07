package com.mad.dev.hsim.signal.component;

import com.mad.dev.hsim.signal.annotation.SignalExecute;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class ApplicationConfig {

    private String removeServerUrl = "http://remote.server.com";

    @SignalExecute(signalName = "SIGUSR1")
    public void changeUrl() {
        this.removeServerUrl = "http://remote2.server.com";
    }

}
