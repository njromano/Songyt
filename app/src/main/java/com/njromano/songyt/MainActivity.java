package com.njromano.songyt;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;

import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Text;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static android.provider.Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS;

// TODO: create cohesive walkthrough of user flow

public class MainActivity extends AppCompatActivity {
    private String TAG = this.getClass().getSimpleName();
    private String ACTION_LISTENER = "com.njromano.songyt.NOTIFICATION_LISTENER";
    private String ACTION_SERVICE = "com.njromano.songyt.NOTIFICATION_SERVICE";
    private TextView mSongText, mArtistText, mLinkText;
    private TextView mSongytStatusText;
    private NotificationReceiver nReceiver;
    private Song mSong;
    private StringRequest mYTRequest;
    private ArrayList<YTResource> mYTResources;
    private ProgressBar mProgressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mSongytStatusText = (TextView) findViewById(R.id.songyt_status_text);
        mProgressBar = (ProgressBar) findViewById(R.id.progressbar);
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                //        .setAction("Action", null).show();
                Intent i = new Intent(ACTION_SERVICE);
                i.putExtra("command", "getSong");
                sendBroadcast(i);
                showLoading();
                //toggleNotificationListenerService();
            }
        });


        mSong = new Song();
        mYTResources = new ArrayList<>();

        //mSongText = (TextView) findViewById(R.id.songtext);
        //mArtistText = (TextView) findViewById(R.id.artisttext);
        //mLinkText = (TextView) findViewById(R.id.linktext);

        nReceiver = new NotificationReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_LISTENER);
        registerReceiver(nReceiver, filter);
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        hideLoading();
        checkIfNotificationsEnabled();
    }

    private void showLoading()
    {
        mSongytStatusText.setVisibility(View.INVISIBLE);
        mProgressBar.setVisibility(View.VISIBLE);
    }

    private void hideLoading()
    {
        mSongytStatusText.setVisibility(View.VISIBLE);
        mProgressBar.setVisibility(View.INVISIBLE);
    }

    private void checkIfNotificationsEnabled()
    {
        boolean notificationsEnabled = Settings.Secure
                .getString(this.getContentResolver(),"enabled_notification_listeners")
                .contains(getApplicationContext().getPackageName());
        Log.d(TAG, "notificationsEnabled = " + String.valueOf(notificationsEnabled));
        if(!notificationsEnabled)
        {
            mSongytStatusText.setText(null);
            final Snackbar snackBar = Snackbar.make(mSongytStatusText,
                    "Please allow Songyt access to device notifications.",
                    Snackbar.LENGTH_INDEFINITE);
            snackBar.setAction("SETTINGS", new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            startActivity(new Intent(ACTION_NOTIFICATION_LISTENER_SETTINGS));
                            snackBar.dismiss();
                        }
                    }).show();
        }
        else
        {
            mSongytStatusText.setText(R.string.songyt_status_1);
            Snackbar.make(mSongytStatusText, R.string.songyt_hint_1, Snackbar.LENGTH_LONG).show();
        }
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
        mYTRequest = new StringRequest(Request.Method.GET, url,
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
                                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
                                hideLoading();
                            }
                        }catch (JSONException e)
                        {
                            hideLoading();
                            e.printStackTrace();
                            Snackbar.make(mSongytStatusText, "Sorry, an error has occurred.", Snackbar.LENGTH_SHORT).show();
                        }
                    }
                }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error)
                    {
                        hideLoading();
                        error.printStackTrace();
                        Log.d(TAG, error.toString());
                        Snackbar.make(mSongytStatusText, "Sorry, an error has occurred. Please check your internet connection and try again.", Snackbar.LENGTH_LONG).show();
                    }
                });
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();
        unregisterReceiver(nReceiver);
    }

    class NotificationReceiver extends BroadcastReceiver
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            if(intent.hasExtra("error"))
            {
                hideLoading();
                Snackbar.make(mSongytStatusText, "Error: " + intent.getStringExtra("error"),
                        Snackbar.LENGTH_LONG)
                        .show();
            }
            else
            {
                mSongytStatusText.setText(R.string.songyt_status_2);
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
