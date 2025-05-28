package com.dajava.backend.domain.mouseeventvalidation.scheduler.analyzer.htmlparser;

public class FSMHtmlParser {

	public HtmlNode parse(String html) {
		HtmlParserContext context = new HtmlParserContext(html);
		HtmlNode root = new HtmlNode();
		root.tagName = "root";
		context.nodeStack.push(root);

		while (context.hasNext()) {
			context.currentState.handle(context);
		}

		return root;
	}
}
