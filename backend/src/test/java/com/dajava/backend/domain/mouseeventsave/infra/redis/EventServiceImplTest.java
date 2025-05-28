package com.dajava.backend.domain.mouseeventsave.infra.redis;

import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.dajava.backend.domain.mouseeventsave.infra.redis.dto.ClickEventRequest;
import com.dajava.backend.domain.mouseeventsave.infra.redis.dto.identifier.SessionIdentifier;
import com.dajava.backend.domain.mouseeventsave.infra.redis.service.EventServiceImpl;
import com.dajava.backend.domain.mouseeventsave.infra.redis.service.RedisSessionDataService;
import com.dajava.backend.utils.event.EventRedisBuffer;

class EventServiceImplTest {
	@Mock
	private EventRedisBuffer eventRedisBuffer;

	@Mock
	private RedisSessionDataService redisSessionDataService;

	@InjectMocks
	private EventServiceImpl eventService;

	@BeforeEach
	void setUp() {
		MockitoAnnotations.openMocks(this);
	}

	private SessionIdentifier sessionIdentifier() {
		return new SessionIdentifier("session123", "http://page.com", "member456");
	}


	@Test
	void createClickEvent_shouldCallRedisSessionDataServiceAndBuffer() {
		// given
		SessionIdentifier identifier = sessionIdentifier();
		ClickEventRequest request = mock(ClickEventRequest.class);
		when(request.getSessionIdentifier()).thenReturn(identifier);

		// when
		eventService.createClickEvent(request);

		// then
		verify(redisSessionDataService).createOrFindSessionDataDocument(eq(identifier));
		verify(eventRedisBuffer).addClickEvent(eq(request), eq(identifier));
	}

	@Test
	void createMoveEvent() {
	}

	@Test
	void createScrollEvent() {
	}
}