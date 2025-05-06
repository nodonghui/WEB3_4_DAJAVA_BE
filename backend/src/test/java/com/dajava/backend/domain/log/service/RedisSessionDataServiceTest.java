package com.dajava.backend.domain.log.service;

import com.dajava.backend.domain.event.es.entity.SessionDataDocument;
import com.dajava.backend.domain.event.es.repository.SessionDataDocumentRepository;
import com.dajava.backend.domain.log.dto.identifier.SessionIdentifier;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.openMocks;

class RedisSessionDataServiceTest {

	@Mock
	private SessionDataDocumentRepository repository;

	@InjectMocks
	private RedisSessionDataService redisSessionDataService;

	private SessionIdentifier sessionIdentifier;

	@BeforeEach
	void setUp() {
		openMocks(this);
		sessionIdentifier = new SessionIdentifier("test-session-id", "localhost:3000", "member-123");
	}

	@Test
	@DisplayName("DB에 세션 데이터가 없으면 새로 생성하여 반환한다")
	void t001() {
		// given
		when(repository.findByPageUrlAndSessionIdAndMemberSerialNumber(anyString(), anyString(), anyString()))
			.thenReturn(Optional.empty());

		SessionDataDocument savedDocument = SessionDataDocument.create(
			sessionIdentifier.getSessionId(),
			sessionIdentifier.getMemberSerialNumber(),
			sessionIdentifier.getPageUrl(),
			System.currentTimeMillis()
		);
		when(repository.save(any(SessionDataDocument.class))).thenReturn(savedDocument);

		// when
		SessionDataDocument result = redisSessionDataService.createOrFindSessionDataDocument(sessionIdentifier);

		// then
		assertThat(result).isNotNull();
		assertThat(result.getSessionId()).isEqualTo(sessionIdentifier.getSessionId());
		verify(repository).findByPageUrlAndSessionIdAndMemberSerialNumber(
			sessionIdentifier.getPageUrl(),
			sessionIdentifier.getSessionId(),
			sessionIdentifier.getMemberSerialNumber()
		);
		verify(repository).save(any(SessionDataDocument.class));
	}

	@Test
	@DisplayName("DB에 세션 데이터가 존재하면 그것을 반환한다")
	void t002() {
		// given
		SessionDataDocument existingDocument = SessionDataDocument.create(
			sessionIdentifier.getSessionId(),
			sessionIdentifier.getMemberSerialNumber(),
			sessionIdentifier.getPageUrl(),
			System.currentTimeMillis()
		);

		when(repository.findByPageUrlAndSessionIdAndMemberSerialNumber(anyString(), anyString(), anyString()))
			.thenReturn(Optional.of(existingDocument));

		// when
		SessionDataDocument result = redisSessionDataService.createOrFindSessionDataDocument(sessionIdentifier);

		// then
		assertThat(result).isEqualTo(existingDocument);
		verify(repository).findByPageUrlAndSessionIdAndMemberSerialNumber(
			sessionIdentifier.getPageUrl(),
			sessionIdentifier.getSessionId(),
			sessionIdentifier.getMemberSerialNumber()
		);
		verify(repository, never()).save(any());
	}

	@Test
	@DisplayName("캐시에 존재하는 세션 데이터는 repository 호출 없이 반환된다")
	void t003() {
		// given
		SessionDataDocument document = SessionDataDocument.create(
			sessionIdentifier.getSessionId(),
			sessionIdentifier.getMemberSerialNumber(),
			sessionIdentifier.getPageUrl(),
			System.currentTimeMillis()
		);

		when(repository.findByPageUrlAndSessionIdAndMemberSerialNumber(anyString(), anyString(), anyString()))
			.thenReturn(Optional.of(document));

		// 첫 번째 호출로 캐시에 저장
		redisSessionDataService.createOrFindSessionDataDocument(sessionIdentifier);
		reset(repository); // 이후 호출 검증 위해 초기화

		// when
		SessionDataDocument cachedResult = redisSessionDataService.createOrFindSessionDataDocument(sessionIdentifier);

		// then
		assertThat(cachedResult).isEqualTo(document);
		verifyNoInteractions(repository); // 호출이 없어야 한다
	}

	@Test
	@DisplayName("removeFromEsCache 호출 시 캐시에서 세션 데이터가 제거된다")
	void t004() {
		// given
		SessionDataDocument document = SessionDataDocument.create(
			sessionIdentifier.getSessionId(),
			sessionIdentifier.getMemberSerialNumber(),
			sessionIdentifier.getPageUrl(),
			System.currentTimeMillis()
		);
		when(repository.findByPageUrlAndSessionIdAndMemberSerialNumber(anyString(), anyString(), anyString()))
			.thenReturn(Optional.of(document));

		// 캐시에 저장
		redisSessionDataService.createOrFindSessionDataDocument(sessionIdentifier);

		// when
		redisSessionDataService.removeFromEsCache(sessionIdentifier);

		// 캐시 제거 후 다시 호출
		redisSessionDataService.createOrFindSessionDataDocument(sessionIdentifier);

		// then
		verify(repository, times(2)).findByPageUrlAndSessionIdAndMemberSerialNumber(
			sessionIdentifier.getPageUrl(),
			sessionIdentifier.getSessionId(),
			sessionIdentifier.getMemberSerialNumber()
		);
	}
}
