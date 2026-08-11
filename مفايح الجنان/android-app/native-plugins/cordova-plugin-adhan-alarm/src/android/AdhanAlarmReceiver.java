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

        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        PowerManager.WakeLock wl = null;
        if (pm != null) {
            wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "alkawthar:adhan");
            wl.acquire(120000);
        }

        try {
            Intent svc = new Intent(context, AdhanPlaybackService.class);
            svc.setAction(AdhanPlaybackService.ACTION_PLAY);
            svc.putExtra(AdhanAlarmScheduler.EXTRA_ID, intent.getIntExtra(AdhanAlarmScheduler.EXTRA_ID, 0));
            svc.putExtra(AdhanAlarmScheduler.EXTRA_TITLE, intent.getStringExtra(AdhanAlarmScheduler.EXTRA_TITLE));
            svc.putExtra(AdhanAlarmScheduler.EXTRA_TEXT, intent.getStringExtra(AdhanAlarmScheduler.EXTRA_TEXT));
            svc.putExtra(AdhanAlarmScheduler.EXTRA_SOUND, intent.getStringExtra(AdhanAlarmScheduler.EXTRA_SOUND));
            svc.putExtra(AdhanAlarmScheduler.EXTRA_PLAY, intent.getBooleanExtra(AdhanAlarmScheduler.EXTRA_PLAY, true));
            svc.putExtra(AdhanAlarmScheduler.EXTRA_KIND, intent.getStringExtra(AdhanAlarmScheduler.EXTRA_KIND));

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(svc);
            } else {
                context.startService(svc);
            }
        } finally {
            if (wl != null && wl.isHeld()) {
                try { wl.release(); } catch (Exception ignored) {}
            }
        }
    }
}
