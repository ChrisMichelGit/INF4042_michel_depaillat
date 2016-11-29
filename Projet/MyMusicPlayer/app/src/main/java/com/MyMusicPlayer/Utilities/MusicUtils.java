package com.MyMusicPlayer.Utilities;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.support.annotation.Nullable;

import com.MyMusicPlayer.R;

import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;


public class MusicUtils
{

    ////////////////
    // Attributes //
    ////////////////

    private static final BitmapFactory.Options sBitmapOptions = new BitmapFactory.Options();        // The bitmap options
    private static final Uri sArtworkUri = Uri.parse("content://media/external/audio/albumart");    // The default path to album cover
    private static Bitmap defaultBM;                                                                // The default album cover


    /////////////
    // Methods //
    /////////////


    /////////////
    // Setters //
    /////////////

    static
    {

        sBitmapOptions.inPreferredConfig = Bitmap.Config.RGB_565;
        sBitmapOptions.inDither = false;
    }

    public static void setDefaultBM(Context context)
    {
        defaultBM = BitmapFactory.decodeResource(context.getResources(), R.drawable.albumart_mp_unknown);
        defaultBM = Bitmap.createScaledBitmap(defaultBM, 200, 200, true);
    }


    /////////////
    // Getters //
    /////////////

    // Get album art for specified album. You should not pass in the album id for the "unknown" album here (use -1 instead)
    // This method always returns the default album art icon when no album art is found.
    public static Bitmap getArtwork(Context context, long song_id, long album_id)
    {
        return getArtwork(context, song_id, album_id, true);
    }

    // Get album art for specified album. You should not pass in the album id for the "unknown" album here (use -1 instead)
    @Nullable
    private static Bitmap getArtwork(Context context, long song_id, long album_id,
                                     boolean allowDefault)
    {

        if (album_id < 0)
        {

            // This is something that is not in the database, so get the album art directly from the file.
            if (song_id >= 0)
            {
                Bitmap bm = getArtworkFromFile(context, song_id, -1);
                if (bm != null)
                {
                    return bm;
                }
            }
            if (allowDefault)
            {
                return getDefaultArtwork();
            }
            return null;
        }

        // Get the album art from the album
        ContentResolver res = context.getContentResolver();
        Uri uri = ContentUris.withAppendedId(sArtworkUri, album_id);

        if (uri != null)
        {
            InputStream in = null;
            try
            {
                in = res.openInputStream(uri);
                return BitmapFactory.decodeStream(in, null, sBitmapOptions);
            }
            catch (FileNotFoundException ex)
            {

                // The album art thumbnail does not actually exist. Maybe the user deleted it, or maybe it never existed to begin with.
                Bitmap bm = getArtworkFromFile(context, song_id, album_id);
                if (bm != null)
                {
                    if (bm.getConfig() == null)
                    {
                        bm = bm.copy(Bitmap.Config.RGB_565, false);
                        if (bm == null && allowDefault)
                        {
                            return getDefaultArtwork();
                        }
                    }
                }
                else if (allowDefault)
                {
                    bm = getDefaultArtwork();
                }
                return bm;
            }
            finally
            {
                try
                {
                    if (in != null)
                    {
                        in.close();
                    }
                }
                catch (IOException exception)
                {
                    exception.printStackTrace();
                }
            }
        }

        return null;
    }

    // Get album art for specified file
    private static Bitmap getArtworkFromFile(Context context, long songId, long albumId)
    {

        if (albumId < 0 && songId < 0)
        {
            throw new IllegalArgumentException("Must specify an album or a song_tab id");
        }

        try
        {
            Uri uri = Uri.parse("content://media/external/audio/media/" + songId + "/albumart");
            ParcelFileDescriptor pfd = context.getContentResolver().openFileDescriptor(uri, "r");
            if (pfd != null)
            {
                FileDescriptor fd = pfd.getFileDescriptor();
                return BitmapFactory.decodeFileDescriptor(fd);
            }
        }
        catch (IllegalStateException | FileNotFoundException exception)
        {
            exception.printStackTrace();
        }
        return getDefaultArtwork();
    }

    // Get the default artwork
    public static Bitmap getDefaultArtwork()
    {
        return defaultBM;
    }

    // Convert duration into a string: hour, min, sec
    public static String getDurationToString(int duration)
    {
        String convertedDuration = "";

        int hours, mins, secs;

        secs = duration / 1000;
        mins = secs / 60;
        secs = secs - (mins * 60);
        hours = mins / 60;
        mins = mins - (hours * 60);

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