package com.MyMusicPlayer.Album;


import android.graphics.Bitmap;
import android.os.Parcel;
import android.os.Parcelable;

import com.MyMusicPlayer.Activity.MainActivity;
import com.MyMusicPlayer.Utilities.MusicUtils;
import com.MyMusicPlayer.Song.Song;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class Album implements Parcelable
{
    ////////////////
    // Attributes //
    ////////////////

    private ArrayList<Song> songAlbum;  // The songs of the album
    private String artist;              // The artist of the album
    private String title;               // The title of the album
    private long albumId;               // The album ID
    private long artistId;              // The artist ID
    private int year;                   // The year of the album
    private int duration;               // Duration of the album
    private Bitmap albumArt;            // Album cover
    private MainActivity activity;      // A reference to the MainActivity


    //////////////////
    // Constructors //
    //////////////////

    public Album(String p_artist, String p_title, long p_albumId, long p_artistId, int p_year, MainActivity p_activity)
    {
        artist = p_artist;
        title = p_title;
        albumId = p_albumId;
        artistId = p_artistId;
        year = p_year;
        duration = 0;
        albumArt = MusicUtils.getDefaultArtwork();
        activity = p_activity;
        songAlbum = new ArrayList<>();
    }

    private Album(Parcel parcel)
    {
        songAlbum = parcel.readArrayList(Song.class.getClassLoader());
        artist = parcel.readString();
        title = parcel.readString();
        albumId = parcel.readLong();
        duration = parcel.readInt();
    }


    ////////////////////////
    // Overridden Methods //
    ////////////////////////

    // Parcelable methods //

    @Override
    public int describeContents()
    {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i)
    {
        // Memorize every info
        parcel.writeString(artist);
        parcel.writeString(title);
        parcel.writeLong(albumId);
        parcel.writeInt(duration);
    }


    /////////////
    // Methods //
    /////////////

    // Construct a Song from a parcelable
    public static final Parcelable.Creator<Album> CREATOR = new Parcelable.Creator<Album>()
    {
        public Album createFromParcel(Parcel in)
        {
            return new Album(in);
        }

        @Override
        public Album[] newArray(int i)
        {
            return new Album[0];
        }
    };

    // Add a song to the album
    public void addSongToAlbum (Song song)
    {
        songAlbum.add(song);
        duration += song.getDuration();
        setAlbumArt(song.getBitmap());
        sortSongsByTrack();
    }

    private void sortSongsByTrack ()
    {
        Collections.sort(songAlbum, new Comparator<Song>()
        {
            public int compare(Song a, Song b)
            {
                if (a.getTrackNumber() < b.getTrackNumber()) return -1;
                else if (a.getTrackNumber() > b.getTrackNumber()) return 1;
                else return 0;
            }
        });
    }

    /////////////
    // Setters //
    /////////////

    public void setActivity(MainActivity p_activity)
    {
        activity = p_activity;
    }

    public void setBitmap(Bitmap p_albumArt)
    {
        albumArt = p_albumArt;
    }

    public void setYear (int p_year) { year = p_year; }

    private void setAlbumArt(Bitmap p_albumArt)
    {
        if (p_albumArt != null && (albumArt == null || albumArt.sameAs(MusicUtils.getDefaultArtwork())))
        {
            // Create the bitmap
            albumArt = Bitmap.createScaledBitmap(p_albumArt, 200, 200, true);

            // Add the album cover to the cache
            activity.addBitmapToMemoryCache(title + albumId, albumArt);
        }
    }


    /////////////
    // Getters //
    /////////////

    String getArtist()
    {
        return artist;
    }

    public String getTitle()
    {
        return title;
    }

    public long getAlbumID()
    {
        return albumId;
    }

    public int getDuration()
    {
        return duration;
    }

    public Bitmap getBitmap()
    {
        return albumArt;
    }

    MainActivity getActivity() { return activity; }

    public ArrayList<Song> getSongList ()
    {
        return songAlbum;
    }

    public long getArtistId()
    {
        return artistId;
    }

    public int getYear()
    {
        return year;
    }
}
