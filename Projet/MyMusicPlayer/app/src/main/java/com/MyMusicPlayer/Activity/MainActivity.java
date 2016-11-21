package com.MyMusicPlayer.Activity;

import android.Manifest;
import android.annotation.TargetApi;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.design.widget.TabLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.util.LruCache;
import android.os.Bundle;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.PopupMenu;
import android.view.MenuItem;
import android.view.View;
import android.util.Log;
import android.widget.Toast;
import android.widget.Toolbar;

import com.MyMusicPlayer.MusicService.MusicPlayerService;
import com.MyMusicPlayer.MusicService.ServiceCallbacks;
import com.MyMusicPlayer.PageAdapter;
import com.MyMusicPlayer.Song.MusicUtils;
import com.MyMusicPlayer.R;
import com.MyMusicPlayer.RecyclerViewFastScroll.RecyclerViewClickListener;
import com.MyMusicPlayer.Song.Song;


@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
public class MainActivity extends AppCompatActivity implements RecyclerViewClickListener, ServiceCallbacks
{

    ////////////////
    // Attributes //
    ////////////////

    private String TAG = "MainActivity";                                                // The log tag

    private ArrayList<Song> songList;                                                   // The list containing all the song_tab of the user
    private int currSongPlayingID = -1;                                                 // The ID of the actual played song_tab
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
            Log.d(TAG, "Service Connected");

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
            Log.d(TAG, "Service Disconnected");
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

        RetainFragment retainFragment = RetainFragment.findOrCreateRetainFragment(getSupportFragmentManager());

        songList = new ArrayList<>();

        // Check the android version and ask permission in Run-Time mode if it's 6.0 or higher
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1)
            askForPermissions(permissionsList, requestCode);

        MusicUtils.setDefaultBM(this.getApplicationContext());

        // Check if the song_tab list has been already created
        // If yes it will save a lot of time and resources when rotate screen
        if (savedInstanceState == null || !savedInstanceState.containsKey("songList"))
        { // No saved instance found

            if (ActivityCompat.checkSelfPermission(MainActivity.this, permissionsList[0]) == PackageManager.PERMISSION_GRANTED)
            {
                getAllSong();
            }

            // Get max available VM memory, exceeding this amount will throw an OutOfMemory exception. Stored in kilobytes as LruCache takes an int in its constructor.
            final int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);

            mMemoryCache = new LruCache<>(maxMemory);

            // Get all the album cover (more songs, longer it takes)
            getAllAlbumCover();

            // Set the cache
            retainFragment.mRetainedCache = mMemoryCache;
        }
        else
        { // Saved instance found
            mMemoryCache = retainFragment.mRetainedCache;
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

        setContentView(R.layout.tab_layout);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setActionBar(toolbar);

        TabLayout tabLayout = (TabLayout) findViewById(R.id.tab_layout);


        assert tabLayout != null;
        tabLayout.addTab(tabLayout.newTab().setText(R.string.songs));

        final ViewPager viewPager = (ViewPager) findViewById(R.id.pager);
        PageAdapter adapter = new PageAdapter (getSupportFragmentManager(), tabLayout.getTabCount());

        assert viewPager != null;
        viewPager.setAdapter(adapter);

        viewPager.addOnPageChangeListener(new TabLayout.TabLayoutOnPageChangeListener(tabLayout));

        tabLayout.setOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                viewPager.setCurrentItem(tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });
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
                getAllSong();
                getAllAlbumCover();
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
        Log.d(TAG, "Play song_tab");
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
        { // Service is active, play a new song_tab
            if (position != currSongPlayingID)
            {
                Log.d(TAG, "PlayNewSong");
                player.playNewSong(song);
            }

        }
        currSongPlayingID = position;
    }

    public void openPopUpMenu (View v)
    {
        //Creating the instance of PopupMenu
        PopupMenu popup = new PopupMenu(MainActivity.this, v);
        //Inflating the Popup using xml file
        popup.getMenuInflater().inflate(R.menu.pop_up_menu, popup.getMenu());

        //registering popup with OnMenuItemClickListener
        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            public boolean onMenuItemClick(MenuItem item) {
                Toast.makeText(MainActivity.this,"You Clicked : " + item.getTitle(),Toast.LENGTH_SHORT).show();
                return true;
            }
        });

        popup.show();//showing popup menu
    }


    /////////////
    // Getters //
    /////////////

    // Get the first the song_tab played
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
    public void getAllSong()
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

                // Store the song_tab
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

            // Use a thread got the advantage to not block the display of loaded song_tab
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

    public ArrayList<Song> getSongList()
    {
        return songList;
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

