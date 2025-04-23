package com.dajava.backend.domain.event.es.scheduler.vaildation.htmlparser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HtmlNode {

	public String tagName;
	public Map<String, String> attributes = new HashMap<>();
	public List<HtmlNode> children = new ArrayList<>();
	public String textContent = "";
	public boolean malformed = false;

	@Override
	public String toString() {
		return "Tag: " + tagName + ", Attrs: " + attributes + ", Text: " + textContent + ", Malformed: " + malformed;
	}
}
