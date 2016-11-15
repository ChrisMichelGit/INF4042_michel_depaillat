package com.MyMusicPlayer;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.media.MediaPlayer;
import android.os.Build;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.util.LruCache;
import android.os.Bundle;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import android.net.Uri;
import android.content.ContentResolver;
import android.database.Cursor;
import android.support.v7.widget.LinearLayoutManager;
import android.view.View;
import android.widget.Toast;
import android.util.Log;


@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
class MainActivity extends Activity implements RecyclerViewClickListener {

    ////////////////
    // Attributes //
    ////////////////

    private ArrayList<Song> songList;                                                   // The list containing all the song of the user
    private FastScrollRecyclerView songView;                                            // The view displaying the song list
    private Song currPlaying;                                                           // The actual played song
    private MediaPlayer mp = new MediaPlayer();                                         // The media that play the song

    private String [] permissionsList = {Manifest.permission.READ_EXTERNAL_STORAGE};    // The list of needed permissions
    private static final int PERMISSIONS_READ_EXTERNAL_STORAGE = 1;                     // The request code of READ_EXTERNAL_STORAGE permission

    private LruCache<String, Bitmap> mMemoryCache;                                      // The cache containing all album cover
    private int coverByThread = 200;                                                    // The number of all album cover a thread should searching for (recommended to set here)
    private int numThread;                                                              // The number of thread necessary to load all album cover (do not set it here, it will be overridden)
    private int lastThread;                                                             // The number of album covers the last thread should searching for (do not set it here, it will be overridden)
    private int currThreadId;                                                           // The ID of the current thread


    ////////////////////////
    // Overridden Methods //
    ////////////////////////

    // Main methods //

    @TargetApi(Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) askForPermissions(permissionsList, PERMISSIONS_READ_EXTERNAL_STORAGE);

        MusicUtils.setDefaultBM(this.getApplicationContext());

        // Check if the song list has been already created
        // If yes it will save a lot of time and resources when rotate screen
        if(savedInstanceState == null || !savedInstanceState.containsKey("songList")) { // Saved instance found

            if (ActivityCompat.checkSelfPermission(MainActivity.this, permissionsList[0]) == PackageManager.PERMISSION_GRANTED) {
                getSongList();
            }

            // Get max available VM memory, exceeding this amount will throw an OutOfMemory exception. Stored in kilobytes as LruCache takes an int in its constructor.
            final int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);

            mMemoryCache = new LruCache<>(maxMemory);

            // Get all the album cover (more songs, longer it takes)
            getAllAlbumCover();

            // Set the cache
            retainFragment.mRetainedCache = mMemoryCache;
        }
        else { // No saved instance found
            mMemoryCache = retainFragment.mRetainedCache;
            songList = savedInstanceState.getParcelableArrayList("songList");
            if (songList != null) {
                for (Song song : songList)
                {
                    Log.e ("MainActivity", song.getTitle());
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
    protected void onSaveInstanceState(Bundle outState) {
        outState.putParcelableArrayList("songList", songList);
        super.onSaveInstanceState(outState);
    }

    // AppCompatActivity methods //

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == PERMISSIONS_READ_EXTERNAL_STORAGE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d("MainActivity", "Permission has been granted");
                getSongList();
                getAllAlbumCover();
                songView.getAdapter().notifyDataSetChanged();
            } else {
                Log.d("MainActivity", "Permission has been denied or request cancelled");
            }
        }
    }

    // RecyclerViewClickListener methods //

    @Override
    public void recyclerViewListClicked(View v, int position)
    {
        PlaySong(position);
    }


    /////////////
    // Methods //
    /////////////

    // Ask the needed permissions to the user (Only on Android 6.0 and higher)
    private void askForPermissions(String [] permission, Integer requestCode) {

        for (String perm : permission) {
            if (ActivityCompat.checkSelfPermission(MainActivity.this, perm) != PackageManager.PERMISSION_GRANTED) {

                // Should we show an explanation?
                if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this, perm)) {

                    // This is called if user has denied the permission before
                    // In this case I am just asking the permission again
                    ActivityCompat.requestPermissions(MainActivity.this, new String[]{perm}, requestCode);
                } else {
                    ActivityCompat.requestPermissions(MainActivity.this, new String[]{perm}, requestCode);
                }
            } else {
                Log.d ("MainActivity", "" + perm + " is already granted.");
            }
        }
    }

    // Add the bitmap to the cache
    public void addBitmapToMemoryCache(String key, Bitmap bitmap) {
        if (getBitmapFromMemCache(key) == null && bitmap != null) {
            Log.d ("MainActivity", "Bitmap Added to cache at key: " + key);
            mMemoryCache.put(key, bitmap);
        }
    }

    public void PlaySong (int position)
    {
        Log.d ("MainActivity", "Play song");
        Song song = songList.get(position);

        if (song != currPlaying || currPlaying == null) {
            currPlaying = song;
            //MediaPlayer nextPlayer = new MediaPlayer();

            mp.stop();
            mp.release();
            mp = new MediaPlayer();
            //mp.setNextMediaPlayer(nextPlayer);
            try {
                mp.setDataSource(song.getData());
                mp.prepare();
            } catch (IOException e) {
                e.printStackTrace();

            }
        }
        if (!mp.isPlaying())
        {
            mp.start();
        }
        else
        {
            mp.pause();
        }
    }


    /////////////
    // Getters //
    /////////////

    // Get the specified bitmap from the cache
    public Bitmap getBitmapFromMemCache(String key) {
        if (mMemoryCache.get(key) != null) Log.d ("MainActivity", "Get bitmap from cache at key: " + key);
        return mMemoryCache.get(key);
    }

    // Get all the songs that the user has
    public void getSongList() {

        Toast.makeText(this, "GET SONG LIST", Toast.LENGTH_SHORT).show();

        ContentResolver musicResolver = getContentResolver();
        Uri musicUri = android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        Cursor musicCursor = musicResolver.query(musicUri, null, MediaStore.Audio.Media.IS_MUSIC + "!= 0", null, null);

        if (musicCursor != null && musicCursor.moveToFirst()) {

            // Add songs to list
            do {
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


        Collections.sort(songList, new Comparator<Song>() {
            public int compare(Song a, Song b) {
                return a.getTitle().compareTo(b.getTitle());
            }
        });
    }

    // Get all the album cover (more songs, longer it takes)
    void getAllAlbumCover ()
    {
        // We determine the number of threads by the number total of songs divided by the number of album covers that a thread should searching for
        numThread = songList.size()/coverByThread;

        // If the rest is not 0 we need another thread to complete the missing album cover
        lastThread = songList.size() % coverByThread;

        // All the thread except the last one
        for (currThreadId = 0; currThreadId < numThread; currThreadId++) {

            // Use a thread got the advantage to not block the display of loaded song
            new Thread() {

                int threadId = currThreadId;
                @Override
                public void run() {
                    for (int i = numThread * threadId * 50; i < numThread * threadId * 50 + coverByThread; i++) {
                        Log.i("MAIN", "Thread n°" + threadId + " id: " + i);
                        songList.get(i).initBitmap();
                    }
                }
            }.start();
        }

        // The last thread
        new Thread() {
            @Override
            public void run() {
                for (int i = numThread * currThreadId * 50; i < numThread * currThreadId * 50 + lastThread; i++) {
                    Log.i("MAIN", "Thread n°" + currThreadId + " id: " + i);
                    songList.get(i).initBitmap();
                }
            }
        }.start();
    }


    /////////////////
    // Other class //
    /////////////////

    public static class RetainFragment extends Fragment {
        private static final String TAG = "RetainFragment";
        public LruCache<String, Bitmap> mRetainedCache;

        public RetainFragment() {
        }

        public static RetainFragment findOrCreateRetainFragment(FragmentManager fm) {
            RetainFragment fragment = (RetainFragment) fm.findFragmentByTag(TAG);
            if (fragment == null) {
                fragment = new RetainFragment();
                fm.beginTransaction().add(fragment, TAG).commit();
            }
            return fragment;
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setRetainInstance(true);
        }

    }
}
