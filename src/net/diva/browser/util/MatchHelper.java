package net.diva.browser.util;

import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MatchHelper {
	@SuppressWarnings("serial")
	public static class NoMatchException extends Exception {
		public NoMatchException() {
		}
	}

	private static class Region {
		int start;
		int end;

		public Region(int s, int e) {
			start = s;
			end = e;
		}
	}

	private CharSequence m_input;
	private Matcher m_matcher;
	private int m_position = -1;
	private Stack<Region> m_regions = new Stack<Region>();

	public MatchHelper(CharSequence input) {
		m_input = input;
	}

	private Matcher usePattern(Pattern pattern) {
		if (m_matcher == null)
			m_matcher = pattern.matcher(m_input);
		else if (!m_matcher.pattern().equals(pattern))
			m_matcher.usePattern(pattern);

		return m_matcher;
	}

	public boolean find(Pattern pattern) {
		usePattern(pattern);

		boolean matched = m_position < 0 ? m_matcher.find() : m_matcher.find(m_position);
		if (matched)
			m_position = m_matcher.end();
		return matched;
	}

	public String findString(Pattern pattern, int group) throws NoMatchException {
		if (!find(pattern))
			throw new NoMatchException();

		return m_matcher.group(group);
	}

	public String findString(Pattern pattern, int group, String defaultValue) {
		return find(pattern) ? m_matcher.group(group) : defaultValue;
	}

	public int findInteger(Pattern pattern, int group) throws NoMatchException {
		return Integer.parseInt(findString(pattern, group));
	}

	public int findInteger(Pattern pattern, int group, int defaultValue) {
		final String value = findString(pattern, group, null);
		if (value == null)
			return defaultValue;
		return Integer.parseInt(value);
	}

	public long findLong(Pattern pattern, int group, int defaultValue) {
		final String value = findString(pattern, group, null);
		if (value == null)
			return defaultValue;
		return Long.parseLong(value);
	}

	public int groupCount() {
		return m_matcher.groupCount();
	}

	public String group(int group) {
		return m_matcher.group(group);
	}

	public boolean bind(Pattern from, Pattern to) {
		int oldPosition = m_position;
		if (!find(from))
			return false;
		int start = m_matcher.end();
		if (!find(to)) {
			m_position = oldPosition;
			return false;
		}

		m_regions.push(new Region(m_matcher.regionStart(), m_matcher.regionEnd()));
		m_position = start;
		m_matcher.region(start, m_matcher.start());
		return true;
	}

	public void unbind() {
		if (m_regions.isEmpty())
			return;

		Region r = m_regions.pop();
		m_matcher.region(r.start, r.end);
	}
}
