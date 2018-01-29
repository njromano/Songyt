package com.njromano.songyt

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import android.widget.RemoteViews
import android.widget.Toast

import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest

import org.json.JSONException
import org.json.JSONObject

import java.lang.reflect.Field
import java.net.URLEncoder
import java.util.*

/**
 * Created by Nick on 9/20/16. -
 */
class SongScraper : NotificationListenerService() {
    private val TAG = this.javaClass.simpleName
    private val ACTION_LISTENER = "com.njromano.songyt.NOTIFICATION_LISTENER"
    private val ACTION_SERVICE = "com.njromano.songyt.NOTIFICATION_SERVICE"
    private var nlservicereceiver: NLServiceReceiver? = null
    private val mLastNotificationID: Int = 0

    private object MUSIC_PACKAGES {
        const val GPM = "com.google.android.music"
        const val SPOTIFY = "com.spotify.music"
        const val PANDORA = "com.pandora.android"
        const val IHEART = "com.clearchannel.iheartradio.controller"
        const val APPLE = "com.apple.android.music"
        const val AMAZON = "com.amazon.mp3"
        val SUPPORTED = arrayOf(GPM, SPOTIFY, PANDORA, IHEART, APPLE, AMAZON)
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "SongScraper created")
        nlservicereceiver = NLServiceReceiver()
        val filter = IntentFilter()
        filter.addAction(ACTION_SERVICE)
        registerReceiver(nlservicereceiver, filter)
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        Log.d(TAG, "onListenerConnected")
        val sbns = activeNotifications
        for (i in sbns.indices) {
            Log.d(TAG, "NOTIFICATION RECEIVED: " + sbns[i].packageName + "\n"
                    + sbns[i].notification.toString() + "\n")
        }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        super.onNotificationPosted(sbn)
        Log.d(TAG, "onNotificationPosted")
        Log.d(TAG, "NOTIFICATION POSTED: " + sbn.packageName + "\n"
                + sbn.notification.toString() + "\n"
                + sbn.notification.extras.toString() + "\n")

        if (sbn.packageName !in MUSIC_PACKAGES.SUPPORTED) // ignore it
            return

        // get notification preference
        val preferences = getSharedPreferences(getString(R.string.prefs), Context.MODE_PRIVATE)
        if (!preferences.getBoolean(getString(R.string.notification_preference), false))
            return


        val song = scrapeSongInfo(sbn)
        if (song == null) {
                hideNotification(mLastNotificationID)
        } else {
            queryYouTube(song)
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        super.onNotificationRemoved(sbn)
        Log.d(TAG, "onNotificationRemoved")
        Log.d(TAG, "NOTIFICATION REMOVED: "
                + sbn.packageName + "\n"
                + sbn.notification.toString() + "\n"
                + sbn.notification.extras.toString() + "\n")

        if (sbn.packageName !in MUSIC_PACKAGES.SUPPORTED) // ignore it
            return

        hideNotification(mLastNotificationID)
    }

    override fun onDestroy() {
        Log.d(TAG, "SongScraper destroyed")
        super.onDestroy()
        unregisterReceiver(nlservicereceiver)
    }

    internal inner class NLServiceReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.getStringExtra("command") == "getSong") {
                // search in current active notifications and broadcast the first result known to be
                // from a music application to the notification listener in MainActivity
                for (sbn in this@SongScraper.activeNotifications) {
                    val song = scrapeSongInfo(sbn)
                    if(song == null) {
                        val noSongIntent = Intent(ACTION_LISTENER)
                        noSongIntent.putExtra("error", "No song found to be playing.")
                        sendBroadcast(noSongIntent)
                    }
                    else {
                        val i = Intent(ACTION_LISTENER)
                        i.putExtra("song_title", song.first) // song
                        i.putExtra("artist_name", song.second) // title
                        sendBroadcast(i)
                    }
                }
            }
        }
    }
    // ------------ private functions ------------

    private fun hideNotification(notiID:Int) {
        val notificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(notiID)
    }

    @SuppressLint("NewApi")
    private fun showNotification(videoID: String, song:Pair<String, String>, notiID:Int) {
        val url = "https://www.youtube.com/watch?v=" + videoID
        val nManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // build a new notification
        if(Build.VERSION.SDK_INT > Build.VERSION_CODES.N) {
            NotificationChannel(
                    getString(R.string.notification_channel),
                    "Songyt",
                    NotificationManager.IMPORTANCE_DEFAULT)
        }
        val noti = android.support.v4.app.NotificationCompat
                .Builder(applicationContext, getString(R.string.notification_channel))
                    .setContentTitle(getString(R.string.SONG_FOUND))
                    .setContentText("Tap to watch top result for ${song.first} by ${song.second}")
                    .setTicker("Songyt")
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setAutoCancel(true)
                    .setContentIntent(
                        PendingIntent
                                .getActivity(applicationContext,
                                        0,
                                        Intent(Intent.ACTION_VIEW,
                                                Uri.parse(url)),
                                        PendingIntent.FLAG_UPDATE_CURRENT
                                )
                    )
                .build()
        nManager.notify(notiID, noti)
    }

    private fun scrapeSongInfo(noti: StatusBarNotification): Pair<String, String>? {
        if (noti.packageName !in MUSIC_PACKAGES.SUPPORTED)
            return null

        // implementations are subject to change with each update
        val songTitle: String
        val artistName: String
        when {
            noti.packageName == MUSIC_PACKAGES.GPM -> {

                // Look for songs being played by Google Play Music.
                // Notification extras:
                //  android.title - song title
                //  android.text - artist name

                songTitle = noti.notification.extras.get("android.title").toString().trim()
                artistName = noti.notification.extras.get("android.text").toString().trim()
                Log.d(TAG, "SONG FOUND: $artistName - $songTitle\n")
                if (songTitle.equals("") || artistName.equals(""))
                    return null
                return Pair(songTitle, artistName)

            }
            noti.packageName == MUSIC_PACKAGES.SPOTIFY -> {

                // Look for songs being played by Spotify
                // Notification tickerText:
                // "Song Title — Artist Name

                val tickerText = noti.notification.tickerText
                        .toString()
                        .split("—".toRegex())
                        .dropLastWhile { it.isEmpty() }.toTypedArray()
                songTitle = tickerText[0].trim()
                artistName = tickerText[1].trim()
                Log.d(TAG, "SONG FOUND: $artistName - $songTitle\n")
                if (songTitle == "" || artistName == "")
                    return null
                return Pair(songTitle, artistName)

            }
            noti.packageName == MUSIC_PACKAGES.PANDORA -> {

                // Look for songs being played by Pandora
                // Within RemoteViews, using Reflection

                Log.d(TAG, "PANDORA: " + noti.notification.extras.toString())
                try {
                    val remoteviews = noti.notification.bigContentView as RemoteViews
                    val pandoraClass = remoteviews.javaClass
                    val outerFields = pandoraClass.getDeclaredFields() as Array<Field>
                    for (i in 0 until outerFields.size) {
                        if (outerFields[i].name != "mActions") continue
                        outerFields[i].isAccessible = true
                        val actions = outerFields[i].get(remoteviews) as ArrayList<Object>

                        // get song title
                        val songField = actions[0].javaClass.getDeclaredField("value")
                        songField.isAccessible = true
                        songTitle = songField.get(actions[0]).toString().trim()

                        // get artist name
                        val artistField = actions[1].javaClass.getDeclaredField("value")
                        artistField.isAccessible = true
                        artistName = artistField.get(actions[1]).toString().trim()

                        Log.d(TAG, "SONG FOUND: $artistName - $songTitle\n")
                        if (songTitle == "" || artistName == "")
                            return null
                        return Pair(songTitle, artistName)
                    }
                }
                catch (e: Exception) {
                    e.printStackTrace()
                }
                return null

            }
            noti.packageName == MUSIC_PACKAGES.IHEART -> {

                // Look for songs being played by IHeartRadio
                try {
                    val remoteviews = noti.notification.bigContentView as RemoteViews
                    val iheartClass = remoteviews.javaClass
                    val outerFields = iheartClass.getDeclaredFields() as Array<Field>
                    for (i in 0 until outerFields.size) {
                        if (outerFields[i].name != "mActions") continue
                        outerFields[i].isAccessible = true
                        val actions = outerFields[i].get(remoteviews) as ArrayList<Object>

                        // get song title
                        val songField = actions[4].javaClass.getDeclaredField("value")
                        songField.isAccessible = true
                        songTitle = songField.get(actions[4]).toString().trim()

                        // get artist name
                        val artistField = actions[6].javaClass.getDeclaredField("value")
                        artistField.isAccessible = true
                        artistName = artistField.get(actions[6]).toString().trim()

                        Log.d(TAG, "SONG FOUND: $artistName - $songTitle\n")
                        if (songTitle == "" || artistName == "")
                            return null
                        return Pair(songTitle, artistName)
                    }
                }
                catch (e: Exception) {
                    e.printStackTrace()
                }
                return null
            }
            noti.packageName ==  MUSIC_PACKAGES.APPLE -> {

                // Look for songs being played by Apple Music

                Log.d(TAG, "APPLE: " + noti.notification.extras.toString())
                // TODO get Apple Music info
                return null
            }
            noti.packageName ==  MUSIC_PACKAGES.AMAZON -> {

                // Look for songs being played by Apple Music

                Log.d(TAG, "AMAZON: " + noti.notification.extras.toString())
                // TODO get Amazon Music info
                return null
            }
            else -> return null
        }
    }

    private fun queryYouTube(song: Pair<String,String>) {
        try {
            val url = ("https://www.googleapis.com/youtube/v3/search"
                    + "?part=snippet"
                    + "&q=" + URLEncoder.encode(song.first + " " + song.second, "UTF-8")
                    + "&type=video"
                    + "&key=" + URLEncoder.encode(
                    resources.getString(R.string.API_KEY),
                    "UTF-8")
                    )
            val r = StringRequest(Request.Method.GET, url,
                    Response.Listener { response ->
                        val ytresults = ArrayList<YouTubeResult>()
                        Log.d(TAG, response)
                        try {
                            val jsonResponse = JSONObject(response)
                            ytresults.clear()
                            ytresults.addAll(
                                    YouTubeResult.fromJson(jsonResponse.getJSONArray("items"))
                            )
                            if (ytresults.isEmpty()) {
                                val i = Intent(ACTION_LISTENER)
                                i.putExtra("error", "No matching songs on YouTube.")
                                sendBroadcast(i)
                            } else {
                                showNotification(ytresults[0].videoId, song, mLastNotificationID)
                            }
                        } catch (e: JSONException) {
                            e.printStackTrace()
                            Toast.makeText(baseContext,
                                    "Sorry, an error has occurred with Songyt.",
                                    Toast.LENGTH_SHORT).show()
                        }
                    }, Response.ErrorListener { error ->
                error.printStackTrace()
                Log.d(TAG, error.toString())
                Toast.makeText(baseContext,
                        getString(R.string.ERROR_CHECK_CONNECTION),
                        Toast.LENGTH_SHORT).show()
            })

            VolleyRequestQueue.getInstance(applicationContext).addToRequestQueue(r)

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
