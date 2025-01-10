package net.coosanta.llm.utility;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static net.coosanta.llm.LlmController.llamaConfig;

public class WebUtils {

    public static void ping(ScheduledExecutorService scheduler, SseEmitter emitter) {
        if (llamaConfig.getSsePing()) {
            scheduler.scheduleAtFixedRate(() -> {
                try {
                    emitter.send(SseEmitter.event().name("ping"));
                } catch (Exception e) {
                    emitter.completeWithError(e);
                }
            }, llamaConfig.getPingInterval(), llamaConfig.getPingInterval(), TimeUnit.SECONDS);
        }
    }
}
