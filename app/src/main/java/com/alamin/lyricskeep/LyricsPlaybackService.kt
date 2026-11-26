package com.alamin.lyricskeep

import android.app.*
import android.content.Intent
import android.media.MediaPlayer
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.support.v4.media.MediaMetadataCompat

class LyricsPlaybackService : Service() {
    private var player: MediaPlayer? = null
    private var queuePaths = arrayListOf<String>()
    private var queueTitles = arrayListOf<String>()
    private var queueIds = longArrayOf()
    private var index = 0
    private var playing = false
    private var duration = 0
    private var current = 0
    private lateinit var session: MediaSessionCompat
    private val handler = Handler(Looper.getMainLooper())
    private val progressTask = object : Runnable {
        override fun run() {
            player?.let { runCatching { current = it.currentPosition; duration = it.duration } }
            broadcastState()
            if (playing) handler.postDelayed(this, 500)
        }
    }

    override fun onCreate() {
        super.onCreate()
        createChannel()
        session = MediaSessionCompat(this, "LyricsKeepPlayback").apply {
            setCallback(object : MediaSessionCompat.Callback() {
                override fun onPlay() = resume()
                override fun onPause() = pause()
                override fun onSkipToNext() = next()
                override fun onSkipToPrevious() = previous()
                override fun onStop() = stopPlayback()
                override fun onSeekTo(pos: Long) { seek(pos.toInt()) }
            })
            isActive = true
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PLAY -> {
                queuePaths = intent.getStringArrayListExtra(EXTRA_PATHS) ?: arrayListOf()
                queueTitles = intent.getStringArrayListExtra(EXTRA_TITLES) ?: arrayListOf()
                queueIds = intent.getLongArrayExtra(EXTRA_IDS) ?: longArrayOf()
                index = intent.getIntExtra(EXTRA_INDEX, 0).coerceIn(0, (queuePaths.size - 1).coerceAtLeast(0))
                startForeground(NOTIFICATION_ID, buildNotification())
                playIndex()
            }
            ACTION_TOGGLE -> if (playing) pause() else resume()
            ACTION_NEXT -> next()
            ACTION_PREVIOUS -> previous()
            ACTION_STOP -> stopPlayback()
            ACTION_SEEK -> seek(intent.getIntExtra(EXTRA_POSITION, 0))
        }
        return START_STICKY
    }

    private fun playIndex() {
        if (queuePaths.isEmpty() || index !in queuePaths.indices) { stopPlayback(); return }
        runCatching { player?.release() }
        playing = false; current = 0; duration = 0
        val mp = MediaPlayer()
        player = mp
        runCatching {
            mp.setOnPreparedListener {
                duration = it.duration
                it.start()
                playing = true
                updateSession()
                updateNotification()
                broadcastState()
                handler.removeCallbacks(progressTask)
                handler.post(progressTask)
            }
            mp.setOnCompletionListener { next() }
            mp.setOnErrorListener { _, _, _ -> playing = false; updateNotification(); broadcastState(); true }
            mp.setDataSource(queuePaths[index])
            updateSession()
            updateNotification()
            mp.prepareAsync()
        }.onFailure { stopPlayback() }
    }

    private fun resume() {
        val mp = player
        if (mp == null) { playIndex(); return }
        runCatching { mp.start(); playing = true; handler.removeCallbacks(progressTask); handler.post(progressTask) }
        updateSession(); updateNotification(); broadcastState()
    }

    private fun pause() {
        runCatching { player?.pause() }
        playing = false
        handler.removeCallbacks(progressTask)
        updateSession(); updateNotification(); broadcastState()
    }

    private fun next() {
        if (queuePaths.isEmpty()) return
        index = (index + 1) % queuePaths.size
        playIndex()
    }

    private fun previous() {
        if (queuePaths.isEmpty()) return
        index = if (current > 3000) 0 else (index - 1 + queuePaths.size) % queuePaths.size
        playIndex()
    }

    private fun seek(position: Int) {
        runCatching { player?.seekTo(position.coerceIn(0, duration)) }
        current = position.coerceIn(0, duration)
        broadcastState()
    }

    private fun stopPlayback() {
        playing = false
        handler.removeCallbacks(progressTask)
        runCatching { player?.stop(); player?.release() }
        player = null
        session.setPlaybackState(PlaybackStateCompat.Builder().setState(PlaybackStateCompat.STATE_STOPPED, 0, 0f).build())
        broadcastState()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun updateSession() {
        val title = queueTitles.getOrNull(index) ?: "LyricsKeep"
        session.setMetadata(MediaMetadataCompat.Builder()
            .putString(MediaMetadataCompat.METADATA_KEY_TITLE, title)
            .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, duration.toLong()).build())
        val state = if (playing) PlaybackStateCompat.STATE_PLAYING else PlaybackStateCompat.STATE_PAUSED
        session.setPlaybackState(PlaybackStateCompat.Builder()
            .setActions(PlaybackStateCompat.ACTION_PLAY or PlaybackStateCompat.ACTION_PAUSE or PlaybackStateCompat.ACTION_PLAY_PAUSE or PlaybackStateCompat.ACTION_SKIP_TO_NEXT or PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS or PlaybackStateCompat.ACTION_SEEK_TO or PlaybackStateCompat.ACTION_STOP)
            .setState(state, current.toLong(), if (playing) 1f else 0f).build())
    }

    private fun buildNotification(): android.app.Notification {
        val title = queueTitles.getOrNull(index) ?: "LyricsKeep Player"
        val open = Intent(this, MainActivity::class.java)
        val content = PendingIntent.getActivity(this, 10, open, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        fun action(code: String, request: Int): PendingIntent {
            val i = Intent(this, LyricsPlaybackService::class.java).setAction(code)
            return PendingIntent.getService(this, request, i, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        }
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle(title).setContentText("LyricsKeep • Music player")
            .setContentIntent(content).setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOnlyAlertOnce(true).setOngoing(playing)
            .addAction(android.R.drawable.ic_media_previous, "Previous", action(ACTION_PREVIOUS, 11))
            .addAction(if (playing) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play,
                if (playing) "Pause" else "Play", action(ACTION_TOGGLE, 12))
            .addAction(android.R.drawable.ic_media_next, "Next", action(ACTION_NEXT, 13))
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Close", action(ACTION_STOP, 14))
            .setStyle(androidx.media.app.NotificationCompat.MediaStyle()
                .setMediaSession(session.sessionToken).setShowActionsInCompactView(0, 1, 2))
            .build()
    }

    private fun updateNotification() {
        updateSession()
        val notification = buildNotification()
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, notification)
    }

    private fun broadcastState() {
        val i = Intent(ACTION_STATE).setPackage(packageName)
            .putExtra(EXTRA_ID, queueIds.getOrNull(index) ?: -1L)
            .putExtra(EXTRA_INDEX, index).putExtra(EXTRA_PLAYING, playing)
            .putExtra(EXTRA_DURATION, duration).putExtra(EXTRA_POSITION, current)
        sendBroadcast(i)
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            val channel = NotificationChannel(CHANNEL_ID, "LyricsKeep Music", NotificationManager.IMPORTANCE_LOW)
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null
    override fun onDestroy() {
        handler.removeCallbacks(progressTask)
        runCatching { player?.release() }
        session.release()
        super.onDestroy()
    }

    companion object {
        const val ACTION_PLAY = "com.alamin.lyricskeep.PLAY"
        const val ACTION_TOGGLE = "com.alamin.lyricskeep.TOGGLE"
        const val ACTION_NEXT = "com.alamin.lyricskeep.NEXT"
        const val ACTION_PREVIOUS = "com.alamin.lyricskeep.PREVIOUS"
        const val ACTION_STOP = "com.alamin.lyricskeep.STOP"
        const val ACTION_SEEK = "com.alamin.lyricskeep.SEEK"
        const val ACTION_STATE = "com.alamin.lyricskeep.STATE"
        const val EXTRA_PATHS = "paths"
        const val EXTRA_TITLES = "titles"
        const val EXTRA_IDS = "ids"
        const val EXTRA_INDEX = "index"
        const val EXTRA_ID = "id"
        const val EXTRA_PLAYING = "playing"
        const val EXTRA_DURATION = "duration"
        const val EXTRA_POSITION = "position"
        private const val CHANNEL_ID = "lyricskeep_music"
        private const val NOTIFICATION_ID = 7021
    }
}
