package com.njromano.songyt;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.support.design.widget.Snackbar;
import android.util.Log;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;

/**
 * Created by Nick on 2/10/17.
 */

public class NotificationService extends Service {
    private String TAG = this.getClass().getSimpleName();
    private String mName;
    private volatile Looper mServiceLooper;
    private volatile ServiceHandler mServiceHandler;
    private String ACTION_LISTENER = "com.njromano.songyt.NOTIFICATION_LISTENER";
    private String ACTION_SERVICE = "com.njromano.songyt.NOTIFICATION_SERVICE";
    private NotificationService.NotificationReceiver nReceiver;
    private Song mSong;
    private StringRequest mYTRequest;
    private ArrayList<YTResource> mYTResources;


    private final class ServiceHandler extends Handler {
        public ServiceHandler(Looper looper)
        {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg)
        {
            Intent i = new Intent(ACTION_SERVICE);
            i.putExtra("command", "getSong");
            sendBroadcast(i);

            try{
                Thread.sleep(5000);
            } catch (InterruptedException e)
            {
                Thread.currentThread().interrupt();
            }
        }
    }

    @Override
    public void onCreate()
    {
        super.onCreate();
        HandlerThread thread = new HandlerThread("ServiceStartArguments", Process.THREAD_PRIORITY_BACKGROUND);
        thread.start();

        mServiceLooper = thread.getLooper();
        mServiceHandler = new ServiceHandler(mServiceLooper);

        mSong = new Song();
        mYTResources = new ArrayList<>();

        nReceiver = new NotificationService.NotificationReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_LISTENER);
        registerReceiver(nReceiver, filter);

        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);

        Notification notification = new Notification.Builder(this)
                .setContentTitle(getText(R.string.notification_title))
                .setContentText(getText(R.string.notification_message))
                .setSmallIcon(R.drawable.ic_action_search)
                .setContentIntent(pendingIntent)
                .setTicker(getText(R.string.ticker_text))
                .build();

        startForeground(1, notification);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        Message msg = mServiceHandler.obtainMessage();
        msg.arg1 = startId;
        mServiceHandler.sendMessage(msg);

        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent)
    {
        return null;
    }

    @Override
    public void onDestroy()
    {
        Toast.makeText(this, "Notification service terminated.", Toast.LENGTH_SHORT).show();
        super.onDestroy();
        unregisterReceiver(nReceiver);
    }

    private void getYoutubeInfo()
    {
        String url = "";
        try {
            url = "https://www.googleapis.com/youtube/v3/search"
                    + "?part=snippet"
                    + "&q=" + URLEncoder.encode(mSong.getArtist() + " " + mSong.getTitle(), "UTF-8")
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
                        Log.d(TAG, response);
                        try {
                            JSONObject jsonResponse = new JSONObject(response);
                            mYTResources.clear();
                            mYTResources.addAll(YTResource.fromJson(jsonResponse.getJSONArray("items")));
                            if(mYTResources.isEmpty()) {
                                Intent i = new Intent(ACTION_LISTENER);
                                i.putExtra("error", "No matching songs on YouTube.");
                                sendBroadcast(i);
                            }
                            else
                            {
                                String url = "https://www.youtube.com/watch?v="
                                        + mYTResources.get(0).getVideoId();
                                //mLinkText.setText(url);
                                Intent youtubeIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                startActivity(youtubeIntent);
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
    }

    class NotificationReceiver extends BroadcastReceiver
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            if(intent.hasExtra("error"))
            {
                //Snackbar.make(mSongytStatusText, "Error: " + intent.getStringExtra("error"),
                //        Snackbar.LENGTH_LONG)
                //        .show();
                Toast.makeText(getBaseContext(),
                        "Error: " + intent.getStringExtra("error"),
                        Toast.LENGTH_SHORT).show();
            }
            else
            {
                mSong.setTitle(intent.getStringExtra("song_title"));
                mSong.setArtist(intent.getStringExtra("artist_name"));
                //mSongText.setText(mSong.getTitle());
                //mArtistText.setText(mSong.getArtist());
                getYoutubeInfo();
                MySingleton.getInstance(getApplicationContext()).addToRequestQueue(mYTRequest);
                Log.d(TAG,mYTRequest.toString());
            }
        }
    }
}
