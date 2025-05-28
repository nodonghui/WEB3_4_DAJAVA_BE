package com.dajava.backend.domain.mouseeventvalidation.scheduler.analyzer.htmlparser.state;

import com.dajava.backend.domain.mouseeventvalidation.scheduler.analyzer.htmlparser.HtmlNode;
import com.dajava.backend.domain.mouseeventvalidation.scheduler.analyzer.htmlparser.HtmlParserContext;

public class TextNodeState implements ParserState {
	@Override
	public void handle(HtmlParserContext context) {
		StringBuilder text = new StringBuilder();
		while (context.hasNext() && context.peek() != '<') {
			text.append(context.consume());
		}

		String content = text.toString().trim();
		if (!content.isEmpty()) {
			HtmlNode node = new HtmlNode();
			node.textContent = content;
			context.nodeStack.peek().children.add(node);
		}

		context.currentState = new InitState();
	}
}