package com.dajava.backend.domain.mouseeventvalidation.service;

import java.util.List;

import org.springframework.data.domain.Page;

import com.dajava.backend.domain.mouseeventsave.infra.memory.entity.SessionDataDocument;

/**
 *
 * SessionDataDocumen 도메인의 서비스 로직을 처리하는 인터페이스
 *
 * @author NohDongHui
 */
public interface SessionDataDocumentService {

	/**
	 * 세션이 끝난 데이터를 페이징으로 분할해 반환
	 *  스케줄러에서 for문으로 분할해 가져옴
	 * @param page, pageSize
	 * @return SessionDataDocuments 일부를 가진 페이징 객체
	 */
	public Page<SessionDataDocument> getEndedSessions(int page, int size);

	/**
	 * 세션데이터 저장 메서드
	 *  현재 검증 스케줄러에서 세션 검증 후 isverify 변경사항 저장하려고 사용
	 * @param sessionDataDocument
	 * @return
	 */
	public void save(SessionDataDocument sessionDataDocument);

	/**
	 * 현재 시각으로 부터 1시간 이내 세션 데이터를 조회하는 메서드
	 * @return
	 */
	public List<SessionDataDocument> getRecentSessionsInLastHour();
}
