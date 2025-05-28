package com.dajava.backend.domain.mouseeventvalidation.scheduler.analyzer.htmlparser.state;

import com.dajava.backend.domain.mouseeventvalidation.scheduler.analyzer.htmlparser.HtmlNode;
import com.dajava.backend.domain.mouseeventvalidation.scheduler.analyzer.htmlparser.HtmlParserContext;

public class TagOpenState implements ParserState {
	@Override
	public void handle(HtmlParserContext context) {
		context.match("<");
		String tagName = context.readUntil(' ', '>', '/');


		HtmlNode node = new HtmlNode();

		// ✅ 현재 부모 노드에 자식으로 추가
		if (!context.nodeStack.isEmpty()) {
			context.nodeStack.peek().children.add(node);
		}

		node.tagName = tagName;
		context.currentNode = node;
		context.nodeStack.push(node);

		context.currentState = new AttributeState();
	}
}

