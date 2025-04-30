package com.dajava.backend.domain.heatmap.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import com.dajava.backend.domain.event.es.entity.SolutionEventDocument;
import com.dajava.backend.domain.event.es.repository.SolutionEventDocumentRepository;
import com.dajava.backend.domain.heatmap.exception.HeatmapException;
import com.dajava.backend.global.exception.ErrorCode;

import co.elastic.clients.elasticsearch._types.ElasticsearchException;

@Service
public class SolutionEventFetcher {

    // 페이지 사이즈 고정 값
    private static final int PAGE_SIZE = 1000;

    private final SolutionEventDocumentRepository repository;

    public SolutionEventFetcher(SolutionEventDocumentRepository repository) {
        this.repository = repository;
    }

    /**
     * Pagenation으로 1회 쿼리 호출시 1000개의 이벤트를 가져옵니다.
     * sortByTimestamp 플래그로 정렬 여부를 결정합니다.
     * 데이터가 없다면 중단되고 모든 이벤트를 반환합니다.
     *
     * @param serialNumber ES에 접근해 데이터를 가져오기 위한 값입니다.
     * @param sortByTimestamp 플래그 여부에 따라 정렬을 할지 말지 결정합니다.
     * @return List<SolutionEventDocument>
     */
    public List<SolutionEventDocument> getAllEvents(String serialNumber, boolean sortByTimestamp) {
        List<SolutionEventDocument> allEvents = new ArrayList<>();
        int pageNumber = 0;
        List<SolutionEventDocument> pageData;

        do {
            PageRequest pageRequest;
            if (sortByTimestamp) {
                pageRequest = PageRequest.of(pageNumber, PAGE_SIZE, Sort.by(Sort.Direction.ASC, "timestamp"));
            } else {
                pageRequest = PageRequest.of(pageNumber, PAGE_SIZE);
            }
            try {
                pageData = repository.findBySerialNumber(serialNumber, pageRequest);
            } catch (ElasticsearchException ex) {
                // 예외 로깅 및 처리
                // (생략)
                throw new HeatmapException(ErrorCode.ELASTICSEARCH_QUERY_FAILED);
            }
            allEvents.addAll(pageData);
            pageNumber++;
        } while (!pageData.isEmpty());
        
        if (allEvents.isEmpty()) {
            throw new HeatmapException(ErrorCode.SOLUTION_EVENT_DATA_NOT_FOUND);
        }

        return allEvents;
    }
}
