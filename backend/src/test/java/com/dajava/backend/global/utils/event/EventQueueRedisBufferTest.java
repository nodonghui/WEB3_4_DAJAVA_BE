package com.dajava.backend.global.utils.event;

import com.dajava.backend.domain.mouseeventsave.infra.redis.dto.ClickEventRequest;
import com.dajava.backend.domain.mouseeventsave.infra.redis.dto.identifier.SessionIdentifier;
import com.dajava.backend.utils.event.EventQueueRedisBuffer;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class EventQueueRedisBufferTest {

	private final StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
	private final ListOperations<String, String> listOperations = mock(ListOperations.class);
	private final ObjectMapper objectMapper = new ObjectMapper();

	private final EventQueueRedisBuffer<ClickEventRequest> buffer =
		new EventQueueRedisBuffer<>(redisTemplate, objectMapper, ClickEventRequest.class);

	private final SessionIdentifier identifier =
		new SessionIdentifier("session123", "localhost", "member001");

	private final ClickEventRequest event = ClickEventRequest.builder()
		.eventId("event123")
		.timestamp(123456789L)
		.browserWidth(1920)
		.scrollHeight(3000)
		.viewportHeight(900)
		.sessionIdentifier(identifier)
		.clientX(100)
		.clientY(200)
		.scrollY(300)
		.tag("button")
		.build();

	@Test
	@DisplayName("t001 - 클릭 이벤트를 Redis에 캐싱할 수 있다")
	void t001() {
		// given
		when(redisTemplate.opsForList()).thenReturn(listOperations);
		when(redisTemplate.expire(anyString(), eq(1L), eq(TimeUnit.HOURS))).thenReturn(true);
		when(redisTemplate.opsForValue()).thenReturn(mock(org.springframework.data.redis.core.ValueOperations.class));

		// when
		buffer.cacheEvents(identifier, event);

		// then
		verify(redisTemplate).opsForList();
		verify(listOperations).leftPush(anyString(), anyString());
		verify(redisTemplate).expire(anyString(), eq(1L), eq(TimeUnit.HOURS));
	}

	@Test
	@DisplayName("t002 - Redis에 저장된 클릭 이벤트를 조회할 수 있다")
	void t002() throws Exception {
		// given
		String json = objectMapper.writeValueAsString(event);
		when(redisTemplate.opsForList()).thenReturn(listOperations);
		when(listOperations.range(anyString(), eq(0L), eq(-1L)))
			.thenReturn(List.of(json));

		// when
		List<ClickEventRequest> result = buffer.getEvents(identifier, event);

		// then
		assertThat(result).hasSize(1);
		ClickEventRequest actual = result.get(0);
		assertThat(actual.getClientX()).isEqualTo(100);
		assertThat(actual.getClientY()).isEqualTo(200);
		assertThat(actual.getScrollY()).isEqualTo(300);
		assertThat(actual.getTag()).isEqualTo("button");
		assertThat(actual.getSessionIdentifier()).isEqualTo(identifier);
	}

	@Test
	@DisplayName("t003 - Redis에 저장된 클릭 이벤트를 삭제하고 반환할 수 있다")
	void t003() throws Exception {
		// given
		String json = objectMapper.writeValueAsString(event);
		when(redisTemplate.opsForList()).thenReturn(listOperations);
		when(listOperations.range(anyString(), eq(0L), eq(-1L)))
			.thenReturn(List.of(json));

		// when
		List<ClickEventRequest> flushed = buffer.flushEvents(identifier, event);

		// then
		verify(redisTemplate, times(2)).delete(anyString());
		assertThat(flushed).hasSize(1);
		assertThat(flushed.get(0).getTag()).isEqualTo("button");
	}
}
