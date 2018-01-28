package com.njromano.songyt

import android.app.NotificationManager
import android.app.Service
import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.support.design.widget.FloatingActionButton
import android.support.design.widget.Snackbar
import android.support.v4.app.NotificationManagerCompat
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.util.Log
import android.view.View
import android.view.Menu
import android.view.MenuItem
import android.widget.Button
import android.widget.CompoundButton
import android.widget.ProgressBar
import android.widget.Switch
import android.widget.TextView

import com.android.volley.AuthFailureError
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.VolleyError
import com.android.volley.toolbox.StringRequest

import org.json.JSONException
import org.json.JSONObject
import org.w3c.dom.Text

import java.io.UnsupportedEncodingException
import java.net.URLEncoder
import java.util.ArrayList
import java.util.HashMap

import kotlinx.android.synthetic.main.activity_main.*;

import android.provider.Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS

// TODO: create cohesive walk-through of user flow
// TODO: Make it pretty
// TODO: disable notifications from an action on the notification
// TODO: test notificationlistener
// TODO: clean code
// TODO: add Pandora
// TODO: add Pandora and Spotify to notifications
// TODO: share directly from the app

class MainActivity : AppCompatActivity() {
    private val TAG = this.javaClass.simpleName
    private val ACTION_LISTENER = "com.njromano.songyt.NOTIFICATION_LISTENER"
    private val ACTION_SERVICE = "com.njromano.songyt.NOTIFICATION_SERVICE"
    val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.hasExtra("error")) {
                hideLoading()
                Snackbar.make(songyt_status_text,
                        "Error: " + intent.getStringExtra("error"),
                        Snackbar.LENGTH_LONG)
                        .show()
            } else {
                songyt_status_text.setText(R.string.songyt_status_2)
                val request = getYoutubeInfo(
                        intent.getStringExtra("song_title"),
                        intent.getStringExtra("artist_name")
                )
                if(request != null) {
                    MySingleton.getInstance(applicationContext).addToRequestQueue(request)
                    Log.d(TAG, request.toString())
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val toolbar = findViewById<View>(R.id.toolbar) as Toolbar
        setSupportActionBar(toolbar)

        val notificationSwitch = findViewById<View>(R.id.notification_switch) as Switch
        notificationSwitch.isChecked = applicationContext
                .getSharedPreferences(getString(R.string.prefs), Context.MODE_PRIVATE)
                .getBoolean(getString(R.string.notification_preference), false)

        notificationSwitch.setOnCheckedChangeListener { compoundButton, b ->
            applicationContext.getSharedPreferences(getString(R.string.prefs), Context.MODE_PRIVATE)
                .edit()
                    .putBoolean(getString(R.string.notification_preference), b)
                    .apply()
            if (!b) {
                // delete current notification(s)
                val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                nm.cancelAll()
            }
        }

        // set onclick to look for a song
        var fab = findViewById<View>(R.id.fab) as FloatingActionButton
        fab.setOnClickListener {
            val i = Intent(ACTION_SERVICE)
            i.putExtra("command", "getSong")
            sendBroadcast(i)
            showLoading()
        }

        // register the notificationlistener so that we can pass information between
        // this activity and the listener
        val filter = IntentFilter()
        filter.addAction(ACTION_LISTENER)
        registerReceiver(receiver, filter)
    }

    override fun onResume() {
        super.onResume()
        hideLoading()
        checkIfNotificationsEnabled()
    }

    private fun showLoading() {
        songyt_status_text.visibility = View.INVISIBLE
        progressbar.visibility = View.VISIBLE
    }

    private fun hideLoading() {
        songyt_status_text.visibility = View.VISIBLE
        progressbar.visibility = View.INVISIBLE
    }

    private fun checkIfNotificationsEnabled() {
        val notificationsEnabled = Settings.Secure
                .getString(this.contentResolver, "enabled_notification_listeners")
                .contains(applicationContext.packageName)

        Log.d(TAG, "notificationsEnabled = " + notificationsEnabled.toString())

        if (!notificationsEnabled) {
            songyt_status_text.text = ""
            val snackBar = Snackbar.make(songyt_status_text,
                    R.string.please_enable_notifications,
                    Snackbar.LENGTH_INDEFINITE)
            snackBar.setAction(R.string.notification_settings_action) {
                startActivity(Intent(ACTION_NOTIFICATION_LISTENER_SETTINGS))
                snackBar.dismiss()
            }.show()
        } else {
            songyt_status_text.setText(R.string.songyt_status_1)
            Snackbar.make(songyt_status_text, R.string.songyt_hint_1, Snackbar.LENGTH_LONG).show()
        }
    }

    private fun getYoutubeInfo(song: String, artist: String): StringRequest? {
        var url = ""
        var volleyRequest: StringRequest
        try {
            url = ("https://www.googleapis.com/youtube/v3/search"
                    + "?part=snippet"
                    + "&q=" + URLEncoder.encode(song + " " + artist, "UTF-8")
                    + "&type=video"
                    + "&key=" + URLEncoder.encode(resources.getString(R.string.API_KEY), "UTF-8"))


            volleyRequest = StringRequest(Request.Method.GET, url,
                Response.Listener { response ->
                    Log.d(TAG, response)
                    try {
                        val jsonResponse = JSONObject(response)
                        val youtubeResults = ArrayList<YTResource>()
                        youtubeResults.addAll(YTResource.fromJson(jsonResponse.getJSONArray("items")))
                        if (youtubeResults.isEmpty()) {
                            val i = Intent(ACTION_LISTENER)
                            i.putExtra("error", "No matching songs on YouTube.")
                            sendBroadcast(i)
                        } else {
                            val result = "https://www.youtube.com/watch?v=" + youtubeResults[0].videoId
                            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(result)))
                            hideLoading()
                        }
                    } catch (e: JSONException) {
                        hideLoading()
                        e.printStackTrace()
                        Snackbar.make(songyt_status_text, R.string.JSON_ERROR, Snackbar.LENGTH_SHORT).show()
                    }
                },
                Response.ErrorListener { error ->
                    hideLoading()
                    error.printStackTrace()
                    Log.d(TAG, error.toString())
                    Snackbar.make(songyt_status_text, R.string.REQUEST_ERROR, Snackbar.LENGTH_LONG).show()
            })
            return volleyRequest
        } catch (e: UnsupportedEncodingException) {
            e.printStackTrace()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    override fun onDestroy() {
        super.onDestroy()

        unregisterReceiver(receiver)
    }
}