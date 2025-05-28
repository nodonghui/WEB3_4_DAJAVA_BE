package com.dajava.backend.domain.mouseeventvalidation.scheduler.analyzer.htmlparser.state;

import com.dajava.backend.domain.mouseeventvalidation.scheduler.analyzer.htmlparser.HtmlParserContext;

public interface ParserState {
	void handle(HtmlParserContext context);
}
