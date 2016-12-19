package com.MyMusicPlayer.RecyclerViewFastScroll;


import android.view.View;

import com.MyMusicPlayer.Album.Album;
import com.MyMusicPlayer.Artist.Artist;
import com.MyMusicPlayer.Song.Song;

public interface RecyclerViewClickListener
{
    void recyclerViewListClickedSong(View v, Song song, int position);
    void recyclerViewListClickedAlbum(View v, Album album);
    void recyclerViewListClickedArtist(View v, Artist artist);
    void recyclerViewListClickedPopup (View v, Song correspondingSong);
}
