package com.dajava.backend.domain.mouseeventvalidation.scheduler.analyzer.htmlparser.state;

import com.dajava.backend.domain.mouseeventvalidation.scheduler.analyzer.htmlparser.HtmlNode;
import com.dajava.backend.domain.mouseeventvalidation.scheduler.analyzer.htmlparser.HtmlParserContext;

public class AttributeState implements ParserState {
	@Override
	public void handle(HtmlParserContext context) {
		HtmlNode node = context.currentNode;

		while (context.hasNext() && context.peek() != '>' && context.peek() != '/') {
			context.skipWhitespace();
			String attrName = context.readUntil('=', '>', ' ');
			if (context.match("=")) {
				char quote = context.peek();
				context.consume();
				String value = context.readUntil(quote);
				context.consume(); // skip quote
				node.attributes.put(attrName, value);
			}
		}

		if (context.peek() == '/') {
			context.match("/>");
			context.nodeStack.pop();
			context.currentState = new InitState();
		} else {
			context.match(">");
			context.currentState = new TagBodyState();
		}
	}
}
