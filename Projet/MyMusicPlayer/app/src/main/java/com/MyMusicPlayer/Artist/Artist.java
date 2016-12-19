package com.MyMusicPlayer.Artist;


import android.graphics.Bitmap;
import android.os.Parcel;
import android.os.Parcelable;

import com.MyMusicPlayer.Activity.MainActivity;
import com.MyMusicPlayer.Album.Album;
import com.MyMusicPlayer.Song.Song;
import com.MyMusicPlayer.Utilities.MusicUtils;

import java.util.ArrayList;

public class Artist implements Parcelable
{
    ////////////////
    // Attributes //
    ////////////////

    private ArrayList<Album> albumArtist;   // The albums of the artist
    private String artist;                  // The name artist
    private long albumId;                   // The album ID
    private long artistId;                  // The artist ID
    private Bitmap albumArt;                // Album cover
    private MainActivity activity;          // A reference to the MainActivity


    //////////////////
    // Constructors //
    //////////////////

    public Artist(String p_artist, long p_albumId, long p_artistId, MainActivity p_activity)
    {
        artist = p_artist;
        albumId = p_albumId;
        artistId = p_artistId;
        albumArt = MusicUtils.getDefaultArtwork();
        activity = p_activity;
        albumArtist = new ArrayList<>();
    }

    private Artist(Parcel parcel)
    {
        albumArtist = parcel.readArrayList(Album.class.getClassLoader());
        artist = parcel.readString();
        albumId = parcel.readLong();
        artistId = parcel.readLong();
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
        parcel.writeLong(albumId);
        parcel.writeLong(artistId);
    }


    /////////////
    // Methods //
    /////////////

    // Construct a Song from a parcelable
    public static final Creator<Artist> CREATOR = new Creator<Artist>()
    {
        public Artist createFromParcel(Parcel in)
        {
            return new Artist(in);
        }

        @Override
        public Artist[] newArray(int i)
        {
            return new Artist[0];
        }
    };

    // Add a song to the album
    public void addAlbumToArtist (Album album)
    {
        albumArtist.add(album);
    }


    /////////////
    // Setters //
    /////////////

    public void setActivity(MainActivity p_activity)
    {
        activity = p_activity;
    }


    /////////////
    // Getters //
    /////////////

    String getArtist()
    {
        return artist;
    }

    public long getAlbumID()
    {
        return albumId;
    }

    public Bitmap getBitmap()
    {
        return albumArt;
    }

    MainActivity getActivity() { return activity; }

    public ArrayList<Album> getAlbumList ()
    {
        return albumArtist;
    }
}
