package net.diva.browser.page;
import java.util.List;

import net.diva.browser.DdN;
import net.diva.browser.R;
import net.diva.browser.model.MusicInfo;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;


public class AllMusicFragment extends MusicListFragment {
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setHasOptionsMenu(true);
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		inflater.inflate(R.menu.allmusic_options, menu);
		inflater.inflate(R.menu.main_options, menu);
	}

	@Override
	public void onPrepareOptionsMenu(Menu menu) {
		boolean update = DdN.isAllowUpdateMusics(m_preferences);
		boolean selection = isSelectionMode();
		menu.findItem(R.id.item_update_all).setVisible(update && !selection);
		menu.findItem(R.id.item_update_bulk).setVisible(update && selection);
		menu.findItem(R.id.item_update_new).setVisible(!update);
	}

	protected List<MusicInfo> getMusics() {
		return DdN.getPlayRecord().musics;
	}
}