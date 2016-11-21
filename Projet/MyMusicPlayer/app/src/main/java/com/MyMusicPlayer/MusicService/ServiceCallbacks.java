package com.MyMusicPlayer.MusicService;


import com.MyMusicPlayer.Song.Song;

public interface ServiceCallbacks
{
    Song getNextSong();
    Song getPreviousSong();
}
