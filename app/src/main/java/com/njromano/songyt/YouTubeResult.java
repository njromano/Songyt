package com.njromano.songyt;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.ImageView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;

/**
 * Created by Nick on 9/21/16.
 * Class for YouTube Resources to be pulled from JSON
 */
public class YouTubeResult {
    private String TAG = getClass().getSimpleName();
    private String videoId, description, title, channel, imageURL;

    public YouTubeResult(JSONObject object)
    {
        try {
            this.videoId = object.getJSONObject("id").getString("videoId");
            this.description = object.getJSONObject("snippet").getString("description");
            this.title = object.getJSONObject("snippet").getString("title");
            this.channel = object.getJSONObject("snippet").getString("channelTitle");
            this.imageURL = object.getJSONObject("snippet").getJSONObject("thumbnails")
                    .getJSONObject("high").getString("url");
        } catch (JSONException e)
        {
            e.printStackTrace();
        }
    }

    public static ArrayList<YouTubeResult> fromJson(JSONArray jsonObjects)
    {
        ArrayList<YouTubeResult> resources = new ArrayList<>();
        for(int i=0; i< jsonObjects.length(); i++)
        {
            try {
                resources.add(new YouTubeResult(jsonObjects.getJSONObject(i)));
            } catch (JSONException e)
            {
                e.printStackTrace();
            }
        }
        return resources;
    }

    public String getVideoId()
    {
        return videoId;
    }
    public void setVideoId(String videoId)
    {
        this.videoId = videoId;
    }

    public String getDescription()
    {
        return description;
    }
    public void setDescription(String description)
    {
        this.description = description;
    }

    public String getTitle()
    {
        return title;
    }
    public void setTitle(String title)
    {
        this.title = title;
    }

    public String getChannel() { return channel; }

    public String getImageURL()
    {
        return imageURL;
    }

    public static class DownloadImageTask extends AsyncTask<String, Void, Bitmap>
    {
        ImageView bmView;

        public DownloadImageTask(ImageView bmIn) {
            bmView = bmIn;
        }

        @Override
        protected Bitmap doInBackground(String... urls) {
            String url = urls[0];
            Bitmap image = null;
            try {
                InputStream in = new java.net.URL(url).openStream();
                image = BitmapFactory.decodeStream(in);
            } catch (Exception e) {
                Log.e("Error", e.getMessage());
                e.printStackTrace();
            }
            return image;
        }

        protected void onPostExecute(Bitmap result) {
            bmView.setImageBitmap(result);
        }
    }
}
