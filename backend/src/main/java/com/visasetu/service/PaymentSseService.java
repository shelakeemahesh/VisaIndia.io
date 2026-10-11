package com.visasetu.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
@Slf4j
public class PaymentSseService {

    private final Map<String, List<SseEmitter>> activeEmitters = new ConcurrentHashMap<>();

    public SseEmitter subscribe(String referenceId) {
        SseEmitter emitter = new SseEmitter(600_000L); // 10 minutes timeout

        activeEmitters.computeIfAbsent(referenceId, k -> new CopyOnWriteArrayList<>()).add(emitter);

        Runnable cleanup = () -> {
            List<SseEmitter> emitters = activeEmitters.get(referenceId);
            if (emitters != null) {
                emitters.remove(emitter);
                if (emitters.isEmpty()) {
                    activeEmitters.remove(referenceId);
                }
            }
        };

        emitter.onCompletion(cleanup);
        emitter.onTimeout(cleanup);
        emitter.onError((ex) -> cleanup.run());

        try {
            emitter.send(SseEmitter.event()
                    .name("CONNECTED")
                    .data(Map.of("message", "Live event stream connected for " + referenceId)));
        } catch (IOException e) {
            cleanup.run();
        }

        return emitter;
    }

    public void emitStatusChange(String referenceId, String status, String message) {
        List<SseEmitter> emitters = activeEmitters.get(referenceId);
        if (emitters != null && !emitters.isEmpty()) {
            List<SseEmitter> deadEmitters = new ArrayList<>();
            for (SseEmitter emitter : emitters) {
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
                    log.warn("Failed sending SSE to subscriber of {}: {}", referenceId, e.getMessage());
                    deadEmitters.add(emitter);
                }
            }
            emitters.removeAll(deadEmitters);
            if (emitters.isEmpty()) {
                activeEmitters.remove(referenceId);
            }
        }
    }
}
