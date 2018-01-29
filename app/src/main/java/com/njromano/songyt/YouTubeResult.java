package com.njromano.songyt;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

/**
 * Created by Nick on 9/21/16.
 * Class for YouTube Resources to be pulled from JSON
 */
public class YouTubeResult {
    private String videoId, description, title;

    public YouTubeResult(JSONObject object)
    {
        try {
            this.videoId = object.getJSONObject("id").getString("videoId");
            this.description = object.getJSONObject("snippet").getString("description");
            this.title = object.getJSONObject("snippet").getString("title");
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
}
