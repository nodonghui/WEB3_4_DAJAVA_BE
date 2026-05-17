package com.dajava.backend.domain.event.kafka.service;

import com.dajava.backend.domain.event.dto.PointerClickEventRequest;
import com.dajava.backend.domain.event.dto.PointerMoveEventRequest;
import com.dajava.backend.domain.event.dto.PointerScrollEventRequest;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LogProducerService {

    private final KafkaTemplate<String, PointerClickEventRequest> clickKafkaTemplate;
    private final KafkaTemplate<String, PointerMoveEventRequest> moveKafkaTemplate;
    private final KafkaTemplate<String, PointerScrollEventRequest> scrollKafkaTemplate;

    private static final String CLICK_TOPIC = "click-events";
    private static final String MOVE_TOPIC = "move-events";
    private static final String SCROLL_TOPIC = "scroll-events";


    public void sendClickEvent(PointerClickEventRequest event) {
        clickKafkaTemplate.send(CLICK_TOPIC, event.getSessionId(), event);
    }

    public void sendMoveEvent(PointerMoveEventRequest event) {
        moveKafkaTemplate.send(MOVE_TOPIC, event.getSessionId(), event);
    }

    public void sendScrollEvent(PointerScrollEventRequest event) {
        scrollKafkaTemplate.send(SCROLL_TOPIC, event.getSessionId(), event);
    }
}
