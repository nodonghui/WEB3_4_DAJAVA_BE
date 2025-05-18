package com.dajava.backend.domain.event.es.scheduler.abusingCheck;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.dajava.backend.domain.event.es.entity.AbusingBaseLine;
import com.dajava.backend.domain.event.exception.AbusingBaseLineException;

public class AbusingCheckProcessorTest {

	@Test
	@DisplayName("베이지안 평균 정상 작동 테스트")
	void testBayesianAverageCalculation() {
		AbusingBaseLine baseline = AbusingBaseLine.builder()
			.averageEventsPerHour(400)
			.standardDeviation(80)
			.sampleSize(10)
			.m2(10000)
			.priorAverage(300)
			.priorWeight(10)
			.pageUrl("/test")
			.build();

		double expected = (10 * 300 + 10 * 400) / (double)(10 + 10);
		assertEquals(expected, baseline.calculateBayesianAverage(), 0.001);
	}

	@Test
	@DisplayName("극단적인 데이터가 들어와도 베이지안 평균이 천천히 변하는지 확인")
	void testBayesianAverageStabilityWithOutlier() {
		// 기존 baseline
		AbusingBaseLine baseline = AbusingBaseLine.builder()
			.averageEventsPerHour(400)
			.standardDeviation(80)
			.sampleSize(10)
			.m2(10000)
			.priorAverage(300)
			.priorWeight(10)
			.pageUrl("/test")
			.build();

		// 기존 Bayesian 평균
		double oldBayesian = baseline.calculateBayesianAverage();
		assertEquals(350.0, oldBayesian, 0.001); // (10*300 + 10*400)/20 = 350.0

		// 이상치 데이터 추가: eventCount = 3000
		long newEventCount = 3000;
		long oldAvg = baseline.getAverageEventsPerHour();
		int sampleSize = baseline.getSampleSize();
		double m2 = baseline.getM2();

		int newSampleSize = sampleSize + 1;
		double delta = newEventCount - oldAvg;
		double newAvg = oldAvg + delta / newSampleSize;
		double delta2 = newEventCount - newAvg;
		double newM2 = m2 + delta * delta2;
		double newVariance = newM2 / (newSampleSize - 1);
		double newStddev = Math.sqrt(newVariance);

		// Welford 업데이트
		baseline.updateStatistics(Math.round(newAvg), newStddev, newSampleSize, newM2);

		// 갱신 후 Bayesian 평균 계산
		double newBayesian = baseline.calculateBayesianAverage();

		// ✅ 극단적인 값 하나로 Bayesian 평균이 급격히 오르지 않음을 확인
		System.out.println("▶ oldBayesian: " + oldBayesian);
		System.out.println("▶ newBayesian: " + newBayesian);
		assertTrue(newBayesian < 600); // 급격한 상승 방지
		assertTrue(newBayesian > 350); // 과도한 하락도 방지
	}

	@Test
	@DisplayName("welFord 알고리즘이 정상 작동 하는지 테스트")
	void testWelfordUpdateCorrectness() {
		AbusingBaseLine baseline = AbusingBaseLine.builder()
			.averageEventsPerHour(100)
			.standardDeviation(0)
			.sampleSize(1)
			.m2(0)
			.priorAverage(0)
			.priorWeight(0)
			.pageUrl("/test")
			.build();

		long newValue = 300;

		// Welford 갱신
		long avg = baseline.getAverageEventsPerHour();
		int sampleSize = baseline.getSampleSize();
		double m2 = baseline.getM2();

		int newSampleSize = sampleSize + 1;
		double delta = newValue - avg;
		double newAvg = avg + delta / newSampleSize;
		double delta2 = newValue - newAvg;
		double newM2 = m2 + delta * delta2;
		double newVariance = newM2 / (newSampleSize - 1);
		double newStddev = Math.sqrt(newVariance);

		// 갱신 적용
		baseline.updateStatistics(Math.round(newAvg), newStddev, newSampleSize, newM2);

		assertEquals(2, baseline.getSampleSize());
		assertEquals(200, baseline.getAverageEventsPerHour());
		assertEquals(Math.sqrt(20000), baseline.getStandardDeviation(), 0.001);
	}

	@Test
	@DisplayName("PriorAverage값을 두번째 설정했을 때 예외 반환하는지 확인")
	void testPriorAverageSetOnlyOnce() {
		AbusingBaseLine baseline = AbusingBaseLine.builder()
			.averageEventsPerHour(500)
			.standardDeviation(50)
			.sampleSize(10)
			.m2(5000)
			.priorAverage(0)
			.priorWeight(10)
			.pageUrl("/test")
			.build();

		baseline.setPriorAverage(450);
		assertEquals(450, baseline.getPriorAverage());

		// 두 번째 설정 시 예외 발생해야 함
		assertThrows(AbusingBaseLineException.class, () -> {
			baseline.setPriorAverage(400);
		});
	}

	@Test
	@DisplayName("모집단 분산과 welford 방식으로 구한 표본 분산이 큰 차이 없는지 확인")
	void testWelfordVsDirectVariance() {
		// 1. 직접 계산용 데이터
		long[] data = {300, 320, 290, 310, 305, 295, 315, 300, 310, 305};
		int n = data.length;

		// 2. 직접 계산 - 모집단 평균과 분산
		double sum = 0;
		for (long val : data) sum += val;
		double mean = sum / n;

		double squaredDiffSum = 0;
		for (long val : data) {
			double diff = val - mean;
			squaredDiffSum += diff * diff;
		}
		double directVariance = squaredDiffSum / n; // 모집단 분산
		double directStddev = Math.sqrt(directVariance);

		// 3. Welford 방식
		long avg = data[0];
		int sampleSize = 1;
		double m2 = 0;

		for (int i = 1; i < data.length; i++) {
			long newValue = data[i];
			sampleSize += 1;
			double delta = newValue - avg;
			avg = avg + (long)(delta / sampleSize);
			double delta2 = newValue - avg;
			m2 = m2 + delta * delta2;
		}
		double welfordVariance = m2 / (sampleSize - 1); // 표본 분산
		double welfordStddev = Math.sqrt(welfordVariance);

		System.out.println("▶ 직접 계산 stddev: " + directStddev);
		System.out.println("▶ Welford stddev : " + welfordStddev);

		// 4. 차이 검증
		assertEquals(directStddev, welfordStddev, 2.0); // 2 정도 이내면 거의 동일
	}
}
