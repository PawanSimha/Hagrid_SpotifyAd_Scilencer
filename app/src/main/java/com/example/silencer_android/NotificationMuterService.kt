package com.example.silencer_android

import android.app.Notification
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSession
import android.os.Build
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Advanced Muting Engine for Android (API 24+)
 * Targets: Spotify, YouTube, Chrome, YT Music
 * Refactored for: Background persistence and reliable mute/unmute cycle.
 */
class NotificationMuterService : NotificationListenerService() {

    private lateinit var audioManager: AudioManager
    private var cachedVolume: Int = -1
    private var isCurrentlyMutedByUs: Boolean = false
    
    // Track active media controllers to prevent memory leaks and handle intra-session changes
    private val controllerMap = mutableMapOf<String, MediaControllerRecord>()

    private val simulationReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                ACTION_SIMULATE_AD -> applyMute("Simulated Advertisement")
                ACTION_SIMULATE_TRACK -> applyRestore("Simulated Track")
            }
        }
    }

    private data class MediaControllerRecord(
        val controller: MediaController,
        val callback: MediaController.Callback
    )

    companion object {
        private const val TAG = "HagridEngine"
        private const val AD_DURATION_THRESHOLD_MS = 35000L // 35 seconds

        const val ACTION_SIMULATE_AD = "com.example.silencer_android.SIMULATE_AD"
        const val ACTION_SIMULATE_TRACK = "com.example.silencer_android.SIMULATE_TRACK"

        // Strict whitelist to prevent system-wide interference
        private val TARGET_PACKAGES = setOf(
            "com.spotify.music",
            "com.google.android.apps.youtube.music",
            "com.google.android.youtube",
            "com.android.chrome"
        )

        private val _sessionLogs = MutableStateFlow<List<String>>(emptyList())
        val sessionLogs: StateFlow<List<String>> = _sessionLogs

        private val _isServiceActive = MutableStateFlow(false)
        val isServiceActive: StateFlow<Boolean> = _isServiceActive

        private val _totalAdsMuted = MutableStateFlow(0)
        val totalAdsMuted: StateFlow<Int> = _totalAdsMuted

        // Tracking for charts (0-6 for Mon-Sun, 0-23 for hours)
        private val _dailyMutes = MutableStateFlow(FloatArray(7) { 0f })
        val dailyMutes: StateFlow<FloatArray> = _dailyMutes

        private val _hourlyMutes = MutableStateFlow(FloatArray(24) { 0f })
        val hourlyMutes: StateFlow<FloatArray> = _hourlyMutes

        private val _totalSecondsSaved = MutableStateFlow(0L)
        val totalSecondsSaved: StateFlow<Long> = _totalSecondsSaved

        private val _isEngineRunning = MutableStateFlow(false)
        val isEngineRunning: StateFlow<Boolean> = _isEngineRunning

        private val _recentMuteTitle = MutableStateFlow("None")
        val recentMuteTitle: StateFlow<String> = _recentMuteTitle

        fun setEngineState(enabled: Boolean) {
            _isEngineRunning.value = enabled
            addLog(if (enabled) "SYSTEM: Engine Started" else "SYSTEM: Engine Stopped")
        }

        fun addLog(message: String) {
            val time = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
            val entry = "[$time] $message"
            val current = _sessionLogs.value.toMutableList()
            current.add(0, entry)
            if (current.size > 20) current.removeAt(current.size - 1)
            _sessionLogs.value = current
        }

        private fun updateStats(title: String) {
            _totalAdsMuted.value += 1
            _recentMuteTitle.value = title
            
            // Update daily data (adjusting for Mon-Sun index)
            val calendar = java.util.Calendar.getInstance()
            val dayOfWeek = (calendar.get(java.util.Calendar.DAY_OF_WEEK) + 5) % 7 // Mon=0, Tue=1...
            val hourOfDay = calendar.get(java.util.Calendar.HOUR_OF_DAY)
            
            val daily = _dailyMutes.value.copyOf()
            daily[dayOfWeek] += 1f
            _dailyMutes.value = daily
            
            val hourly = _hourlyMutes.value.copyOf()
            hourly[hourOfDay] += 1f
            _hourlyMutes.value = hourly
        }
    }

    override fun onCreate() {
        super.onCreate()
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        _isServiceActive.value = true
        
        val filter = IntentFilter().apply {
            addAction(ACTION_SIMULATE_AD)
            addAction(ACTION_SIMULATE_TRACK)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(simulationReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(simulationReceiver, filter)
        }
    }

    /**
     * STEP 1: FIX SERVICE SLEEP (MAKE IT STICKY)
     * Tells Android to restart the service if killed due to low memory.
     */
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        if (!_isEngineRunning.value || sbn == null) return

        val pkg = sbn.packageName ?: ""
        if (!TARGET_PACKAGES.contains(pkg)) return

        val extras = sbn.notification.extras
        val token = extras.getParcelable<MediaSession.Token>(Notification.EXTRA_MEDIA_SESSION) ?: return

        // Register Dynamic Callback for this session if not already tracked
        if (!controllerMap.containsKey(pkg)) {
            registerMediaCallback(pkg, token)
        }
        
        // Initial evaluation
        val controller = controllerMap[pkg]?.controller
        evaluateMediaState(pkg, controller?.metadata)
    }

    private fun registerMediaCallback(pkg: String, token: MediaSession.Token) {
        try {
            val controller = MediaController(this, token)
            val callback = object : MediaController.Callback() {
                override fun onMetadataChanged(metadata: MediaMetadata?) {
                    Log.d(TAG, "Intra-session metadata change detected for $pkg")
                    evaluateMediaState(pkg, metadata)
                }

                override fun onSessionDestroyed() {
                    cleanupController(pkg)
                }
            }
            
            controller.registerCallback(callback)
            controllerMap[pkg] = MediaControllerRecord(controller, callback)
            Log.d(TAG, "Registered dynamic monitoring for: $pkg")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to register media callback for $pkg", e)
        }
    }

    private fun evaluateMediaState(pkg: String, metadata: MediaMetadata?) {
        if (metadata == null) return

        val title = metadata.getString(MediaMetadata.METADATA_KEY_TITLE) ?: ""
        val artist = metadata.getString(MediaMetadata.METADATA_KEY_ARTIST) ?: ""
        val duration = metadata.getLong(MediaMetadata.METADATA_KEY_DURATION)

        val isAdByKeyword = isAdKeyword(title, artist)
        val isAdByDuration = duration in 1..AD_DURATION_THRESHOLD_MS

        Log.d(TAG, "Evaluating $pkg: '$title' | Duration: ${duration}ms | isAd: ${isAdByKeyword || isAdByDuration}")

        if (isAdByKeyword || isAdByDuration) {
            applyMute(title.ifEmpty { "Advertisement" })
        } else if (isCurrentlyMutedByUs) {
            applyRestore(title.ifEmpty { "Track" })
        }
    }

    private fun isAdKeyword(title: String, artist: String): Boolean {
        val combined = "$title $artist".lowercase()
        val keywords = listOf("advertisement", "sponsored", "promoted", "ad •", "discover")
        
        if (title.equals("ad", ignoreCase = true) || title.equals("advertisement", ignoreCase = true)) return true
        
        return keywords.any { combined.contains(it) }
    }

    /**
     * STEP 2: FIX THE UNMUTE TRAP (EXPLICIT INDEX MANIPULATION)
     * To MUTE: Cache real volume, set stream to 0.
     */
    private fun applyMute(title: String) {
        try {
            if (isCurrentlyMutedByUs) return
            
            val currentVol = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
            
            // Only cache if the volume is actually above 0
            if (currentVol > 0) {
                cachedVolume = currentVol
            }

            // Explicitly set volume to 0 (Hide system UI with flag 0)
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 0, 0)
            
            isCurrentlyMutedByUs = true
            updateStats(title)
            addLog("MUTED: $title")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to apply mute", e)
        }
    }

    /**
     * STEP 2: FIX THE UNMUTE TRAP (EXPLICIT INDEX MANIPULATION)
     * To RESTORE: Restore from cache, reset flags.
     */
    private fun applyRestore(title: String) {
        try {
            if (!isCurrentlyMutedByUs) return
            
            // Restore via cached index (Hide system UI with flag 0)
            if (cachedVolume != -1) {
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, cachedVolume, 0)
            }
            
            isCurrentlyMutedByUs = false
            cachedVolume = -1
            addLog("RESTORED: $title")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to restore volume", e)
        }
    }

    private fun cleanupController(pkg: String) {
        controllerMap[pkg]?.let { record ->
            try {
                record.controller.unregisterCallback(record.callback)
                Log.d(TAG, "Cleaned up media controller for $pkg")
            } catch (e: Exception) {
                Log.e(TAG, "Cleanup failed for $pkg", e)
            }
        }
        controllerMap.remove(pkg)
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        val pkg = sbn?.packageName ?: return
        if (TARGET_PACKAGES.contains(pkg)) {
            cleanupController(pkg)
            if (isCurrentlyMutedByUs) {
                applyRestore("Session Ended")
            }
        }
    }

    override fun onDestroy() {
        // Explicitly unregister all callbacks to prevent memory leaks
        val keys = controllerMap.keys.toList()
        keys.forEach { cleanupController(it) }
        
        try {
            unregisterReceiver(simulationReceiver)
        } catch (e: Exception) {
            Log.e(TAG, "Receiver not registered", e)
        }
        
        _isServiceActive.value = false
        super.onDestroy()
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        _isServiceActive.value = true
        addLog("LISTENER: Connected")
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        _isServiceActive.value = false
        addLog("LISTENER: Disconnected")
    }
}
