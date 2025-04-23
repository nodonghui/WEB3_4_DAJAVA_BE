package com.dajava.backend.domain.heatmap.filter;

import java.util.List;

import com.dajava.backend.domain.event.es.entity.SolutionEventDocument;
import com.dajava.backend.domain.heatmap.validation.ScreenWidthValidator;
import com.dajava.backend.domain.heatmap.validation.UrlEqualityValidator;

public class SolutionEventFilter {
	private static final UrlEqualityValidator urlEqualityValidator = new UrlEqualityValidator();
	private static final ScreenWidthValidator screenWidthValidator = new ScreenWidthValidator();

	public static List<SolutionEventDocument> filter(List<SolutionEventDocument> events, String targetUrl, int widthRange) {
		return events.stream()
			.filter(event -> urlEqualityValidator.isMatching(targetUrl, event.getPageUrl())
				&& screenWidthValidator.normalizeToWidthRange(event.getBrowserWidth()) == widthRange)
			.toList();
	}
}
