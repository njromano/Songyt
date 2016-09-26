package com.njromano.songyt;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
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

public class MainActivity extends AppCompatActivity {
    private String TAG = this.getClass().getSimpleName();
    private String ACTION_LISTENER = "com.njromano.songyt.NOTIFICATION_LISTENER";
    private String ACTION_SERVICE = "com.njromano.songyt.NOTIFICATION_SERVICE";
    private TextView mSongText, mArtistText, mLinkText;
    private NotificationReceiver nReceiver;
    private Song mSong;
    private StringRequest mYTRequest;
    private ArrayList<YTResource> mYTResources;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                //        .setAction("Action", null).show();
                Intent i = new Intent(ACTION_SERVICE);
                i.putExtra("command", "getSong");
                sendBroadcast(i);
            }
        });

        mSong = new Song();
        mYTResources = new ArrayList<>();

        mSongText = (TextView) findViewById(R.id.songtext);
        mArtistText = (TextView) findViewById(R.id.artisttext);
        mLinkText = (TextView) findViewById(R.id.linktext);

        nReceiver = new NotificationReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_LISTENER);
        registerReceiver(nReceiver, filter);
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
                                mLinkText.setText("https://www.youtube.com/watch?v="
                                        + mYTResources.get(0).getVideoId());
                            }
                        }catch (JSONException e)
                        {
                            e.printStackTrace();
                        }
                    }
                }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error)
                    {
                        error.printStackTrace();
                        Log.d(TAG, error.toString());
                    }
                });
    }

    public void makeSnackbar(String text)
    {
        Snackbar.make(mArtistText, text, Snackbar.LENGTH_LONG).show();
    }

    @Override
    public void onStart()
    {
        super.onStart();
        //startService(new Intent(getApplicationContext(), NotificationListener.class));
    }

    @Override
    public void onStop()
    {
        //stopService(new Intent(getApplicationContext(), NotificationListener.class));
        super.onStop();
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();
        unregisterReceiver(nReceiver);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    class NotificationReceiver extends BroadcastReceiver
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            if(intent.hasExtra("error"))
            {
                Snackbar.make(mSongText, "Error: " + intent.getStringExtra("error"),
                        Snackbar.LENGTH_LONG)
                        .show();
            }
            else
            {
                mSong.setTitle(intent.getStringExtra("song_title"));
                mSong.setArtist(intent.getStringExtra("artist_name"));
                mSongText.setText(mSong.getTitle());
                mArtistText.setText(mSong.getArtist());
                getYoutubeInfo();
                MySingleton.getInstance(getApplicationContext()).addToRequestQueue(mYTRequest);
                Log.d(TAG,mYTRequest.toString());
            }
        }
    }
}
