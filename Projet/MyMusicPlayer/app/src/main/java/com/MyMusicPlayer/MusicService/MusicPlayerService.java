package com.MyMusicPlayer.MusicService;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.session.MediaSessionManager;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.support.v7.app.NotificationCompat;
import android.widget.RemoteViews;

import com.MyMusicPlayer.Activity.MainActivity;
import com.MyMusicPlayer.R;
import com.MyMusicPlayer.Utilities.MusicUtils;
import com.MyMusicPlayer.Song.Song;

import java.io.IOException;

public class MusicPlayerService extends Service implements MediaPlayer.OnCompletionListener, MediaPlayer.OnPreparedListener, MediaPlayer.OnErrorListener, MediaPlayer.OnSeekCompleteListener,
        MediaPlayer.OnInfoListener, MediaPlayer.OnBufferingUpdateListener, AudioManager.OnAudioFocusChangeListener, Runnable
{

    ////////////////
    // Attributes //
    ////////////////

    private String TAG = "MusicPlayerService"; // The log tag

    private final IBinder iBinder = new LocalBinder();
    private MediaPlayer mediaPlayer; // The media that actually plays the songs
    private Song currSong; // The song_tab currently playing
    private boolean isFirstSong;

    private ServiceCallbacks serviceCallbacks; // Use to communicate with the MainActivity

    //Used to pause/resume MediaPlayer
    private int resumePosition;
    private AudioManager audioManager;

    //Handle incoming phone calls
    private boolean ongoingCall = false;
    private PhoneStateListener phoneStateListener;
    private TelephonyManager telephonyManager;

    //MediaSession
    private MediaSessionManager mediaSessionManager;
    private MediaSessionCompat mediaSession;
    private MediaControllerCompat.TransportControls transportControls;

    // Strings used to notify which action is triggered from the MediaSession callback listener.
    public static final String ACTION_PLAY = "com.MyMusicPlayer.ACTION_PLAY";
    public static final String ACTION_PAUSE = "com.MyMusicPlayer.ACTION_PAUSE";
    public static final String ACTION_PREVIOUS = "com.MyMusicPlayer.ACTION_PREVIOUS";
    public static final String ACTION_NEXT = "com.MyMusicPlayer.ACTION_NEXT";
    public static final String ACTION_STOP = "com.MyMusicPlayer.ACTION_STOP";
    public static final String ACTION_REWIND = "com.MyMusicPlayer.ACTION_REWIND";
    public static final String ACTION_FASTFORWARD = "com.MyMusicPlayer.ACTION_FASTFORWARD";

    // MyMusicPlayer notification
    private static final int NOTIFICATION_ID = 10;
    private NotificationManager mNotificationManager;
    private Notification notif;
    private RemoteViews notifRemoteExpand, notifRemoteCompact;
    private Thread progressBarThread;
    private int total;
    private int currentPosition;

    ////////////////////////
    // Overridden Methods //
    ////////////////////////


    // Service methods //

    @Override
    public void onCreate()
    {
        Log.d("Tag", "onCreate");
        super.onCreate();
        // Perform one-time setup procedures

        // Manage incoming phone calls during playback.
        // Pause MediaPlayer on incoming call,
        // Resume on hangup.
        callStateListener();
        isFirstSong = true;
        currSong = MainActivity.getCurrentSongPlaying();

        try
        {
            initMediaSession();
            initMediaPlayer();
            buildNotification(PlaybackStatus.PLAYING);
            registerBecomingNoisyReceiver();
        }
        catch (RemoteException e)
        {
            e.printStackTrace();
        }
    }

    //The system calls this method when an activity, requests the service be started
    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        Log.d(TAG, "onStartCommand");

        //Request audio focus
        if (!requestAudioFocus())
        {
            //Could not gain focus
            stopSelf();
        }

        //Request audio focus
        if (!requestAudioFocus())
        {
            //Could not gain focus
            stopSelf();
        }

        if (mediaSessionManager == null)
        {
            try
            {
                initMediaSession();
                initMediaPlayer();
            }
            catch (RemoteException e)
            {
                e.printStackTrace();
                stopSelf();
            }
            //buildNotification(PlaybackStatus.PLAYING);
        }

        //Handle Intent action from MediaSession.TransportControls
        handleIncomingActions(intent);
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();
        if (mediaPlayer != null)
        {
            stopMedia();
            mediaPlayer.release();
        }
        removeAudioFocus();
        //Disable the PhoneStateListener
        if (phoneStateListener != null)
        {
            telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_NONE);
        }

        removeNotification();

        //unregister BroadcastReceivers
        unregisterReceiver(becomingNoisyReceiver);
        mediaSession.release();
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {

        Log.d(TAG, "onTaskRemoved");
        onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent)
    {
        return iBinder;
    }

    // AudioManager.OnAudioFocusChangeListener methods //

    @Override
    public void onAudioFocusChange(int focusState)
    {
        //Invoked when the audio focus of the system is updated.
        switch (focusState)
        {
            case AudioManager.AUDIOFOCUS_GAIN:
                // resume playback
                if (mediaPlayer == null) initMediaPlayer();
                else if (!mediaPlayer.isPlaying()) mediaPlayer.start();
                mediaPlayer.setVolume(1.0f, 1.0f);
                break;
            case AudioManager.AUDIOFOCUS_LOSS:
                // Lost focus for an unbounded amount of time: stop playback and release media player
                if (mediaPlayer.isPlaying()) mediaPlayer.stop();
                mediaPlayer.release();
                mediaPlayer = null;
                break;
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                // Lost focus for a short time, but we have to stop
                // playback. We don't release the media player because playback
                // is likely to resume
                if (mediaPlayer.isPlaying()) mediaPlayer.pause();
                break;
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                // Lost focus for a short time, but it's ok to keep playing
                // at an attenuated level
                if (mediaPlayer.isPlaying()) mediaPlayer.setVolume(0.1f, 0.1f);
                break;

        }
    }

    // MediaPlayer.OnCompletionListener methods //

    @Override
    public void onCompletion(MediaPlayer mediaPlayer)
    {
        //Invoked when playback of a media source has completed.
        stopMedia();
        //stop the service
        //stopSelf();
    }

    // MediaPlayer.OnErrorListener mathods //

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra)
    {
        //Invoked when there has been an error during an asynchronous operation.
        switch (what)
        {
            case MediaPlayer.MEDIA_ERROR_NOT_VALID_FOR_PROGRESSIVE_PLAYBACK:
                Log.d(TAG, "MEDIA ERROR NOT VALID FOR PROGRESSIVE PLAYBACK " + extra);
                break;
            case MediaPlayer.MEDIA_ERROR_SERVER_DIED:
                Log.d(TAG, "MEDIA ERROR SERVER DIED " + extra);
                break;
            case MediaPlayer.MEDIA_ERROR_UNKNOWN:
                Log.d(TAG, "MEDIA ERROR UNKNOWN " + extra);
                break;
            case MediaPlayer.MEDIA_ERROR_IO:
                Log.d(TAG, "MEDIA EROR IO" + extra);
        }
        return false;
    }

    // MediaPlayer.OnInfoListener methods //

    @Override
    public boolean onInfo(MediaPlayer mediaPlayer, int i, int i1)
    {
        //Invoked to communicate some info.
        return false;
    }

    // MediaPlayer.OnPreparedListener methods //

    @Override
    public void onPrepared(MediaPlayer mediaPlayer)
    {
        //Invoked when the media source is ready for playback.
        playMedia();
    }

    // MediaPlayer.OnSeekCompleteListener methods //

    @Override
    public void onSeekComplete(MediaPlayer mediaPlayer)
    {
//Invoked indicating the completion of a seek operation.
    }

    // MediaPlayer.OnBufferingUpdateListener methods //

    @Override
    public void onBufferingUpdate(MediaPlayer mediaPlayer, int i)
    {
//Invoked indicating buffering status of
        //a media resource being streamed over the network.
    }

    // Runnable methods //

    @Override
    public void run()
    {
        currentPosition = mediaPlayer.getCurrentPosition();
        total = mediaPlayer.getDuration();
        while (mediaPlayer != null && currentPosition <= total)
        {
            try
            {
                Thread.sleep(1000);
                currentPosition = mediaPlayer.getCurrentPosition();
            }
            catch (InterruptedException e)
            {
                e.printStackTrace();
                break;
            }
            update();
        }
        Log.d(TAG, "Thread end");
    }

    private synchronized void update ()
    {
        notifRemoteExpand.setProgressBar(R.id.progressBar_notif, total, currentPosition, false);
        notifRemoteExpand.setTextViewText(R.id.current_time, MusicUtils.getDurationToString(currentPosition));
        mNotificationManager.notify(NOTIFICATION_ID, notif);
    }

    /////////////
    // Methods //
    /////////////

    private void playMedia()
    {
        if (!mediaPlayer.isPlaying())
        {
            mediaPlayer.start();
            progressBarThread = new Thread(this);
            progressBarThread.start();

        }
    }

    private void stopMedia()
    {
        if (mediaPlayer == null) return;
        if (mediaPlayer.isPlaying())
        {
            mediaPlayer.stop();
            progressBarThread.interrupt();
        }
    }

    private void pauseMedia()
    {
        try
        {
            mediaPlayer.pause();
            resumePosition = mediaPlayer.getCurrentPosition();
            progressBarThread.interrupt();
        }
        catch (IllegalStateException e)
        {
            e.printStackTrace();
        }
    }

    private void resumeMedia()
    {
        if (!mediaPlayer.isPlaying())
        {
            mediaPlayer.seekTo(resumePosition);
            mediaPlayer.start();
            progressBarThread = new Thread(this);
            progressBarThread.start();
        }
    }

    private void nextSong()
    {
        stopMedia();
        currSong = serviceCallbacks.getNextSong();

        //reset mediaPlayer
        mediaPlayer.reset();
        initMediaPlayer();
    }

    private void previousSong()
    {
        stopMedia();
        currSong = serviceCallbacks.getPreviousSong();

        //reset mediaPlayer
        mediaPlayer.reset();
        initMediaPlayer();
    }

    private void fastForward()
    {
        Log.d (TAG, "Fast Forward");
        if (mediaPlayer != null)
        {
            if (mediaPlayer.getCurrentPosition() + 10000 < currSong.getDuration()) mediaPlayer.seekTo(mediaPlayer.getCurrentPosition()+10000);
            else nextSong();
        }
    }

    private void rewind()
    {
        if (mediaPlayer != null)
        {
            if (mediaPlayer.getCurrentPosition() - 10000 >= 0) mediaPlayer.seekTo(mediaPlayer.getCurrentPosition() - 10000);
            else previousSong();
        }
    }

    public void playNewSong(Song newSong)
    {
        stopMedia();
        currSong = newSong;
        isFirstSong = false;
        if (mediaPlayer == null) initMediaPlayer();
        mediaPlayer.reset();
        initMediaPlayer();
        updateSongMetaData();
        buildNotification(PlaybackStatus.PLAYING);
        playMedia();
    }

    private void initMediaPlayer()
    {
        Log.d(TAG, "InitMediaPlayer");
        if (mediaPlayer == null) mediaPlayer = new MediaPlayer();
        //Set up MediaPlayer event listeners
        mediaPlayer.setOnCompletionListener(this);
        mediaPlayer.setOnErrorListener(this);
        mediaPlayer.setOnPreparedListener(this);
        mediaPlayer.setOnBufferingUpdateListener(this);
        mediaPlayer.setOnSeekCompleteListener(this);
        mediaPlayer.setOnInfoListener(this);
        //Reset so that the MediaPlayer is not pointing to another data source
        mediaPlayer.reset();

        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        try
        {
            // Set the data source to the mediaFile location
            mediaPlayer.setDataSource(currSong.getData());
            mediaPlayer.prepare();
        }
        catch (IOException e)
        {
            e.printStackTrace();
            stopSelf();
        }
        currentPosition = 0;
    }

    private void updateSongMetaData()
    {
        // Update the current metadata
        mediaSession.setMetadata(new MediaMetadataCompat.Builder()
                .putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, currSong.getBitmap())
                .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, currSong.getArtist())
                .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, currSong.getAlbum())
                .putString(MediaMetadataCompat.METADATA_KEY_TITLE, currSong.getTitle())
                .build());
        updateRemoteViews();
    }

    // Update the remotes views
    private void updateRemoteViews()
    {
        if (notifRemoteExpand == null)
        { // Initialize the expanded remote view
            notifRemoteExpand = new RemoteViews(getPackageName(), R.layout.notification_player_expand);
            notifRemoteExpand.setOnClickPendingIntent(R.id.next_button, playbackAction(2));
            notifRemoteExpand.setOnClickPendingIntent(R.id.previous_button, playbackAction(3));
            notifRemoteExpand.setOnClickPendingIntent(R.id.rewind_button, playbackAction(4));
            notifRemoteExpand.setOnClickPendingIntent(R.id.fast_forward_button, playbackAction(5));

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            {
                notifRemoteExpand.setInt(R.id.bg_color_notif, "setBackgroundColor",getColor(android.R.color.black));
            }
            else
            {
                notifRemoteExpand.setInt(R.id.bg_color_notif, "setBackgroundColor", getResources().getColor(android.R.color.black));
            }
        }
        if (notifRemoteCompact == null)
        { // Initialize the compact remote view
            notifRemoteCompact = new RemoteViews(getPackageName(), R.layout.notification_player_compact);
            notifRemoteCompact.setOnClickPendingIntent(R.id.next_button, playbackAction(2));
            notifRemoteCompact.setOnClickPendingIntent(R.id.previous_button, playbackAction(3));
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            {
                notifRemoteCompact.setInt(R.id.bg_color_notif, "setBackgroundColor",getColor(android.R.color.black));
            }
            else
            {
                notifRemoteCompact.setInt(R.id.bg_color_notif, "setBackgroundColor", getResources().getColor(android.R.color.black));
            }
        }
        if (currSong != null)
        { // Set the different parts of the remote view

            // Remote view expand
            notifRemoteExpand.setProgressBar(R.id.progressBar_notif, currSong.getDuration(), currentPosition, false);
            notifRemoteExpand.setImageViewBitmap(R.id.notif_album_art, Bitmap.createScaledBitmap(currSong.getBitmap(), 300, 300, true));
            notifRemoteExpand.setTextViewText(R.id.notif_title, currSong.getTitle());
            notifRemoteExpand.setTextViewText(R.id.notif_album, currSong.getAlbum());
            notifRemoteExpand.setTextViewText(R.id.notif_artist, currSong.getArtist());
            notifRemoteExpand.setTextViewText(R.id.current_time, MusicUtils.getDurationToString(currentPosition));
            notifRemoteExpand.setTextViewText(R.id.total_time, MusicUtils.getDurationToString(currSong.getDuration()));

            // Remote view compact
            notifRemoteCompact.setImageViewBitmap(R.id.notif_album_art, Bitmap.createScaledBitmap(currSong.getBitmap(), 200, 200, true));
            notifRemoteCompact.setTextViewText(R.id.notif_title, currSong.getTitle());
            notifRemoteCompact.setTextViewText(R.id.notif_album, currSong.getAlbum());
            notifRemoteCompact.setTextViewText(R.id.notif_artist, currSong.getArtist());
            notifRemoteCompact.setTextViewText(R.id.current_time, MusicUtils.getDurationToString(currentPosition));
            notifRemoteCompact.setTextViewText(R.id.total_time, MusicUtils.getDurationToString(currSong.getDuration()));
        }
    }

    private boolean requestAudioFocus()
    {
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        int result = audioManager.requestAudioFocus(this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
        if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED)
        {
            //Focus gained
            return true;
        }
        //Could not gain focus
        return false;
    }

    private boolean removeAudioFocus()
    {
        return AudioManager.AUDIOFOCUS_REQUEST_GRANTED == audioManager.abandonAudioFocus(this);
    }

    //Handle incoming phone calls
    private void callStateListener()
    {
        // Get the telephony manager
        telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        //Starting listening for PhoneState changes
        phoneStateListener = new PhoneStateListener()
        {
            @Override
            public void onCallStateChanged(int state, String incomingNumber)
            {
                switch (state)
                {
                    //if at least one call exists or the phone is ringing
                    //pause the MediaPlayer
                    case TelephonyManager.CALL_STATE_OFFHOOK:
                    case TelephonyManager.CALL_STATE_RINGING:
                        if (mediaPlayer != null)
                        {
                            pauseMedia();
                            ongoingCall = true;
                        }
                        break;
                    case TelephonyManager.CALL_STATE_IDLE:
                        // Phone idle. Start playing.
                        if (mediaPlayer != null)
                        {
                            if (ongoingCall)
                            {
                                ongoingCall = false;
                                resumeMedia();
                            }
                        }
                        break;
                }
            }
        };
        // Register the listener with the telephony manager
        // Listen for changes to the device call state.
        telephonyManager.listen(phoneStateListener,
                PhoneStateListener.LISTEN_CALL_STATE);
    }

    private void initMediaSession() throws RemoteException
    {
        if (mediaSessionManager != null) return; //mediaSessionManager exists

        mediaSessionManager = (MediaSessionManager) getSystemService(Context.MEDIA_SESSION_SERVICE);
        // Create a new MediaSession
        mediaSession = new MediaSessionCompat(getApplicationContext(), "MyMusicPlayer");
        //Get MediaSessions transport controls
        transportControls = mediaSession.getController().getTransportControls();
        //set MediaSession -> ready to receive media commands
        mediaSession.setActive(true);
        //indicate that the MediaSession handles transport control commands
        // through its MediaSessionCompat.Callback.
        mediaSession.setFlags(MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);

        // Attach Callback to receive MediaSession updates
        mediaSession.setCallback(new MediaSessionCompat.Callback()
        {
            // Implement callbacks
            @Override
            public void onPlay()
            {
                super.onPlay();
                resumeMedia();
                buildNotification(PlaybackStatus.PLAYING);
            }

            @Override
            public void onPause()
            {
                super.onPause();
                pauseMedia();
                buildNotification(PlaybackStatus.PAUSED);
            }

            @Override
            public void onSkipToNext()
            {
                super.onSkipToNext();
                nextSong();
                updateSongMetaData();
                buildNotification(PlaybackStatus.PLAYING);
            }

            @Override
            public void onSkipToPrevious()
            {
                super.onSkipToPrevious();
                previousSong();
                updateSongMetaData();
                buildNotification(PlaybackStatus.PLAYING);
            }

            @Override
            public void onStop()
            {
                super.onStop();
                removeNotification();
                //Stop the service
                stopSelf();
            }

            @Override
            public void onFastForward ()
            {
                super.onFastForward();
                fastForward();
                updateSongMetaData();
                buildNotification(PlaybackStatus.PLAYING);
            }

            @Override
            public void onRewind ()
            {
                super.onRewind();
                rewind();
                updateSongMetaData();
                buildNotification(PlaybackStatus.PLAYING);
            }

            @Override
            public void onSeekTo(long position)
            {
                super.onSeekTo(position);
            }
        });
        updateRemoteViews();
    }


    // Build and update the notification
    @TargetApi(Build.VERSION_CODES.M)
    private void buildNotification(PlaybackStatus playbackStatus)
    {

        /**
         * Notification actions -> playbackAction()
         *  0 -> Play
         *  1 -> Pause
         *  2 -> Next track
         *  3 -> Previous track
         *  4 -> Rewind
         *  5 -> Fast forward
         */

        int notificationAction;
        PendingIntent play_pauseAction = null;

        //Build a new notification according to the current state of the MediaPlayer
        if (playbackStatus == PlaybackStatus.PLAYING)
        {
            // Update the play_pause icon (now set to pause)
            notificationAction = android.R.drawable.ic_media_pause;
            notifRemoteExpand.setImageViewResource(R.id.play_pause, notificationAction);
            notifRemoteCompact.setImageViewResource(R.id.play_pause, notificationAction);

            // Update the action of the play_pause button (now set to pause)
            notifRemoteExpand.setOnClickPendingIntent(R.id.play_pause, playbackAction(1));
            notifRemoteCompact.setOnClickPendingIntent(R.id.play_pause, playbackAction(1));

            //create the pause action
            play_pauseAction = playbackAction(1);
        }
        else if (playbackStatus == PlaybackStatus.PAUSED)
        {

            // Update the play_pause icon (now set to play)
            notificationAction = android.R.drawable.ic_media_play;
            notifRemoteExpand.setImageViewResource(R.id.play_pause, notificationAction);
            notifRemoteCompact.setImageViewResource(R.id.play_pause, notificationAction);

            // Update the action of the play_pause button (now set to play)
            notifRemoteExpand.setOnClickPendingIntent(R.id.play_pause, playbackAction(0));
            notifRemoteCompact.setOnClickPendingIntent(R.id.play_pause, playbackAction(0));

            //create the play action
            play_pauseAction = playbackAction(0);
        }

        NotificationCompat.Builder notificationBuilder;
        notificationBuilder = (NotificationCompat.Builder) new NotificationCompat.Builder(this)
                .setContentIntent(play_pauseAction).setSmallIcon(android.R.drawable.stat_sys_headset).setLargeIcon(currSong.getBitmap())
                .setContent(notifRemoteExpand).setOngoing(true);;

        notif = notificationBuilder.build();
        notif.bigContentView = notifRemoteExpand;
        notif.contentView = notifRemoteCompact;

        mNotificationManager = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        mNotificationManager.notify(NOTIFICATION_ID, notif);
    }

    // The action that each buttons of the notification will return
    private PendingIntent playbackAction(int actionNumber)
    {
        Intent playbackAction = new Intent(this, MusicPlayerService.class);
        switch (actionNumber)
        {
            case 0:
                // Play
                playbackAction.setAction(ACTION_PLAY);
                return PendingIntent.getService(this, actionNumber, playbackAction, 0);
            case 1:
                // Pause
                playbackAction.setAction(ACTION_PAUSE);
                return PendingIntent.getService(this, actionNumber, playbackAction, 0);
            case 2:
                // Next track
                playbackAction.setAction(ACTION_NEXT);
                return PendingIntent.getService(this, actionNumber, playbackAction, 0);
            case 3:
                // Previous track
                playbackAction.setAction(ACTION_PREVIOUS);
                return PendingIntent.getService(this, actionNumber, playbackAction, 0);
            case 4:
                // Rewind
                playbackAction.setAction(ACTION_REWIND);
                return PendingIntent.getService(this, actionNumber, playbackAction, 0);
            case 5:
                // Fast forward
                playbackAction.setAction(ACTION_FASTFORWARD);
                return PendingIntent.getService(this, actionNumber, playbackAction, 0);
            default:
                break;
        }
        return null;
    }

    // Remove every notification that the service has opened
    private void removeNotification()
    {
        Log.d(TAG, "removeNotification");
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(NOTIFICATION_ID);
        notificationManager.cancelAll();
    }

    // Catch the input of the notification button and execute the correct action
    private void handleIncomingActions(Intent playbackAction)
    {
        if (playbackAction == null || playbackAction.getAction() == null) return;

        String actionString = playbackAction.getAction();
        if (actionString.equalsIgnoreCase(ACTION_PLAY))
        {
            transportControls.play();
        }
        else if (actionString.equalsIgnoreCase(ACTION_PAUSE))
        {
            transportControls.pause();
        }
        else if (actionString.equalsIgnoreCase(ACTION_NEXT))
        {
            transportControls.skipToNext();
        }
        else if (actionString.equalsIgnoreCase(ACTION_PREVIOUS))
        {
            transportControls.skipToPrevious();
        }
        else if (actionString.equalsIgnoreCase(ACTION_STOP))
        {
            transportControls.stop();
        }
        else if (actionString.equalsIgnoreCase(ACTION_REWIND))
        {
            transportControls.rewind();
        }
        else if (actionString.equalsIgnoreCase(ACTION_FASTFORWARD))
        {
            transportControls.fastForward();
        }
    }


    /////////////
    // Setters //
    /////////////

    public void setCallbacks(ServiceCallbacks callbacks)
    {
        serviceCallbacks = callbacks;
    }


    /////////////////
    // Other class //
    /////////////////

    public class LocalBinder extends Binder
    {
        public MusicPlayerService getService()
        {
            return MusicPlayerService.this;
        }
    }

    // Pause audio when the headset is unplugged
    private BroadcastReceiver becomingNoisyReceiver = new BroadcastReceiver()
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            if (!isFirstSong && intent.getAction().equals(Intent.ACTION_HEADSET_PLUG) && currSong != null)
            {
                int state = intent.getIntExtra("state", -1);
                switch (state)
                {
                    case 0:
                        // Headset is unplugged
                        pauseMedia();
                        buildNotification(PlaybackStatus.PAUSED);
                        break;
                    case 1:
                        // Headset is plugged
                        playMedia();
                        buildNotification(PlaybackStatus.PLAYING);
                        break;
                    default:
                }
            }
            isFirstSong = false;
        }
    };

    private void registerBecomingNoisyReceiver()
    {
        //register after getting audio focus
        IntentFilter intentFilter = new IntentFilter(AudioManager.ACTION_HEADSET_PLUG);
        Log.d(TAG, "registerBecomingNoisyReceiver");
        registerReceiver(becomingNoisyReceiver, intentFilter);
    }
}
