package com.MyMusicPlayer;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

class MusicPlayer extends Service {
    public MusicPlayer() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }
}