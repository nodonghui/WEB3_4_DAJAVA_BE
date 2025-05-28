package com.dajava.backend.domain.mouseeventvalidation.scheduler.analyzer;

import java.util.List;

/**
 * EsAnalyzer를 묶는 인터페이스
 *  @author NohDongHui
 */
public interface Analyzer<T> {
	void analyze(List<T> eventDocuments);

}
