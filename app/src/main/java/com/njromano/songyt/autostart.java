package com.njromano.songyt;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * Created by Nick on 1/28/18.
 */

public class autostart extends BroadcastReceiver
{
    public void onReceive(Context context, Intent arg1)
    {
        Intent intent = new Intent(context, SongScraper.class);
        context.startService(intent);
        Log.i("autostart", "started");
    }
}