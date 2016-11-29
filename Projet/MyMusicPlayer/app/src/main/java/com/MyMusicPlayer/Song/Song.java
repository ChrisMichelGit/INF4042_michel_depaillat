package com.MyMusicPlayer.Song;

import android.graphics.Bitmap;
import android.os.Parcel;
import android.os.Parcelable;

import com.MyMusicPlayer.Activity.MainActivity;
import com.MyMusicPlayer.Album.Album;
import com.MyMusicPlayer.Utilities.MusicUtils;


public class Song implements Parcelable
{

    ////////////////
    // Attributes //
    ////////////////

    private Album albumRef;         // The reference to the album in memory
    private String artist;          // The artist of the song
    private String album;           // The album of the song
    private String title;           // The title of the song
    private String data;            // The data
    private long albumId;           // The album ID
    private long songId;            // The song ID
    private int duration;           // Duration of the song
    private Bitmap albumArt;        // Album cover
    private MainActivity activity;  // A reference to the MainActivity


    //////////////////
    // Constructors //
    //////////////////

    public Song(String p_artist, String p_album, String p_title, String p_data, long p_albumId, long p_songId, int p_duration, MainActivity p_activity)
    {
        artist = p_artist;
        album = p_album;
        title = p_title;
        data = p_data;
        albumId = p_albumId;
        songId = p_songId;
        duration = p_duration;
        albumArt = MusicUtils.getDefaultArtwork();
        activity = p_activity;
    }

    private Song(Parcel parcel)
    {
        artist = parcel.readString();
        album = parcel.readString();
        title = parcel.readString();
        data = parcel.readString();
        songId = parcel.readLong();
        albumId = parcel.readLong();
        duration = parcel.readInt();
        albumRef = activity.getAlbumList().get((int)getAlbumID());
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
        parcel.writeString(album);
        parcel.writeString(title);
        parcel.writeString(data);
        parcel.writeLong(songId);
        parcel.writeLong(albumId);
        parcel.writeInt(duration);
    }

    /////////////
    // Methods //
    /////////////

    // Construct a Song from a parcelable
    public static final Parcelable.Creator<Song> CREATOR = new Parcelable.Creator<Song>()
    {
        public Song createFromParcel(Parcel in)
        {
            return new Song(in);
        }

        @Override
        public Song[] newArray(int i)
        {
            return new Song[0];
        }
    };


    /////////////
    // Setters //
    /////////////

    public void setActivity(MainActivity p_activity)
    {
        activity = p_activity;
    }

    public void initBitmap()
    {
        if (activity.getBitmapFromMemCache(album) == null)
        {
            // Create the bitmap
            albumArt = MusicUtils.getArtwork(activity.getApplicationContext(), songId, albumId);
            albumArt = Bitmap.createScaledBitmap(albumArt, 200, 200, true);

            // Add the album cover to the cache
            activity.addBitmapToMemoryCache(album, albumArt);
        }

        else
        {
            albumArt = activity.getBitmapFromMemCache(album);
        }
        albumRef.setBitmap(albumArt);
    }

    public void setBitmap(Bitmap p_albumArt)
    {
        albumArt = p_albumArt;

        // Add the album cover to the cache
        activity.addBitmapToMemoryCache(album, albumArt);
    }

    public void setAlbumRef (Album ref)
    {
        albumRef = ref;
    }


    /////////////
    // Getters //
    /////////////

    public String getArtist()
    {
        return artist;
    }

    public String getAlbum()
    {
        return album;
    }

    public String getTitle()
    {
        return title;
    }

    public String getData()
    {
        return data;
    }

    public long getAlbumID()
    {
        return albumId;
    }

    long getSongId()
    {
        return songId;
    }

    public int getDuration()
    {
        return duration;
    }

    public Bitmap getBitmap()
    {
        return albumArt;
    }

    public Album getAlbumRef() { return albumRef; }

}
