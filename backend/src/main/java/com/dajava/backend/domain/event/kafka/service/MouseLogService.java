package com.dajava.backend.domain.event.kafka.service;

import com.dajava.backend.domain.event.converter.PointerEventConverter;
import com.dajava.backend.domain.event.dto.PointerClickEventRequest;
import com.dajava.backend.domain.event.dto.PointerMoveEventRequest;
import com.dajava.backend.domain.event.dto.PointerScrollEventRequest;
import com.dajava.backend.domain.event.es.entity.PointerClickEventDocument;
import com.dajava.backend.domain.event.es.entity.PointerMoveEventDocument;
import com.dajava.backend.domain.event.es.entity.PointerScrollEventDocument;
import com.dajava.backend.domain.event.es.repository.PointerClickEventDocumentRepository;
import com.dajava.backend.domain.event.es.repository.PointerMoveEventDocumentRepository;
import com.dajava.backend.domain.event.es.repository.PointerScrollEventDocumentRepository;
import com.dajava.backend.domain.log.converter.EventConverter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;


@Service
@Slf4j
@RequiredArgsConstructor
public class MouseLogService {

    private final PointerClickEventDocumentRepository pointerClickEventDocumentRepository;
    private final PointerMoveEventDocumentRepository pointerMoveEventDocumentRepository;
    private final PointerScrollEventDocumentRepository pointerScrollEventDocumentRepository;

    @KafkaListener(
            topics = "click-events",
            groupId = "mouseLogEvent"
    )
    public void consumeClickEvents(List<PointerClickEventRequest> events) {
        log.info("Consuming {} click events", events.size());

        List<PointerClickEventDocument> documents = events.stream()
                .map(PointerEventConverter::toClickEventDocument)
                .toList();

        pointerClickEventDocumentRepository.saveAll(documents);
        log.info("Saved {} click events to Elasticsearch", documents.size());
    }

    @KafkaListener(
            topics = "move-events",
            groupId = "mouseLogEvent"
    )
    public void consumeMoveEvents(List<PointerMoveEventRequest> events) {
        log.info("Consuming {} move events", events.size());

        List<PointerMoveEventDocument> documents = events.stream()
                .map(PointerEventConverter::toMoveEventDocument)
                .toList();

        pointerMoveEventDocumentRepository.saveAll(documents);
        log.info("Saved {} move events to Elasticsearch", documents.size());
    }

    @KafkaListener(
            topics = "scroll-events",
            groupId = "mouseLogEvent"
    )
    public void consumeScrollEvents(List<PointerScrollEventRequest> events) {
        log.info("Consuming {} scroll events", events.size());

        List<PointerScrollEventDocument> documents = events.stream()
                .map(PointerEventConverter::toScrollEventDocument)
                .toList();

        pointerScrollEventDocumentRepository.saveAll(documents);
        log.info("Saved {} scroll events to Elasticsearch", documents.size());
    }
}
