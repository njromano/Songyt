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
        Log.d(TAG, "NOTIFICATION RECEIVED: " + sbn.getPackageName() + " posted\n");
        Log.d(TAG, "-----------------Song  " + sbn.getNotification().extras.get("android.title") + "\n");
        Log.d(TAG, "---------------Artist  " + sbn.getNotification().extras.get("android.text") + "\n");
        Intent i = new Intent(ACTION_LISTENER);
        i.putExtra("notification_event", "onNotificationPosted: " + sbn.getPackageName() + "\n");
        sendBroadcast(i);
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn)
    {
        Log.d(TAG, "NOTIFICATION RECIEVED: " + sbn.getPackageName() + " removed\n");
        Intent i = new Intent(ACTION_LISTENER);
        i.putExtra("notification_event", "onNotificationRemoved: " + sbn.getPackageName() + "\n");
        sendBroadcast(i);
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
                for(StatusBarNotification sbn : NotificationListener.this.getActiveNotifications())
                {
                    if (sbn.getPackageName().equals("com.google.android.music"))
                    {
                        Intent i = new Intent(ACTION_LISTENER);
                        i.putExtra("song_title", sbn.getNotification().extras.get("android.title").toString());
                        i.putExtra("artist_name", sbn.getNotification().extras.get("android.text").toString());
                        sendBroadcast(i);
                        break;
                    }
                }
            }
        }
    }
}
