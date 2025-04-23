package com.dajava.backend.domain.event.es.scheduler.vaildation.htmlparser.state;

import com.dajava.backend.domain.event.es.scheduler.vaildation.htmlparser.HtmlParserContext;

public interface ParserState {
	void handle(HtmlParserContext context);
}
