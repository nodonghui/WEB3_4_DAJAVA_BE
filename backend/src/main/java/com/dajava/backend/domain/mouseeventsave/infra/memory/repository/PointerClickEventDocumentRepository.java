package com.dajava.backend.domain.mouseeventsave.infra.memory.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

import com.dajava.backend.domain.mouseeventsave.infra.memory.entity.PointerClickEventDocument;

/**
 * 클릭 이벤트를 저장하는 ES 인덱스 입니다.
 *  @author NohDongHui
 */
public interface PointerClickEventDocumentRepository
	extends ElasticsearchRepository<PointerClickEventDocument, String> {

	/**
	 * sessionId에 해당하는 pointerClickEventDocument를  페이징으로 분할하여 정렬해 가져옴
	 *
	 * @param sessionId , pageable
	 * @return List<PointerClickEventDocument>
	 */
	Page<PointerClickEventDocument> findBySessionId(String sessionId, Pageable pageable);

	boolean existsBySessionId(String sessionId);

	/**
	 * 해당 sesssionId와 pageUrl을 가지는 모든 클릭 이벤트의 개수 반환
	 * @param sessionId
	 * @param pageUrl
	 * @return
	 */
	long countBySessionIdAndPageUrl(String sessionId, String pageUrl);

}
