package com.dajava.backend.global.utils.event;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.springframework.data.redis.core.StringRedisTemplate;

import com.dajava.backend.domain.log.dto.identifier.SessionIdentifier;
import com.dajava.backend.domain.log.exception.LogException;
import com.fasterxml.jackson.databind.ObjectMapper;

import static com.dajava.backend.global.exception.ErrorCode.*;

/**
 * EventQueueRedisBuffer는 이벤트 객체를 Redis의 리스트(List) 구조를 사용하여
 * 세션 기반으로 저장하고 조회, 삭제, 메타데이터 갱신 등의 기능을 제공하는 클래스입니다.
 * 제네릭 타입 {@code T}를 사용하여 클릭, 무브, 스크롤 등 다양한 이벤트 타입을 처리할 수 있습니다.
 * 이벤트는 Redis의 key-value 구조로 저장되며, key는 SessionIdentifier와 이벤트 타입 기반으로 생성됩니다.
 *
 * @param <T> 이벤트 타입 (예: ClickEventRequest, MovementEventRequest 등)
 * @author jhon S, sungkibum
 */

public class EventQueueRedisBuffer<T> {

	private final StringRedisTemplate redisTemplate;
	private final EventSerializer<T> serializer;
	private final MetadataManager metadataManager;

	public EventQueueRedisBuffer(StringRedisTemplate redisTemplate, ObjectMapper objectMapper, Class<T> clazz) {
		this.redisTemplate = redisTemplate;
		this.serializer = new EventSerializer<>(objectMapper, clazz);
		this.metadataManager = new MetadataManager(redisTemplate);
	}

	/**
	 * Redis에 이벤트를 저장하는 메서드
	 *
	 * @param sessionIdentifier 세션 식별자
	 * @param event 저장할 이벤트 객체
	 * @throws LogException Redis 저장 중 오류 발생 시
	 */
	public void cacheEvents(SessionIdentifier sessionIdentifier, T event) {
		String eventKey = KeyGenerator.buildEventKey( sessionIdentifier,event);
		String updatedKey = KeyGenerator.buildLastUpdatedKey(eventKey);

		try {
			String json = serializer.serialize(event);
			redisTemplate.opsForList().leftPush(eventKey, json);
			redisTemplate.expire(eventKey, 1, TimeUnit.HOURS);
			metadataManager.updateLastUpdated(updatedKey);
		} catch (Exception e) {
			throw new LogException(REDIS_CACHING_ERROR);
		}
	}

	/**
	 * Redis로부터 이벤트 리스트를 조회하는 메서드
	 *
	 * @param sessionIdentifier 세션 식별자
	 * @param event 이벤트 클래스의 빈 객체 (타입 유추용)
	 * @return 조회된 이벤트 리스트 (없으면 빈 리스트 반환)
	 */
	public List<T> getEvents(SessionIdentifier sessionIdentifier, T event) {
		String key = KeyGenerator.buildEventKey(sessionIdentifier, event);
		List<String> jsonList = redisTemplate.opsForList().range(key, 0, -1);

		if (jsonList == null || jsonList.isEmpty()) return Collections.emptyList();

		return jsonList.stream()
			.map(serializer::deserialize)
			.filter(Objects::nonNull)
			.collect(Collectors.toList());
	}

	/**
	 * 이벤트를 조회한 뒤, Redis에서 삭제하고 반환하는 메서드
	 *
	 * @param sessionIdentifier 세션 식별자
	 * @param event 이벤트 클래스의 빈 객체 (타입 유추용)
	 * @return 삭제 전 조회된 이벤트 리스트
	 */
	public List<T> flushEvents(SessionIdentifier sessionIdentifier,T event) {
		String eventKey = KeyGenerator.buildEventKey( sessionIdentifier,event);
		String updatedKey = KeyGenerator.buildLastUpdatedKey(eventKey);

		List<T> events = getEvents(sessionIdentifier,event);
		redisTemplate.delete(eventKey);
		redisTemplate.delete(updatedKey);
		return events;
	}

	/**
	 * 특정 키에 대한 마지막 업데이트 시간을 조회하는 메서드
	 *
	 * @param key Redis 키
	 * @return UNIX timestamp 형식의 업데이트 시간 (ms 단위), 없으면 null
	 */
	public Long getLastUpdated(String key) {
		String updatedKey = KeyGenerator.buildLastUpdatedKey(key);
		return metadataManager.getLastUpdated(updatedKey);
	}

	/**
	 * Redis에서 event 및 lastUpdated로 시작하는 모든 키를 제거하는 메서드
	 */
	public void clearAll() {
		metadataManager.clearKeysByPattern("event:*");
		metadataManager.clearKeysByPattern("lastUpdated:*");
	}
}
