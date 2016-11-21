package com.MyMusicPlayer.Song;

import android.graphics.Typeface;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.MyMusicPlayer.Activity.MainActivity;
import com.MyMusicPlayer.R;
import com.MyMusicPlayer.RecyclerViewFastScroll.FastScrollRecyclerView;

public class TabSongFragment extends Fragment
{

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.activity_main, container, false);

        MainActivity ma = (MainActivity) getActivity();

        FastScrollRecyclerView songView;

        // Set the recycler view and its properties
        songView = (FastScrollRecyclerView) rootView.findViewById(R.id.song_list);
        songView.getFastScroller().getPopup().setAlpha(0);
        songView.setAutoHideDelay(1000);
        songView.setPopupBgColor(ma.getColor(R.color.grey));
        songView.setPopupTextColor(ma.getColor(R.color.white));
        songView.setPopUpTypeface(Typeface.SANS_SERIF);
        songView.setPopupTextSize(100);
        songView.setTrackColor(ma.getColor(R.color.alpha_grey));
        songView.setThumbColor(ma.getColor(R.color.grey));

        // Set the adapter for the view
        SongAdapter songAdt = new SongAdapter(ma.getSongList(), R.layout.song_tab, ma);
        songView.setAdapter(songAdt);

        LinearLayoutManager lm = new LinearLayoutManager(getActivity());
        songView.setLayoutManager(lm);
        songView.scrollToPosition(lm.findFirstCompletelyVisibleItemPosition());

        return rootView;
    }
}
