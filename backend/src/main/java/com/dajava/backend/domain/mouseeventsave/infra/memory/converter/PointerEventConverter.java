package com.dajava.backend.domain.mouseeventsave.infra.memory.converter;

import java.util.ArrayList;
import java.util.List;

import com.dajava.backend.domain.mouseeventsave.infra.memory.dto.PointerClickEventRequest;
import com.dajava.backend.domain.mouseeventsave.infra.memory.dto.PointerMoveEventRequest;
import com.dajava.backend.domain.mouseeventsave.infra.memory.dto.PointerScrollEventRequest;
import com.dajava.backend.domain.mouseeventsave.infra.memory.entity.PointerClickEventDocument;
import com.dajava.backend.domain.mouseeventsave.infra.memory.entity.PointerMoveEventDocument;
import com.dajava.backend.domain.mouseeventsave.infra.memory.entity.PointerScrollEventDocument;
import com.dajava.backend.domain.mouseeventvalidation.entity.SolutionEventDocument;
import com.dajava.backend.utils.TimeUtils;

public class PointerEventConverter {

	//utiltiy class에서 생성자 호출되는 경우 막음
	private PointerEventConverter() {
	}


	public static PointerClickEventDocument toClickEventDocument(PointerClickEventRequest request) {
		return PointerClickEventDocument.builder()
			.id(request.getEventId() + request.getTimestamp())
			.sessionId(request.getSessionId())
			.pageUrl(request.getPageUrl())
			.memberSerialNumber(request.getMemberSerialNumber())
			.timestamp(request.getTimestamp())
			.browserWidth(request.getBrowserWidth())
			.clientX(request.getClientX())
			.clientY(request.getClientY())
			.scrollY(request.getScrollY())
			.scrollHeight(request.getScrollHeight())
			.viewportHeight(request.getViewportHeight())
			.element(request.getElement())
			.isOutlier(false)
			.build();
	}

	public static PointerMoveEventDocument toMoveEventDocument(PointerMoveEventRequest request) {
		return PointerMoveEventDocument.builder()
			.id(request.getEventId() + request.getTimestamp())
			.sessionId(request.getSessionId())
			.pageUrl(request.getPageUrl())
			.memberSerialNumber(request.getMemberSerialNumber())
			.timestamp(request.getTimestamp())
			.browserWidth(request.getBrowserWidth())
			.clientX(request.getClientX())
			.clientY(request.getClientY())
			.scrollY(request.getScrollY())
			.scrollHeight(request.getScrollHeight())
			.viewportHeight(request.getViewportHeight())
			.isOutlier(false)
			.build();
	}

	public static PointerScrollEventDocument toScrollEventDocument(PointerScrollEventRequest request) {
		return PointerScrollEventDocument.builder()
			.id(request.getEventId() + request.getTimestamp())
			.sessionId(request.getSessionId())
			.pageUrl(request.getPageUrl())
			.memberSerialNumber(request.getMemberSerialNumber())
			.timestamp(request.getTimestamp())
			.browserWidth(request.getBrowserWidth())
			.scrollY(request.getScrollY())
			.scrollHeight(request.getScrollHeight())
			.viewportHeight(request.getViewportHeight())
			.isOutlier(false)
			.build();
	}

	public static SolutionEventDocument fromClickDocument(PointerClickEventDocument event) {

		return SolutionEventDocument.builder()
			.id(event.getId())
			.sessionId(event.getSessionId())
			.pageUrl(event.getPageUrl())
			.serialNumber(event.getMemberSerialNumber())
			.type("click")
			.clientX(event.getClientX())
			.clientY(event.getClientY())
			.timestamp(TimeUtils.toEpochMillis(event.getTimestamp()))
			.browserWidth(event.getBrowserWidth())
			.scrollY(event.getScrollY())
			.scrollHeight(event.getScrollHeight())
			.viewportHeight(event.getViewportHeight())
			.element(event.getElement()) // element를 tag로 쓸 경우
			.isOutlier(event.getIsOutlier())
			.build();
	}

	public static SolutionEventDocument fromMoveDocument(PointerMoveEventDocument event) {

		return SolutionEventDocument.builder()
			.id(event.getId())
			.sessionId(event.getSessionId())
			.pageUrl(event.getPageUrl())
			.serialNumber(event.getMemberSerialNumber())
			.type("move")
			.clientX(event.getClientX())
			.clientY(event.getClientY())
			.timestamp(TimeUtils.toEpochMillis(event.getTimestamp()))
			.browserWidth(event.getBrowserWidth())
			.scrollY(event.getScrollY())
			.scrollHeight(event.getScrollHeight())
			.viewportHeight(event.getViewportHeight())
			.isOutlier(event.getIsOutlier())
			.build();
	}

	public static SolutionEventDocument fromScrollDocument(PointerScrollEventDocument event) {

		return SolutionEventDocument.builder()
			.id(event.getId())
			.sessionId(event.getSessionId())
			.pageUrl(event.getPageUrl())
			.serialNumber(event.getMemberSerialNumber())
			.type("scroll")
			.clientX(null) // scroll 이벤트에는 X/Y 좌표가 없을 수도 있음
			.clientY(null)
			.timestamp(TimeUtils.toEpochMillis(event.getTimestamp()))
			.browserWidth(event.getBrowserWidth())
			.scrollY(event.getScrollY())
			.scrollHeight(event.getScrollHeight())
			.viewportHeight(event.getViewportHeight())
			.isOutlier(event.getIsOutlier())
			.build();
	}

	public static List<SolutionEventDocument> toSolutionEventDocuments(
		List<PointerClickEventDocument> clicks,
		List<PointerMoveEventDocument> moves,
		List<PointerScrollEventDocument> scrolls
	) {
		List<SolutionEventDocument> result = new ArrayList<>();

		result.addAll(clicks.stream()
			.map(PointerEventConverter::fromClickDocument)
			.toList());

		result.addAll(moves.stream()
			.map(PointerEventConverter::fromMoveDocument)
			.toList());

		result.addAll(scrolls.stream()
			.map(PointerEventConverter::fromScrollDocument)
			.toList());

		return result;
	}
}


