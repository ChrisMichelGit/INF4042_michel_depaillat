package com.MyMusicPlayer;

import android.graphics.Bitmap;
import android.os.Parcel;
import android.os.Parcelable;


class Song extends Thread implements Parcelable {

    ////////////////
    // Attributes //
    ////////////////

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

    Song(String p_artist, String p_album, String p_title, String p_data, long p_albumId, long p_songId, int p_duration, MainActivity p_activity) {
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

    private Song (Parcel parcel) {
        artist = parcel.readString();
        album = parcel.readString();
        title = parcel.readString();
        data = parcel.readString();
        songId = parcel.readLong();
        albumId = parcel.readLong();
        duration = parcel.readInt();
    }

    ////////////////////////
    // Overridden Methods //
    ////////////////////////

    // Thread methods //

    @Override
    public void run() {

        // Create the bitmap
        albumArt = MusicUtils.getArtwork (activity.getApplicationContext(), songId, albumId);
        albumArt = Bitmap.createScaledBitmap(albumArt, 200, 200, true);

        // Add the album cover to the cache
        activity.addBitmapToMemoryCache(title + albumId, albumArt);
    }

    // Parcelable methods //

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {

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
    public static final Parcelable.Creator<Song> CREATOR = new Parcelable.Creator<Song>() {
        public Song createFromParcel(Parcel in) {
            return new Song(in);
        }

        @Override
        public Song[] newArray(int i) {
            return new Song[0];
        }
    };


    /////////////
    // Setters //
    /////////////

    void setActivity (MainActivity p_activity)
    {
        activity = p_activity;
    }

    void setBitmap (Bitmap p_albumArt)
    {
        albumArt = p_albumArt;
    }

    /////////////
    // Getters //
    /////////////

    String getArtist(){return artist;}
    String getAlbum(){return album;}
    String getTitle(){return title;}
    String getData(){return data;}
    long getAlbumID(){return albumId;}
    long getSongId(){return songId;}
    public int getDuration(){return duration;}
    Bitmap getBitmap(){return  albumArt;}

    // Convert duration into a string: hour, min, sec
    String getDurationToString ()
    {
        String convertedDuration = "";

        int hours = 0, mins = 0, secs = 0;

        secs = duration / 1000;
        mins  = secs / 60;
        secs = secs - (mins * 60);
        hours = mins / 60;
        mins  = mins - (hours * 60);

        if (hours > 0)
        {
            if (hours / 10 == 0) convertedDuration += "0" + hours + ":";
            else convertedDuration += hours + ":";
        }

        if (mins / 10 == 0) convertedDuration += "0" + mins + ":";
        else convertedDuration += mins + ":";

        if (secs / 10 == 0) convertedDuration += "0" + secs;
        else convertedDuration += secs;


        return convertedDuration;
    }

}
