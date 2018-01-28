package com.njromano.songyt;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.Uri;
import android.net.wifi.WifiConfiguration;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.support.v7.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;
import java.util.ArrayList;

/**
 * Created by Nick on 9/20/16.
 */
public class NotificationListener extends NotificationListenerService {
    private String TAG = this.getClass().getSimpleName();
    private String ACTION_LISTENER = "com.njromano.songyt.NOTIFICATION_LISTENER";
    private String ACTION_SERVICE = "com.njromano.songyt.NOTIFICATION_SERVICE";
    private NLServiceReceiver nlservicereceiver;
    private int mLastNotificationID;

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
    public void onNotificationPosted(final StatusBarNotification sbn)
    {
        super.onNotificationPosted(sbn);
        Log.d(TAG, "onNotificationPosted");
        Log.d(TAG, "NOTIFICATION POSTED: " + sbn.getPackageName() + "\n" + sbn.getNotification().toString() + "\n" + sbn.getNotification().extras.toString() + "\n");

        // get notification preference
        SharedPreferences preferences = getSharedPreferences(getString(R.string.prefs), MODE_PRIVATE);
        if(!preferences.getBoolean(getString(R.string.notification_preference), false))
            return;

        if (sbn.getPackageName().equals("com.google.android.music"))
        {
            final String songTitle = sbn.getNotification().extras.get("android.title").toString();
            final String songArtist = sbn.getNotification().extras.get("android.text").toString();
            String url = "";
            try {
                url = "https://www.googleapis.com/youtube/v3/search"
                        + "?part=snippet"
                        + "&q=" + URLEncoder.encode(songTitle + " " + songArtist, "UTF-8")
                        + "&type=video"
                        + "&key=" + URLEncoder.encode(getResources().getString(R.string.API_KEY), "UTF-8");
            } catch (UnsupportedEncodingException e)
            {
                e.printStackTrace();
            }

            StringRequest r = new StringRequest(Request.Method.GET, url,
                    new Response.Listener<String>() {
                        @Override
                        public void onResponse(String response)
                        {
                            ArrayList<YTResource> ytresults = new ArrayList<>();
                            Log.d(TAG, response);
                            try {
                                JSONObject jsonResponse = new JSONObject(response);
                                ytresults.clear();
                                ytresults.addAll(YTResource.fromJson(jsonResponse.getJSONArray("items")));
                                if(ytresults.isEmpty()) {
                                    Intent i = new Intent(ACTION_LISTENER);
                                    i.putExtra("error", "No matching songs on YouTube.");
                                    sendBroadcast(i);
                                }
                                else
                                {
                                    String url = "https://www.youtube.com/watch?v="
                                            + ytresults.get(0).getVideoId();
                                    NotificationManager nManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
                                    // build a new notification
                                    NotificationCompat.Builder ncomp = new NotificationCompat.Builder(getApplicationContext());
                                    ncomp.setContentTitle(songTitle + " by " + songArtist);
                                    ncomp.setContentText("Tap to open in YouTube.");
                                    ncomp.setTicker("Songyt");
                                    ncomp.setSmallIcon(R.mipmap.ic_launcher);
                                    ncomp.setAutoCancel(true);
                                    ncomp.setContentIntent(PendingIntent.getActivity(getApplicationContext(), 0, new Intent(Intent.ACTION_VIEW, Uri.parse(url)),PendingIntent.FLAG_UPDATE_CURRENT));
                                    nManager.notify(mLastNotificationID,ncomp.build());
                                }
                            }catch (JSONException e)
                            {
                                e.printStackTrace();
                                //Snackbar.make(mSongytStatusText, "Sorry, an error has occurred.", Snackbar.LENGTH_SHORT).show();
                                Toast.makeText(getBaseContext(),
                                        "Sorry, an error has occured with Songyt.",
                                        Toast.LENGTH_SHORT).show();
                            }
                        }
                    }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error)
                {
                    error.printStackTrace();
                    Log.d(TAG, error.toString());
                    //Snackbar.make(mSongytStatusText, "Sorry, an error has occurred. Please check your internet connection and try again.", Snackbar.LENGTH_LONG).show();
                    Toast.makeText(getBaseContext(),
                            "Sorry, an error has occurred with Songyt. Please check your internet connection and try again.",
                            Toast.LENGTH_SHORT).show();
                }
            });

            MySingleton.getInstance(getApplicationContext()).addToRequestQueue(r);
        }


    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn)
    {
        super.onNotificationRemoved(sbn);
        Log.d(TAG, "onNotificationRemoved");
        Log.d(TAG, "NOTIFICATION REMOVED: " + sbn.getPackageName() + "\n" + sbn.getNotification().toString() + "\n" + sbn.getNotification().extras.toString() + "\n");

        if (sbn.getPackageName().equals("com.google.android.music"))
        {
            NotificationManager notificationManager =  (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            notificationManager.cancel(mLastNotificationID);
        }
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
                    // TODO what the hell is happening with Pandora??
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
