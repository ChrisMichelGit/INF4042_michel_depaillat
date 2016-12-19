package com.MyMusicPlayer.Utilities;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;

import com.MyMusicPlayer.Album.TabAlbumFragment;
import com.MyMusicPlayer.Artist.TabArtistFragment;
import com.MyMusicPlayer.Song.TabSongFragment;

public class PageAdapter extends FragmentStatePagerAdapter {
    private int mNumOfTabs;
    private Fragment fragTab;

    public PageAdapter(FragmentManager fm, int NumOfTabs) {
        super(fm);
        this.mNumOfTabs = NumOfTabs;
    }

    @Override
    public Fragment getItem(int position) {

        switch (position) {
            case 0:
                fragTab = new TabSongFragment();
                return fragTab;
            case 1:
                fragTab = new TabAlbumFragment();
                return fragTab;
            case 2:
                fragTab = new TabArtistFragment();
                return fragTab;
            default:
                return fragTab;
        }
    }

    @Override
    public int getCount() {
        return mNumOfTabs;
    }
}