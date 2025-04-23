package com.dajava.backend.domain.event.es.scheduler.vaildation.htmlparser;

import java.util.ArrayDeque;
import java.util.Deque;

import com.dajava.backend.domain.event.es.scheduler.vaildation.htmlparser.state.InitState;
import com.dajava.backend.domain.event.es.scheduler.vaildation.htmlparser.state.ParserState;

public class HtmlParserContext {

	public String html;
	public int index = 0;
	public HtmlNode currentNode = new HtmlNode();
	public Deque<HtmlNode> nodeStack = new ArrayDeque<>();
	public ParserState currentState;

	public HtmlParserContext(String html) {
		this.html = html;
		this.currentState = new InitState(); // 첫 상태는 Init
	}

	public boolean hasNext() {
		return index < html.length();
	}

	public char peek() {
		return html.charAt(index);
	}

	public char consume() {
		return html.charAt(index++);
	}

	public void skipWhitespace() {
		while (hasNext() && Character.isWhitespace(peek())) {
			consume();
		}
	}

	public boolean match(String expected) {
		if (html.startsWith(expected, index)) {
			index += expected.length();
			return true;
		}
		return false;
	}

	public String readUntil(char... endChars) {
		StringBuilder sb = new StringBuilder();
		while (hasNext() && !contains(endChars, peek())) {
			sb.append(consume());
		}
		return sb.toString().trim();
	}

	private boolean contains(char[] arr, char c) {
		for (char x : arr) {
			if (x == c) return true;
		}
		return false;
	}
}
