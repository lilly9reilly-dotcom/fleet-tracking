package com.mafatih.aljinan.adhan;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.PowerManager;

public class AdhanAlarmReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null) return;
        String action = intent.getAction();
        if (action == null) return;

        if (AdhanPlaybackService.ACTION_STOP.equals(action)) {
            Intent stop = new Intent(context, AdhanPlaybackService.class);
            stop.setAction(AdhanPlaybackService.ACTION_STOP);
            try {
                context.startService(stop);
            } catch (Exception ignored) {}
            return;
        }

        if (!AdhanAlarmScheduler.ACTION_FIRE.equals(action)) return;

        // Keep the process alive until the foreground service / activity are started.
        final PendingResult pending = goAsync();
        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        final PowerManager.WakeLock wl;
        if (pm != null) {
            wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "alkawthar:adhan_fire");
            wl.acquire(180000);
        } else {
            wl = null;
        }

        final Context app = context.getApplicationContext();
        final int alarmId = intent.getIntExtra(AdhanAlarmScheduler.EXTRA_ID, 0);
        final String title = intent.getStringExtra(AdhanAlarmScheduler.EXTRA_TITLE);
        final String text = intent.getStringExtra(AdhanAlarmScheduler.EXTRA_TEXT);
        final String sound = intent.getStringExtra(AdhanAlarmScheduler.EXTRA_SOUND);
        final boolean play = intent.getBooleanExtra(AdhanAlarmScheduler.EXTRA_PLAY, true);
        final String kind = intent.getStringExtra(AdhanAlarmScheduler.EXTRA_KIND);

        new Thread(() -> {
            try {
                Intent svc = new Intent(app, AdhanPlaybackService.class);
                svc.setAction(AdhanPlaybackService.ACTION_PLAY);
                svc.putExtra(AdhanAlarmScheduler.EXTRA_ID, alarmId);
                svc.putExtra(AdhanAlarmScheduler.EXTRA_TITLE, title);
                svc.putExtra(AdhanAlarmScheduler.EXTRA_TEXT, text);
                svc.putExtra(AdhanAlarmScheduler.EXTRA_SOUND, sound);
                svc.putExtra(AdhanAlarmScheduler.EXTRA_PLAY, play);
                svc.putExtra(AdhanAlarmScheduler.EXTRA_KIND, kind);

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    app.startForegroundService(svc);
                } else {
                    app.startService(svc);
                }

                // Full-screen activity over lock screen — works even if WebView is dead.
                if (play || "adhan".equals(kind)) {
                    Intent ui = new Intent(app, AdhanAlarmActivity.class);
                    ui.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                            | Intent.FLAG_ACTIVITY_CLEAR_TOP
                            | Intent.FLAG_ACTIVITY_NO_USER_ACTION);
                    ui.putExtra(AdhanAlarmScheduler.EXTRA_ID, alarmId);
                    ui.putExtra(AdhanAlarmScheduler.EXTRA_TITLE, title);
                    ui.putExtra(AdhanAlarmScheduler.EXTRA_TEXT, text);
                    ui.putExtra(AdhanAlarmScheduler.EXTRA_SOUND, sound);
                    ui.putExtra(AdhanAlarmScheduler.EXTRA_PLAY, play);
                    ui.putExtra(AdhanAlarmScheduler.EXTRA_KIND, kind);
                    try {
                        app.startActivity(ui);
                    } catch (Exception ignored) {}
                }

                try {
                    AdhanAlarmScheduler.rollForwardFired(app, alarmId);
                } catch (Exception ignored) {}
            } finally {
                if (wl != null && wl.isHeld()) {
                    try { wl.release(); } catch (Exception ignored) {}
                }
                try { pending.finish(); } catch (Exception ignored) {}
            }
        }, "alkawthar-adhan-fire").start();
    }
}
