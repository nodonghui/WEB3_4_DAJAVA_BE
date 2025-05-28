package com.dajava.backend.domain.mouseeventvalidation.service;

import java.util.List;

import com.dajava.backend.domain.mouseeventsave.infra.memory.entity.PointerClickEventDocument;
import com.dajava.backend.domain.mouseeventsave.infra.memory.entity.PointerMoveEventDocument;
import com.dajava.backend.domain.mouseeventsave.infra.memory.entity.PointerScrollEventDocument;

/**
 *
 * PointerEventDocument 도메인의 서비스 로직을 처리하는 인터페이스
 *
 * @author NohDongHui
 */
public interface PointerEventDocumentService {

	/**
	 * sessionId에 해당하는 ClickEventDocument를 내부적으로 배처 처리해 나눠 가져옴
	 * @param sessionId, batchSize
	 * @return sessionId에 해당하는 모든 pointerClickEventDocument
	 */
	public List<PointerClickEventDocument> fetchAllClickEventDocumentsBySessionId(String sessionId,int batchSize);

	/**
	 * sessionId에 해당하는 ClickEventDocument를 내부적으로 배처 처리해 나눠 가져옴
	 * @param sessionId, batchSize
	 * @return sessionId에 해당하는 모든 pointerMoveEventDocument
	 */
	public List<PointerMoveEventDocument> fetchAllMoveEventDocumentsBySessionId(String sessionId, int batchSize);

	/**
	 * sessionId에 해당하는 ClickEventDocument를 내부적으로 배처 처리해 나눠 가져옴
	 * @param sessionId, batchSize
	 * @return sessionId에 해당하는 모든 pointerScrollEventDocument
	 */
	public List<PointerScrollEventDocument> fetchAllScrollEventDocumentsBySessionId(String sessionId, int batchSize);

	/**
	 * sessionId와 url에 해당하는 클릭 이벤트 개수 반환
	 * @param sessionId
	 * @param pageUrl
	 * @return
	 */
	long countClickEvents(String sessionId, String pageUrl);

	/**
	 * sessionId와 url에 해당하는 무브 이벤트 개수 반환
	 * @param sessionId
	 * @param pageUrl
	 * @return
	 */
	long countMoveEvents(String sessionId, String pageUrl);

	/**
	 * sessionId와 url에 해당하는 스크롤 이벤트 개수 반환
	 * @param sessionId
	 * @param pageUrl
	 * @return
	 */
	long countScrollEvents(String sessionId, String pageUrl);

	/**
	 * sessionId와 url에 해당하는
	 * 세 종류의 이벤트 총합을 반환하는 메서드
	 */
	long countAllEvents(String sessionId, String pageUrl);
}
