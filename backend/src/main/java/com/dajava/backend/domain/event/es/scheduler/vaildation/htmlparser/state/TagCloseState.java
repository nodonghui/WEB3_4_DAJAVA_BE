package com.dajava.backend.domain.event.es.scheduler.vaildation.htmlparser.state;

import com.dajava.backend.domain.event.es.scheduler.vaildation.htmlparser.HtmlParserContext;

public class TagCloseState implements ParserState {
	@Override
	public void handle(HtmlParserContext context) {
		context.match("</" + context.currentNode.tagName + ">");
		context.nodeStack.pop();
		context.currentState = new InitState();
	}
}