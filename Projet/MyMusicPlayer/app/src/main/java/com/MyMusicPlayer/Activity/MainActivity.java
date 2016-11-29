package com.MyMusicPlayer.Activity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.graphics.Typeface;
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

import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.PopupMenu;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.util.Log;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.Toolbar;

import com.MyMusicPlayer.Album.Album;
import com.MyMusicPlayer.MusicService.MusicPlayerService;
import com.MyMusicPlayer.MusicService.ServiceCallbacks;
import com.MyMusicPlayer.PageAdapter;
import com.MyMusicPlayer.RecyclerViewFastScroll.FastScrollRecyclerView;
import com.MyMusicPlayer.Song.SongAdapter;
import com.MyMusicPlayer.Utilities.Utils;
import com.MyMusicPlayer.Utilities.MusicUtils;
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

    private ArrayList<Song> songList;                                                   // The list containing all the song of the user
    private ArrayList<Album> albumList;                                                 // The list containing all the album of the user
    private int currSongPlayingID = -1;                                                 // The ID of the actual played song
    private MusicPlayerService player;                                                  // The music service that manages every actions on audio
    boolean serviceBound = false;                                                       // To know if the above service is active or not

    private String[] permissionsList = {Manifest.permission.READ_EXTERNAL_STORAGE,
                                        Manifest.permission.READ_PHONE_STATE     };     // The list of needed permissions
    private int[] requestCode = {1, 2};                                                 // The request code for the permission (each code correspond to the permission at the same index of permissionList)

    private LruCache<String, Bitmap> mMemoryCache;                                      // The cache containing all album cover
    private RetainFragment retainFragment;                                              // Fragment that keep information until the application is destroy
    private int coverByThread = 200;                                                    // The number of all album cover a thread should searching for (recommended to set here)
    private int numThread;                                                              // The number of thread necessary to load all album cover (do not set it here, it will be overridden)
    private int lastThread;                                                             // The number of album covers the last thread should searching for (do not set it here, it will be overridden)
    private int currThreadId;                                                           // The ID of the current thread
    static Song firstSong;                                                              // Used by the MusicPlayerService on creation

    private int viewNumber;                                                             // Index of the view (0 = tabView, 1 = album_view)
    private int contextIndexView;                                                       // Value of viewNumber when the song has been started
    private Album currAlbum = null;                                                     // The current played song
    private Song currSong = null;                                                       // The album of the current played song
    private Album currLookingAlbum = null;                                              // The album that the user is currently looking at

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

            retainFragment.playerSave = player;
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

        retainFragment = RetainFragment.findOrCreateRetainFragment(getSupportFragmentManager());

        songList = new ArrayList<>();
        albumList = new ArrayList<>();

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

                // Get max available VM memory, exceeding this amount will throw an OutOfMemory exception. Stored in kilobytes as LruCache takes an int in its constructor.
                final int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);

                mMemoryCache = new LruCache<>(maxMemory);

                getAllSong();

                cleanAlbumList();

                // Get all the album cover (more songs, longer it takes)
                getAllAlbumCover();

                // Set the cache
                retainFragment.mRetainedCache = mMemoryCache;
                setTabLayout(0);
            }

        }
        else
        { // Saved instance found
            mMemoryCache = retainFragment.mRetainedCache;
            player = retainFragment.playerSave;
            onRestoreInstanceState(savedInstanceState);
            if (songList != null)
            {
                for (Song song : songList)
                {
                    Log.i(TAG, song.getTitle());
                    song.setActivity(this);
                    song.setBitmap(getBitmapFromMemCache(song.getAlbum()));
                }
            }
            if (albumList != null)
            {
                for (Album album : albumList)
                {
                    Log.i(TAG, album.getTitle());
                    album.setActivity(this);
                    album.setBitmap(getBitmapFromMemCache(album.getTitle()));
                }
            }
            chooseView (viewNumber);
        }
    }

    @Override
    protected void onStart ()
    {
        super.onStart();
        if (player != null) player.setCallbacks(MainActivity.this); // Refresh the service callback to use values of this instance
    }

    @Override
    protected void onSaveInstanceState(Bundle outState)
    {
        outState.putParcelableArrayList("songList", songList);
        outState.putParcelableArrayList("albumList", albumList);
        outState.putParcelable("currAlbum", currAlbum);
        outState.putParcelable("currSong", currSong);
        outState.putParcelable("currLookingAlbum", currLookingAlbum);
        outState.putBoolean("ServiceState", serviceBound);
        outState.putInt("currSongPlayingID", currSongPlayingID);
        outState.putInt("viewIndex", viewNumber);
        outState.putInt("contextIndexView", contextIndexView);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState)
    {
        Log.i (TAG, "Restored Instance");
        super.onRestoreInstanceState(savedInstanceState);
        songList = savedInstanceState.getParcelableArrayList("songList");
        albumList = savedInstanceState.getParcelableArrayList("albumList");
        currAlbum = savedInstanceState.getParcelable("currAlbum");
        currSong = savedInstanceState.getParcelable("currSong");
        currLookingAlbum = savedInstanceState.getParcelable("currLookingAlbum");
        serviceBound = savedInstanceState.getBoolean("ServiceState");
        currSongPlayingID = savedInstanceState.getInt("currSongPlayingID");
        viewNumber = savedInstanceState.getInt("viewIndex");
        contextIndexView = savedInstanceState.getInt("contextIndexView");
    }

    @Override
    public void onBackPressed() {
        moveTaskToBack(true);
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
    public void recyclerViewListClickedSong(View v, Song song, int position)
    {
        playSong(song, position);
    }

    @TargetApi(Build.VERSION_CODES.M)
    @SuppressLint("DefaultLocale")
    @Override
    public void recyclerViewListClickedAlbum(View v, Album album)
    { // Create the album view

        currLookingAlbum = album;
        viewNumber = 1;

        LayoutInflater inf = (LayoutInflater)getApplicationContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View albumView = inf.inflate(R.layout.album_view, (ViewGroup)v.getRootView(), false);

        // Set all the field of album_view
        TextView title = (TextView) albumView.findViewById(R.id.album_title);
        title.setText(album.getTitle());

        ImageView albumArt = (ImageView) albumView.findViewById(R.id.album_view_album_art);
        albumArt.setImageBitmap(album.getBitmap());

        TextView numSongs = (TextView) albumView.findViewById(R.id.album_songs_number);
        String numOfSongs = album.getSongList().size() + " " + getString(R.string.tracks);
        if (album.getSongList().size() == 1) numOfSongs = numOfSongs.replace("s", "");
        numSongs.setText(numOfSongs);

        TextView duration = (TextView) albumView.findViewById(R.id.total_time);
        duration.setText(MusicUtils.getDurationToString(album.getDuration()));

        // Song list of the album
        FastScrollRecyclerView albumSong = (FastScrollRecyclerView) albumView.findViewById(R.id.song_list);
        albumSong.getFastScroller().getPopup().setAlpha(0);
        albumSong.setAutoHideDelay(1000);
        albumSong.setPopupBgColor(getColor(R.color.grey));
        albumSong.setPopupTextColor(getColor(R.color.white));
        albumSong.setPopUpTypeface(Typeface.SANS_SERIF);
        albumSong.setPopupTextSize(100);
        albumSong.setTrackColor(getColor(R.color.alpha_grey));
        albumSong.setThumbColor(getColor(R.color.grey));



        SongAdapter sa = new SongAdapter(album.getSongList(), R.layout.song_tab, this);
        albumSong.setAdapter(sa);

        LinearLayoutManager lm = new LinearLayoutManager(this);
        albumSong.setLayoutManager(lm);
        albumSong.scrollToPosition(lm.findFirstCompletelyVisibleItemPosition());

        // Button to get back to the previous view
        Button back = (Button) albumView.findViewById(R.id.back_to_album);
        back.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                returnToAlbum(view);

            }
        });

        this.setContentView(albumView);
    }

    // ServiceCallbacks methods //

    @Override
    public Song getNextSong()
    {
        if (currAlbum != null && contextIndexView == 1)
        { // Song clicked from an album

            if (currSongPlayingID == currAlbum.getSongList().size() - 1)
            {
                currSongPlayingID = 0;
                currSong = currAlbum.getSongList().get(0);
                currAlbum = currSong.getAlbumRef();
                return currSong;
            }
            else
            {
                currSongPlayingID++;
                currSong = currAlbum.getSongList().get(currSongPlayingID);
                currAlbum = currSong.getAlbumRef();
                return currSong;
            }
        }
        else
        {
            if (currSongPlayingID == songList.size() - 1)
            {
                currSongPlayingID = 0;
                currSong = songList.get(0);
                currAlbum = currSong.getAlbumRef();
                return currSong;
            }
            else
            {
                currSongPlayingID++;
                currSong = songList.get(currSongPlayingID);
                currAlbum = currSong.getAlbumRef();
                return currSong;
            }
        }
    }

    @Override
    public Song getPreviousSong()
    {
        Log.d(TAG, currAlbum.getTitle());
        if (currAlbum != null && contextIndexView == 1)
        { // Song clicked from an album

            if (currSongPlayingID == 0)
            {
                currSongPlayingID = currAlbum.getSongList().size() - 1;
                currSong = currAlbum.getSongList().get(currSongPlayingID);
                currAlbum = currSong.getAlbumRef();
                return currSong;
            }
            else
            {
                currSongPlayingID--;
                currSong = currAlbum.getSongList().get(currSongPlayingID);
                currAlbum = currSong.getAlbumRef();
                return currSong;
            }
        }
        else
        {

            if (currSongPlayingID == 0)
            {
                currSongPlayingID = songList.size() - 1;
                currSong = songList.get(currSongPlayingID);
                currAlbum = currSong.getAlbumRef();
                return currSong;
            }
            else
            {
                currSongPlayingID--;
                currSong = songList.get(currSongPlayingID);
                currAlbum = currSong.getAlbumRef();
                return currSong;
            }
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
        if (bitmap != null)
        {
            Log.i(TAG, "Bitmap Added to cache at key: " + key);
            mMemoryCache.put(key, bitmap);
        }
    }

    public void playSong(Song song, int position)
    {
        Log.d(TAG, "Play song_tab");

        //Check is service is active
        if (!serviceBound)
        {
            Intent playerIntent = new Intent(this, MusicPlayerService.class);
            startService(playerIntent);
            firstSong = song;
            currSong = song;
            currAlbum = currSong.getAlbumRef();
            serviceBound = true;
            bindService(playerIntent, serviceConnection, Context.BIND_AUTO_CREATE);
            contextIndexView = viewNumber;
            currSongPlayingID = position;
        }
        else
        { // Service is active, play a new song_tab
            if (song != currSong && song != null)
            {
                currSong = song;
                currAlbum = currSong.getAlbumRef();
                Log.d(TAG, "PlayNewSong: " + currAlbum.getTitle());
                player.playNewSong(song);
                contextIndexView = viewNumber;
                currSongPlayingID = position;
            }
        }
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

    public void returnToAlbum(View view)
    {
        currLookingAlbum = null;
        setTabLayout(1);
    }


    private void chooseView(int viewNumber)
    {
        if (viewNumber == 0) setTabLayout(0);
        else if (viewNumber == 1) recyclerViewListClickedAlbum(this.findViewById(android.R.id.content), currLookingAlbum);
    }


    /////////////
    // Setters //
    /////////////

    // Initialize the album list
    public void setAlbumList (Song song) {
        Utils.ensureSize(albumList, (int) song.getAlbumID());
        if (albumList.get((int)song.getAlbumID()) == null)
        {
            Log.d (TAG, "Added a new album");
            Album tempAlbum = new Album(song.getArtist(), song.getAlbum(), song.getAlbumID(), this);
            tempAlbum.addSongToAlbum(song);
            albumList.add((int)song.getAlbumID(), tempAlbum);
        }
        else
        {
            albumList.get((int)song.getAlbumID()).addSongToAlbum(song);
        }
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public void setTabLayout (int tabIndex)
    {
        viewNumber = 0;

        setContentView(R.layout.tab_layout);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setActionBar(toolbar);

        TabLayout tabLayout = (TabLayout) findViewById(R.id.tab_layout);

        assert tabLayout != null;
        tabLayout.addTab(tabLayout.newTab().setText(R.string.songs));
        tabLayout.addTab(tabLayout.newTab().setText(R.string.albums));

        final ViewPager viewPager = (ViewPager) findViewById(R.id.pager);
        PagerAdapter adapter = new PageAdapter(getSupportFragmentManager(), tabLayout.getTabCount());

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

        // set the default tab
        viewPager.setCurrentItem(tabIndex, false);
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
        if (mMemoryCache.get(key) != null) Log.i(TAG, "Get bitmap from cache at key: " + key);
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

                // Add the song to its album
                setAlbumList(song);
                song.setAlbumRef(albumList.get((int)song.getAlbumID()));

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

    // Delete all null instance
    private void cleanAlbumList()
    {
        for (int i = 0; i < albumList.size(); i++)
        {
            if (albumList.get(i) == null)
            {
                albumList.remove(i);
                i--;
            }
        }

        // Sort the list by alphabetical order
        Collections.sort(albumList, new Comparator<Album>()
        {
            public int compare(Album a, Album b)
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

    public ArrayList<Album> getAlbumList() {return albumList; }



    /////////////////
    // Other class //
    /////////////////

    // A fragment that memorize the album cover
    public static class RetainFragment extends Fragment
    {
        private static final String TAG = "RetainFragment";
        public LruCache<String, Bitmap> mRetainedCache;
        public MusicPlayerService playerSave;

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

