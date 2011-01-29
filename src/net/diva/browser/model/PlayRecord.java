package net.diva.browser.model;

import java.util.List;

public class PlayRecord {
	public String player_name;
	public String level;
	public String title_id;
	public List<MusicInfo> musics;

	public MusicInfo getMusic(String id) {
		for (MusicInfo music: musics) {
			if (music.id.equals(id))
				return music;
		}
		return null;
	}
}