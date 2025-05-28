package com.dajava.backend.utils.session;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.dajava.backend.domain.mouseeventsave.infra.redis.dto.identifier.SessionIdentifier;

import lombok.RequiredArgsConstructor;

/**
 * Redis에 현재 존재하는 모든 이벤트 유형에 대한 활성 세션 키를 한꺼번에 수집하는 유틸리티 컴포넌트 클래스
 * @author sungkibum, jhon S
 */

@Component
@RequiredArgsConstructor
public class SessionKeyCollector {
	private static final List<String> EVENT_TYPES = List.of("click", "move", "scroll");
	private final ActiveSessionManager activeSessionManager;

	public Set<SessionIdentifier> collectAllActiveSessionKeys() {
		Set<SessionIdentifier> allKeys = new HashSet<>();
		for (String type : EVENT_TYPES) {
			allKeys.addAll(activeSessionManager.getActiveSessionKeysForType(type));
		}
		return allKeys;
	}
}