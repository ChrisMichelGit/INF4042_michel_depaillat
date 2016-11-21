package com.MyMusicPlayer;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.os.Build;
import android.os.IBinder;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.util.LruCache;
import android.os.Bundle;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import android.net.Uri;
import android.content.ContentResolver;
import android.database.Cursor;
import android.support.v7.widget.LinearLayoutManager;
import android.view.View;
import android.util.Log;


@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
class MainActivity extends Activity implements RecyclerViewClickListener, ServiceCallbacks
{

    ////////////////
    // Attributes //
    ////////////////

    private String TAG = "MainActivity";                                                  // The log tag

    private ArrayList<Song> songList;                                                   // The list containing all the song of the user
    private FastScrollRecyclerView songView;                                            // The view displaying the song list
    private int currSongPlayingID = -1;                                                 // The ID of the actual played song
    private MusicPlayerService player;                                                  // The music service that manages every actions on audio
    boolean serviceBound = false;                                                       // To know if the above service is active or not

    private String[] permissionsList = {Manifest.permission.READ_EXTERNAL_STORAGE,
                                        Manifest.permission.READ_PHONE_STATE     };     // The list of needed permissions
    private int[] requestCode = {1, 2};                                                 // The request code for the permission (each code correspond to the permission at the same index of permissionList)

    private LruCache<String, Bitmap> mMemoryCache;                                      // The cache containing all album cover
    private int coverByThread = 200;                                                    // The number of all album cover a thread should searching for (recommended to set here)
    private int numThread;                                                              // The number of thread necessary to load all album cover (do not set it here, it will be overridden)
    private int lastThread;                                                             // The number of album covers the last thread should searching for (do not set it here, it will be overridden)
    private int currThreadId;                                                           // The ID of the current thread
    static Song firstSong;                                                              // Used by the MusicPlayerService on creation

    //Binding this Client to the MusicPlayer Service
    private ServiceConnection serviceConnection = new ServiceConnection()
    {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service)
        {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            MusicPlayerService.LocalBinder binder = (MusicPlayerService.LocalBinder) service;
            player = binder.getService();

            // Link this interface to the service
            player.setCallbacks(MainActivity.this);
            serviceBound = true;

            // Unbind in order to enable the onTaskRemove function of MusicPlayerService
            unbindService(serviceConnection);
        }

        @Override
        public void onServiceDisconnected(ComponentName name)
        {
        }
    };

    ////////////////////////
    // Overridden Methods //
    ////////////////////////

    // Main methods //

    @TargetApi(Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        RetainFragment retainFragment = RetainFragment.findOrCreateRetainFragment(getFragmentManager());

        // Set the recycler view and its properties
        songView = (FastScrollRecyclerView) findViewById(R.id.song_list);
        songView.getFastScroller().getPopup().setAlpha(0);
        songView.setAutoHideDelay(1000);
        songView.setPopupBgColor(getColor(R.color.grey));
        songView.setPopupTextColor(getColor(R.color.white));
        songView.setPopUpTypeface(Typeface.SANS_SERIF);
        songView.setPopupTextSize(100);
        songView.setTrackColor(getColor(R.color.alpha_grey));
        songView.setThumbColor(getColor(R.color.grey));

        songList = new ArrayList<>();

        // Check the android version and ask permission in Run-Time mode if it's 6.0 or higher
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1)
            askForPermissions(permissionsList, requestCode);

        MusicUtils.setDefaultBM(this.getApplicationContext());

        //Intent playerIntent = new Intent(this, MusicPlayerService.class);
        //if (!serviceBound) bindService(playerIntent, serviceConnection, Context.BIND_AUTO_CREATE);

        // Check if the song list has been already created
        // If yes it will save a lot of time and resources when rotate screen
        if (savedInstanceState == null || !savedInstanceState.containsKey("songList"))
        { // No saved instance found

            if (ActivityCompat.checkSelfPermission(MainActivity.this, permissionsList[0]) == PackageManager.PERMISSION_GRANTED)
            {
                getSongList();
            }

            // Get max available VM memory, exceeding this amount will throw an OutOfMemory exception. Stored in kilobytes as LruCache takes an int in its constructor.
            final int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);

            mMemoryCache = new LruCache<>(maxMemory);

            // Get all the album cover (more songs, longer it takes)
            getAllAlbumCover();

            // Set the cache
            retainFragment.mRetainedCache = mMemoryCache;

            // Start the music service
            //startService(playerIntent);
        }
        else
        { // Saved instance found
            mMemoryCache = retainFragment.mRetainedCache;
            //currSongPlayingID = retainFragment.currSongPlayingID;
            songList = savedInstanceState.getParcelableArrayList("songList");
            if (songList != null)
            {
                for (Song song : songList)
                {
                    Log.i(TAG, song.getTitle());
                    song.setActivity(this);
                    song.setBitmap(getBitmapFromMemCache(song.getTitle() + song.getAlbumID()));
                }
            }
        }

        // Set the adapter for the view
        SongAdapter songAdt = new SongAdapter(songList, R.layout.song, this);
        songView.setAdapter(songAdt);

        LinearLayoutManager lm = new LinearLayoutManager(this);
        songView.setLayoutManager(lm);
        songView.scrollToPosition(lm.findFirstCompletelyVisibleItemPosition());

    }

    @Override
    protected void onSaveInstanceState(Bundle outState)
    {
        outState.putParcelableArrayList("songList", songList);
        outState.putBoolean("ServiceState", serviceBound);
        outState.putInt("currSongPlayingID", currSongPlayingID);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState)
    {
        super.onRestoreInstanceState(savedInstanceState);
        songList = savedInstanceState.getParcelableArrayList("songList");
        serviceBound = savedInstanceState.getBoolean("ServiceState");
        currSongPlayingID = savedInstanceState.getInt("currSongPlayingID");
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();
        if (isFinishing())
        {
            Log.d(TAG, "DESTROY");
        }
    }

    // AppCompatActivity methods //

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults)
    {
        if (requestCode == this.requestCode[0])
        {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
            {
                Log.d(TAG, "Permission has been granted");
                getSongList();
                getAllAlbumCover();
                songView.getAdapter().notifyDataSetChanged();
            }
            else
            {
                Log.d(TAG, "Permission has been denied or request cancelled");
            }
        }
    }

    // RecyclerViewClickListener methods //

    @Override
    public void recyclerViewListClicked(View v, int position)
    {
        playSong(position);
    }

    // ServiceCallbacks methods //

    @Override
    public Song getNextSong()
    {
        if (currSongPlayingID == songList.size() - 1)
        {
            currSongPlayingID = 0;
            return songList.get(0);
        }
        else
        {
            currSongPlayingID++;
            return songList.get(currSongPlayingID);
        }
    }

    @Override
    public Song getPreviousSong()
    {
        if (currSongPlayingID == 0)
        {
            currSongPlayingID = songList.size()-1;
            return songList.get(currSongPlayingID);
        }
        else
        {
            currSongPlayingID--;
            return songList.get(currSongPlayingID);
        }
    }


    /////////////
    // Methods //
    /////////////

    // Ask the needed permissions to the user (Only on Android 6.0 and higher)
    private void askForPermissions(String[] permission, int[] requestCode)
    {
        for (int i = 0; i < permission.length; i++)
        {
            if (ActivityCompat.checkSelfPermission(MainActivity.this, permission[i]) != PackageManager.PERMISSION_GRANTED)
            {
                // Should we show an explanation?
                if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this, permission[i]))
                {
                    // This is called if user has denied the permission before
                    // In this case I am just asking the permission again
                    ActivityCompat.requestPermissions(MainActivity.this, permission, requestCode[i]);
                }
                else
                {
                    ActivityCompat.requestPermissions(MainActivity.this, permission, requestCode[i]);
                }
            }
            else
            {
                Log.d(TAG, "" + permission[i] + " is already granted.");
            }
        }
    }

    // Add the bitmap to the cache
    public void addBitmapToMemoryCache(String key, Bitmap bitmap)
    {
        if (getBitmapFromMemCache(key) == null && bitmap != null)
        {
            Log.i(TAG, "Bitmap Added to cache at key: " + key);
            mMemoryCache.put(key, bitmap);
        }
    }

    public void playSong(int position)
    {
        Log.d(TAG, "Play song");
        Song song = songList.get(position);

        //Check is service is active
        if (!serviceBound)
        {
            Intent playerIntent = new Intent(this, MusicPlayerService.class);
            startService(playerIntent);
            firstSong = song;
            serviceBound = true;
            bindService(playerIntent, serviceConnection, Context.BIND_AUTO_CREATE);
        }
        else
        { // Service is active, play a new song
            if (position != currSongPlayingID)
            {
                Log.d(TAG, "PlayNewSong");
                player.playNewSong(song);
            }

        }
        currSongPlayingID = position;
    }


    /////////////
    // Getters //
    /////////////

    // Get the first the song played
    public static Song getCurrentSongPlaying ()
    {
        return firstSong;
    }

    // Get the specified bitmap from the cache
    public Bitmap getBitmapFromMemCache(String key)
    {
        if (mMemoryCache.get(key) != null) Log.d(TAG, "Get bitmap from cache at key: " + key);
        return mMemoryCache.get(key);
    }

    // Get all the songs that the user has
    public void getSongList()
    {
        ContentResolver musicResolver = getContentResolver();
        Uri musicUri = android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        Cursor musicCursor = musicResolver.query(musicUri, null, MediaStore.Audio.Media.IS_MUSIC + "!= 0", null, null);

        if (musicCursor != null && musicCursor.moveToFirst())
        {
            // Add songs to list
            do
            {
                // Get all the important elements
                String artist = musicCursor.getString(musicCursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST));
                String album = musicCursor.getString(musicCursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM));
                String title = musicCursor.getString(musicCursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE));
                String data = musicCursor.getString(musicCursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA));
                long albumId = musicCursor.getLong(musicCursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID));
                long songId = musicCursor.getLong(musicCursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID));
                int duration = musicCursor.getInt(musicCursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION));

                Song song = new Song(artist, album, title, data, albumId, songId, duration, this);

                // Store the song
                songList.add(song);


            } while (musicCursor.moveToNext());
            musicCursor.close();
        }

        // Sort the list by alphabetical order
        Collections.sort(songList, new Comparator<Song>()
        {
            public int compare(Song a, Song b)
            {
                return a.getTitle().compareTo(b.getTitle());
            }
        });
    }

    // Get all the album cover (more songs, longer it takes)
    void getAllAlbumCover()
    {
        // We determine the number of threads by the number total of songs divided by the number of album covers that a thread should searching for
        numThread = songList.size() / coverByThread;

        // If the rest is not 0 we need another thread to complete the missing album cover
        lastThread = songList.size() % coverByThread;

        // All the thread except the last one
        for (currThreadId = 0; currThreadId < numThread; currThreadId++)
        {

            // Use a thread got the advantage to not block the display of loaded song
            new Thread()
            {

                int threadId = currThreadId;

                @Override
                public void run()
                {
                    for (int i = numThread * threadId * 50; i < numThread * threadId * 50 + coverByThread; i++)
                    {
                        Log.i(TAG, "Thread n°" + threadId + " id: " + i);
                        songList.get(i).initBitmap();
                    }
                }
            }.start();
        }

        // The last thread
        new Thread()
        {
            @Override
            public void run()
            {
                for (int i = numThread * currThreadId * 50; i < numThread * currThreadId * 50 + lastThread; i++)
                {
                    Log.i(TAG, "Thread n°" + currThreadId + " id: " + i);
                    songList.get(i).initBitmap();
                }
            }
        }.start();
    }


    /////////////////
    // Other class //
    /////////////////

    // A fragment that memorize the album cover
    public static class RetainFragment extends Fragment
    {
        private static final String TAG = "RetainFragment";
        public LruCache<String, Bitmap> mRetainedCache;

        public RetainFragment()
        {
        }

        public static RetainFragment findOrCreateRetainFragment(FragmentManager fm)
        {
            RetainFragment fragment = (RetainFragment) fm.findFragmentByTag(TAG);
            if (fragment == null)
            {
                fragment = new RetainFragment();
                fm.beginTransaction().add(fragment, TAG).commit();
            }
            return fragment;
        }

        @Override
        public void onCreate(Bundle savedInstanceState)
        {
            super.onCreate(savedInstanceState);
            setRetainInstance(true);
        }
    }
}

