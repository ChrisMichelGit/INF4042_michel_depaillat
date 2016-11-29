package com.MyMusicPlayer.Album;

import android.annotation.TargetApi;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.MyMusicPlayer.Activity.MainActivity;
import com.MyMusicPlayer.R;
import com.MyMusicPlayer.RecyclerViewFastScroll.FastScrollRecyclerView;

public class TabAlbumFragment extends Fragment
{

    @TargetApi(Build.VERSION_CODES.M)
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.activity_main, container, false);

        MainActivity ma = (MainActivity) getActivity();

        FastScrollRecyclerView albumView;

        // Set the recycler view and its properties
        albumView = (FastScrollRecyclerView) rootView.findViewById(R.id.song_list);
        albumView.getFastScroller().getPopup().setAlpha(0);
        albumView.setAutoHideDelay(1000);
        albumView.setPopupBgColor(ma.getColor(R.color.grey));
        albumView.setPopupTextColor(ma.getColor(R.color.white));
        albumView.setPopUpTypeface(Typeface.SANS_SERIF);
        albumView.setPopupTextSize(100);
        albumView.setTrackColor(ma.getColor(R.color.alpha_grey));
        albumView.setThumbColor(ma.getColor(R.color.grey));

        // Set the adapter for the view
        AlbumAdapter albumAdt = new AlbumAdapter(ma.getAlbumList(), R.layout.album_tab, ma);
        albumView.setAdapter(albumAdt);

        LinearLayoutManager lm = new LinearLayoutManager(getActivity());
        albumView.setLayoutManager(lm);
        albumView.scrollToPosition(lm.findFirstCompletelyVisibleItemPosition());

        return rootView;
    }
}
