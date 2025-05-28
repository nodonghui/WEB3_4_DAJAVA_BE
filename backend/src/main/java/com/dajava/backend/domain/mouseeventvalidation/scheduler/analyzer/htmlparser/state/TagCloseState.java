package com.dajava.backend.domain.mouseeventvalidation.scheduler.analyzer.htmlparser.state;

import com.dajava.backend.domain.mouseeventvalidation.scheduler.analyzer.htmlparser.HtmlParserContext;

public class TagCloseState implements ParserState {
	@Override
	public void handle(HtmlParserContext context) {
		context.match("</" + context.currentNode.tagName + ">");
		context.nodeStack.pop();
		context.currentState = new InitState();
	}
}