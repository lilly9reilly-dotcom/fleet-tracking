package com.mafatih.aljinan.adhan;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;

import androidx.core.app.NotificationCompat;

import java.io.IOException;

public class AdhanPlaybackService extends Service {
    public static final String ACTION_PLAY = "com.mafatih.aljinan.adhan.PLAY";
    public static final String ACTION_STOP = "com.mafatih.aljinan.adhan.STOP";
    public static final String CHANNEL_ALARM = "alkawthar_adhan_alarm_v2";
    public static final String CHANNEL_SOFT = "alkawthar_adhan_soft_v2";
    private static final int NOTIF_ID = 77001;

    private MediaPlayer player;
    private PowerManager.WakeLock wakeLock;
    private AudioManager audioManager;
    private AudioFocusRequest focusRequest;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) {
            stopSelf();
            return START_NOT_STICKY;
        }
        String action = intent.getAction();
        if (ACTION_STOP.equals(action)) {
            stopPlaybackAndSelf();
            return START_NOT_STICKY;
        }

        String title = intent.getStringExtra(AdhanAlarmScheduler.EXTRA_TITLE);
        String text = intent.getStringExtra(AdhanAlarmScheduler.EXTRA_TEXT);
        String sound = intent.getStringExtra(AdhanAlarmScheduler.EXTRA_SOUND);
        boolean play = intent.getBooleanExtra(AdhanAlarmScheduler.EXTRA_PLAY, true);
        String kind = intent.getStringExtra(AdhanAlarmScheduler.EXTRA_KIND);
        if (title == null || title.isEmpty()) title = "الكوثر — منبّه الأذان";
        if (text == null) text = "حان وقت الصلاة";
        boolean soft = "pre".equals(kind) || "layl".equals(kind) || "event".equals(kind) || "dua".equals(kind);

        ensureChannels();
        Notification notification = buildNotification(title, text, soft, intent);
        if (Build.VERSION.SDK_INT >= 29) {
            startForeground(NOTIF_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK);
        } else {
            startForeground(NOTIF_ID, notification);
        }

        acquireWakeLock();
        requestAlarmFocus();
        if (play) {
            startAudio(sound);
        } else {
            new android.os.Handler(getMainLooper()).postDelayed(this::stopPlaybackAndSelf, 8000);
        }
        return START_STICKY;
    }

    private void ensureChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;
        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm == null) return;

        NotificationChannel alarm = new NotificationChannel(
                CHANNEL_ALARM,
                "منبّه الأذان — الكوثر",
                NotificationManager.IMPORTANCE_HIGH
        );
        alarm.setDescription("أذان الصلاة حتى والتطبيق مغلق");
        alarm.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
        alarm.enableVibration(true);
        alarm.setBypassDnd(true);
        alarm.setSound(null, null);
        nm.createNotificationChannel(alarm);

        NotificationChannel soft = new NotificationChannel(
                CHANNEL_SOFT,
                "تذكير الصلاة — الكوثر",
                NotificationManager.IMPORTANCE_HIGH
        );
        soft.setDescription("تذكيرات قبل الأذان وصلاة الليل");
        soft.setSound(null, null);
        soft.enableVibration(true);
        nm.createNotificationChannel(soft);
    }

    private Notification buildNotification(String title, String text, boolean soft, Intent src) {
        Intent full = new Intent(this, AdhanAlarmActivity.class);
        full.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        if (src != null) full.putExtras(src);

        int piFlags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            piFlags |= PendingIntent.FLAG_IMMUTABLE;
        }
        PendingIntent fullPi = PendingIntent.getActivity(this, 1, full, piFlags);

        Intent stopIntent = new Intent(this, AdhanAlarmReceiver.class);
        stopIntent.setAction(ACTION_STOP);
        PendingIntent stopPi = PendingIntent.getBroadcast(this, 99, stopIntent, piFlags);

        NotificationCompat.Builder b = new NotificationCompat.Builder(this, soft ? CHANNEL_SOFT : CHANNEL_ALARM)
                .setContentTitle(title)
                .setContentText(text)
                .setSmallIcon(android.R.drawable.ic_lock_silent_mode_off)
                .setContentIntent(fullPi)
                .setFullScreenIntent(fullPi, true)
                .setOngoing(true)
                .setAutoCancel(false)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setDefaults(NotificationCompat.DEFAULT_VIBRATE | NotificationCompat.DEFAULT_LIGHTS)
                .addAction(0, "إيقاف", stopPi);

        return b.build();
    }

    private void acquireWakeLock() {
        try {
            PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
            if (pm == null) return;
            wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "alkawthar:adhan_play");
            wakeLock.acquire(20 * 60 * 1000L);
        } catch (Exception ignored) {}
    }

    private void requestAlarmFocus() {
        try {
            audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
            if (audioManager == null) return;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                AudioAttributes attrs = new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build();
                focusRequest = new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                        .setAudioAttributes(attrs)
                        .build();
                audioManager.requestAudioFocus(focusRequest);
            } else {
                audioManager.requestAudioFocus(null, AudioManager.STREAM_ALARM, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK);
            }
        } catch (Exception ignored) {}
    }

    private void abandonFocus() {
        try {
            if (audioManager == null) return;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && focusRequest != null) {
                audioManager.abandonAudioFocusRequest(focusRequest);
            } else {
                audioManager.abandonAudioFocus(null);
            }
        } catch (Exception ignored) {}
    }

    private void startAudio(String soundAsset) {
        stopPlayer();
        String path = normalizeAssetPath(soundAsset);
        if (!tryPlayAsset(path)) {
            if (!tryPlayAsset("www/audio/adhan/default.mp3")) {
                String listed = firstAdhanAsset();
                if (listed == null || !tryPlayAsset(listed)) {
                    stopPlaybackAndSelf();
                }
            }
        }
    }

    private boolean tryPlayAsset(String path) {
        if (path == null || path.isEmpty()) return false;
        AssetFileDescriptor afd = null;
        try {
            player = new MediaPlayer();
            player.setWakeMode(getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                player.setAudioAttributes(new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build());
            } else {
                player.setAudioStreamType(AudioManager.STREAM_ALARM);
            }
            afd = getAssets().openFd(path);
            player.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
            afd.close();
            afd = null;
            player.setOnCompletionListener(mp -> stopPlaybackAndSelf());
            player.setOnErrorListener((mp, what, extra) -> {
                stopPlaybackAndSelf();
                return true;
            });
            player.setVolume(1f, 1f);
            player.prepare();
            player.start();
            return true;
        } catch (Exception e) {
            if (afd != null) {
                try { afd.close(); } catch (IOException ignored) {}
            }
            stopPlayer();
            return false;
        }
    }

    private String firstAdhanAsset() {
        try {
            AssetManager am = getAssets();
            String[] names = am.list("www/audio/adhan");
            if (names == null) return null;
            for (String n : names) {
                if (n != null && n.toLowerCase().endsWith(".mp3")) {
                    return "www/audio/adhan/" + n;
                }
            }
        } catch (Exception ignored) {}
        return null;
    }

    static String normalizeAssetPath(String soundAsset) {
        if (soundAsset == null || soundAsset.trim().isEmpty()) {
            return "www/audio/adhan/default.mp3";
        }
        String p = soundAsset.trim().replace('\\', '/');
        if (p.startsWith("/")) p = p.substring(1);
        if (!p.startsWith("www/")) {
            p = "www/" + p;
        }
        return p;
    }

    private void stopPlayer() {
        if (player != null) {
            try { if (player.isPlaying()) player.stop(); } catch (Exception ignored) {}
            try { player.release(); } catch (Exception ignored) {}
            player = null;
        }
    }

    private void stopPlaybackAndSelf() {
        stopPlayer();
        abandonFocus();
        try {
            NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm != null) nm.cancel(NOTIF_ID);
        } catch (Exception ignored) {}
        if (wakeLock != null && wakeLock.isHeld()) {
            try { wakeLock.release(); } catch (Exception ignored) {}
            wakeLock = null;
        }
        stopForeground(true);
        stopSelf();
    }

    @Override
    public void onDestroy() {
        stopPlayer();
        abandonFocus();
        if (wakeLock != null && wakeLock.isHeld()) {
            try { wakeLock.release(); } catch (Exception ignored) {}
        }
        super.onDestroy();
    }
}
