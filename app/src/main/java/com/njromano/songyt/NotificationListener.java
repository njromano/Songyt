package com.njromano.songyt;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WifiConfiguration;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.Log;

/**
 * Created by Nick on 9/20/16.
 */
public class NotificationListener extends NotificationListenerService {
    private String TAG = this.getClass().getSimpleName();
    private String ACTION_LISTENER = "com.njromano.songyt.NOTIFICATION_LISTENER";
    private String ACTION_SERVICE = "com.njromano.songyt.NOTIFICATION_SERVICE";
    private NLServiceReceiver nlservicereceiver;

    @Override
    public void onCreate()
    {
        super.onCreate();
        Log.d(TAG, "NotificationListener created");
        nlservicereceiver = new NLServiceReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_SERVICE);
        registerReceiver(nlservicereceiver, filter);
    }

    @Override
    public void onListenerConnected()
    {
        super.onListenerConnected();
        Log.d(TAG, "onListenerConnected");
        StatusBarNotification[] sbns = getActiveNotifications();
        for(int i=0; i<sbns.length; i++)
        {
            Log.d(TAG, "NOTIFICATION RECEIVED: " + sbns[i].getPackageName() + "\n" + sbns[i].getNotification().toString() + "\n");
        }
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn)
    {
        // not needed in this implementation
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn)
    {
        // not needed in this implementation
    }

    @Override
    public void onDestroy()
    {
        Log.d(TAG, "NotificationListener destroyed");
        super.onDestroy();
        unregisterReceiver(nlservicereceiver);
    }

    class NLServiceReceiver extends BroadcastReceiver
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            if(intent.getStringExtra("command").equals("getSong"))
            {
                boolean songFound = false;
                // search in current active notifications and broadcast the first result known to be
                // from a music application to the notification listener in MainActivity
                for(StatusBarNotification sbn : NotificationListener.this.getActiveNotifications())
                {
                    // Look for songs being played by Google Play Music.
                    // Notification extras:
                    //  android.title - song title
                    //  android.text - artist name
                    String songTitle = " ";
                    String artistName = " ";
                    if (sbn.getPackageName().equals("com.google.android.music"))
                    {
                        songTitle = (String) sbn.getNotification().extras.get("android.title");
                        artistName = (String) sbn.getNotification().extras.get("android.text");

                        Log.d(TAG, "SONG FOUND: " + artistName + " - " + songTitle + "\n");
                        Intent i = new Intent(ACTION_LISTENER);
                        i.putExtra("song_title", songTitle);
                        i.putExtra("artist_name", artistName);
                        sendBroadcast(i);

                        songFound = true;
                    }
                    // Look for songs being played by Spotify
                    // Notifications dictated by "tickerText"
                    // "Song — Artist"
                    else if (sbn.getPackageName().equals("com.spotify.music"))
                    {
                        //Log.d(TAG, "SPOTIFY: " + sbn.getNotification().tickerText.toString());
                        String[] tickerText = sbn.getNotification().tickerText.toString().split("—");
                        songTitle = tickerText[0];
                        artistName = tickerText[1];

                        Log.d(TAG, "SONG FOUND: " + artistName + " - " + songTitle + "\n");
                        Intent i = new Intent(ACTION_LISTENER);
                        i.putExtra("song_title", songTitle);
                        i.putExtra("artist_name", artistName);
                        sendBroadcast(i);

                        songFound = true;
                    }
                    else if(sbn.getPackageName().equals("com.pandora.android"))
                    {
                        Log.d(TAG, "PANDORA: " + sbn.getNotification().extras.toString());
                    }
                }

                // send an error message to MainActivity if a song was not found
                if(!songFound)
                {
                    Intent noSongIntent = new Intent(ACTION_LISTENER);
                    noSongIntent.putExtra("error", "No song found to be playing.");
                    sendBroadcast(noSongIntent);
                }
            }
        }
    }
}
