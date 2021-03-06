package net.diva.browser.service.parser;

import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.diva.browser.DdN;
import net.diva.browser.DdNIndex;
import net.diva.browser.model.CustomizeItem;
import net.diva.browser.model.MusicInfo;
import net.diva.browser.model.Ranking;
import net.diva.browser.model.Role;
import net.diva.browser.model.ScoreRecord;
import net.diva.browser.service.ParseException;
import net.diva.browser.util.CodeMap;
import net.diva.browser.util.MatchHelper;

public class MusicParser {
	private static final Pattern RE_MUSIC_TITLE = Pattern.compile("<a href=\"/divanet/pv/info/(\\w+)/0/\\d+\">(.+)</a>");

	public static String parseListPage(InputStream content, List<MusicInfo> list) {
		String body = Parser.read(content);
		Matcher m = RE_MUSIC_TITLE.matcher(body);
		while (m.find())
			list.add(new MusicInfo(m.group(1), m.group(2)));

		m = m.usePattern(Parser.RE_NEXT);
		return m.find() ? m.group(1) : null;
	}

	private static int findLiteral(Matcher m, String... candidates) {
		for (int i = 0; i < candidates.length; ++i) {
			m = m.usePattern(Pattern.compile(Pattern.quote(candidates[i])));
			if (m.find())
				return i + 1;
		}
		return 0;
	}

	private static final Pattern RE_BLOCKEND = Pattern.compile("</table");
	private static final Pattern RE_ACHIVEMENT = Pattern.compile("(\\d+)\\.(\\d)(\\d)?%");
	private static final Pattern RE_HIGHSCORE = Pattern.compile("(\\d+)pts");

	private static ScoreRecord parseScore(Matcher m, String difficulty, ScoreRecord score) throws ParseException {
		if (score == null)
			score = new ScoreRecord();

		Pattern RE_DIFFICULTY = Pattern.compile(Pattern.quote(difficulty)+"</b><br>\\s*★(\\d+)(?:\\.(\\d))?");
		m = m.usePattern(RE_DIFFICULTY);
		if (!m.find())
			return null;
		score.difficulty = Integer.valueOf(m.group(1)) * 10;
		if (m.group(2) != null)
			score.difficulty += Integer.parseInt(m.group(2));

		int end = m.regionEnd();
		int start = m.end();
		m = m.usePattern(RE_BLOCKEND);
		if (!m.find(start))
			throw new ParseException();
		m.region(start, m.start());

		score.clear_status = 5 - findLiteral(m, "clear4.jpg", "clear3.jpg", "clear2.jpg", "clear1.jpg", "-");
		score.trial_status = findLiteral(m, "C-TRIAL", "G-TRIAL", "E-TRIAL", "COMPLETE");
		m = m.usePattern(RE_ACHIVEMENT);
		if (m.find()) {
			score.achievement = m.group(3) == null ? 0 : Integer.valueOf(m.group(3));
			score.achievement += Integer.valueOf(m.group(2)) * 10;
			score.achievement += Integer.valueOf(m.group(1)) * 100;
		}
		m = m.usePattern(RE_HIGHSCORE);
		if (m.find())
			score.high_score = Integer.valueOf(m.group(1));

		m.region(m.end(), end);
		return score;
	}

	public static void parseInfoPage(InputStream content, MusicInfo music) throws ParseException {
		String body = Parser.read(content);
		Pattern RE_COVERART = Pattern.compile(Pattern.quote(music.title) + "<br>\\s*\\[(\\d+)人設定\\]\\s*<br>\\s*<img src=\"(.+?)\"");
		Matcher m = RE_COVERART.matcher(body);
		if (!m.find())
			throw new ParseException();

		CodeMap difficulties = DdN.difficulty();
		music.part = Integer.parseInt(m.group(1));
		music.coverart = m.group(2);
		for (int i = 0; i < difficulties.count(); ++i)
			music.records[i] = parseScore(m, difficulties.name(i), music.records[i]);
	}

	private static final Pattern RE_ROLE = Pattern.compile("\\[(.+?)\\](?:<[^>]+>\\s*)+\\[(?:ボイス|デフォルト)　(.+?)\\](?:<[^>]+>\\s*)+([^<>]+)");
	private static final Pattern RE_ITEM = Pattern.compile("<a href=\".*/customizeItem/selectPart/\\w+/vocal\\d/\\d/[^>]*>(.+)</a>");

	public static void parseRoles(InputStream content, MusicInfo music, DdNIndex index) throws ParseException {
		String body = Parser.read(content);
		MatchHelper m = new MatchHelper(body);
		if ((music.role1 = parseRole(m, index)) == null)
			throw new ParseException();
		if ((music.role2 = parseRole(m, index)) == null)
			return;
		if ((music.role3 = parseRole(m, index)) == null)
			return;
	}

	private static Role parseRole(MatchHelper m, DdNIndex index) throws ParseException {
		if (!m.find(RE_ROLE))
			return null;

		Role role = new Role();
		role.name = m.group(1);
		role.cast = index.character().code(m.group(2));
		role.module = index.module().id(m.group(3));
		if (role.module != null) {
			for (int i = 0; i < CustomizeItem.COUNT; ++i)
				role.items[i] = index.customizeItem().id(m.findString(RE_ITEM, 1, null));
		}
		return role;
	}

	static final Pattern RE_RANKING_TITLE = Pattern.compile("<a href=\".*/(\\w+)/rankingList/\\d+\".*?>(.+)</a>");

	public static String parseRankingList(InputStream content, List<Ranking> list) throws ParseException {
		CodeMap difficulties = DdN.difficulty();
		String body = Parser.read(content);
		Matcher m = RE_RANKING_TITLE.matcher(body);
		int last = m.regionEnd();
		for (MatchResult r = m.find() ? m.toMatchResult() : null; r != null; ) {
			String id = r.group(1);
			String title = r.group(2);
			int start = r.end();

			m = m.usePattern(RE_RANKING_TITLE);
			m.region(start, last);
			if (m.find()) {
				r = m.toMatchResult();
				m.region(start, r.start());
			}
			else {
				r = null;
			}

			for (int rank = difficulties.count()-1; rank > 1; --rank) {
				Ranking entry = parseRankIn(m, difficulties.name(rank));
				if (entry != null) {
					entry.id = id;
					entry.title = title;
					entry.rank = rank;
					list.add(entry);
				}
			}
		}

		m = m.usePattern(Parser.RE_NEXT);
		return m.find() ? m.group(1) : null;
	}

	private static final Pattern RE_RANKIN_SCORE = Pattern.compile(">(\\d+)</");
	private static final Pattern RE_RANKIN_DATE = Pattern.compile(">(\\d+/\\d+/\\d+)</");
	private static final Pattern RE_RANKING = Pattern.compile(">(\\d+)位</");
	private static final SimpleDateFormat RANKING_DATE = new SimpleDateFormat("yy/MM/dd");

	private static String find(Matcher m, Pattern pattern, int group) throws ParseException {
		int from = m.end();
		m.usePattern(pattern);
		if (!m.find(from))
			throw new ParseException();
		return m.group(group);
	}

	private static Ranking parseRankIn(Matcher m, String difficulty) throws ParseException {
		Pattern RE_DIFFICULTY = Pattern.compile(Pattern.quote(difficulty));
		m = m.usePattern(RE_DIFFICULTY);
		if (!m.find())
			return null;
		Ranking entry = new Ranking();
		try {
			entry.score = Integer.valueOf(find(m, RE_RANKIN_SCORE, 1));
			entry.date = RANKING_DATE.parse(find(m, RE_RANKIN_DATE, 1)).getTime();
			entry.ranking = Integer.valueOf(find(m, RE_RANKING, 1));
		}
		catch (ParseException e) {
			throw e;
		}
		catch (Exception e) {
			throw new ParseException(e);
		}
		return entry;
	}

	private final static Pattern RE_HISTORY = Pattern.compile("<font color=\"#00FFFF\">\\[(.+)\\]</font>\\s*<br>\\s*<a href=\"/divanet/pv/info/(\\w+)/");
	private static final SimpleDateFormat HISTORY_DATE = new SimpleDateFormat("yy/MM/dd HH:mm");

	public static String parsePlayHistory(InputStream content, List<String> ids, long[] params)
			throws ParseException {
		String body = Parser.read(content);
		Matcher m = RE_HISTORY.matcher(body);
		try {
			while (m.find()) {
				long playTime = HISTORY_DATE.parse(m.group(1)).getTime();
				if (playTime <= params[0])
					return null;
				if (playTime > params[1])
					params[1] = playTime;
				final String id = m.group(2);
				if (!ids.contains(id))
					ids.add(id);
			}
		}
		catch (Exception e) {
			throw new ParseException(e);
		}

		m = m.usePattern(Parser.RE_NEXT);
		return m.find() ? m.group(1) : null;
	}
}
