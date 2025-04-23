package com.dajava.backend.domain.event.es.scheduler.vaildation.htmlparser.state;

import com.dajava.backend.domain.event.es.scheduler.vaildation.htmlparser.HtmlParserContext;

public class InitState implements ParserState {
	@Override
	public void handle(HtmlParserContext context) {
		context.skipWhitespace();

		if (!context.hasNext()) return;

		if (context.peek() == '<') {
			context.currentState = new TagOpenState();
		} else {
			context.currentState = new TextNodeState();
		}
	}
}
