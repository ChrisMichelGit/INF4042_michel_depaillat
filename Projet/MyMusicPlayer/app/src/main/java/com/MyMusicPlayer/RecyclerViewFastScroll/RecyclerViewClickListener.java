package com.MyMusicPlayer.RecyclerViewFastScroll;


import android.view.View;

import com.MyMusicPlayer.Album.Album;
import com.MyMusicPlayer.Song.Song;

public interface RecyclerViewClickListener
{
    void recyclerViewListClickedSong(View v, Song song, int position);
    void recyclerViewListClickedAlbum(View v, Album album);
}
