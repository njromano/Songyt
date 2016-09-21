package com.njromano.songyt;

/**
 * Created by Nick on 9/20/16.
 *
 * Song object class, thought to simply bind artists and titles
 */
public class Song {
    private String artist, title;

    public Song(String artist, String title)
    {
        this.artist = artist;
        this.title = title;
    }

    public String getArtist()
    {
        return this.artist;
    }
    public void setArtist(String artistIn)
    {
        this.artist = artistIn;
    }

    public String getTitle()
    {
        return this.title;
    }
    public void setTitle(String titleIn)
    {
        this.title = titleIn;
    }
}
