package com.visasetu.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class PaymentSseService {

    private final Map<String, SseEmitter> activeEmitters = new ConcurrentHashMap<>();

    public SseEmitter subscribe(String referenceId) {
        SseEmitter emitter = new SseEmitter(600_000L); // 10 minutes timeout

        activeEmitters.put(referenceId, emitter);

        emitter.onCompletion(() -> activeEmitters.remove(referenceId));
        emitter.onTimeout(() -> activeEmitters.remove(referenceId));
        emitter.onError((ex) -> activeEmitters.remove(referenceId));

        try {
            emitter.send(SseEmitter.event()
                    .name("CONNECTED")
                    .data(Map.of("message", "Live event stream connected for " + referenceId)));
        } catch (IOException e) {
            activeEmitters.remove(referenceId);
        }

        return emitter;
    }

    public void emitStatusChange(String referenceId, String status, String message) {
        SseEmitter emitter = activeEmitters.get(referenceId);
        if (emitter != null) {
            try {
                emitter.send(SseEmitter.event()
                        .name("STATUS_CHANGE")
                        .data(Map.of(
                                "referenceId", referenceId,
                                "status", status,
                                "message", message
                        )));
                log.info("Emitted live status change to SSE subscriber {}: {}", referenceId, status);
            } catch (Exception e) {
                log.warn("Failed sending SSE to {}: {}", referenceId, e.getMessage());
                activeEmitters.remove(referenceId);
            }
        }
    }
}
