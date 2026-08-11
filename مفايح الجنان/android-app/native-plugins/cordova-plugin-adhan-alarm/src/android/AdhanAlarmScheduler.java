package com.mafatih.aljinan.adhan;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import org.json.JSONArray;
import org.json.JSONObject;

public final class AdhanAlarmScheduler {
    public static final String ACTION_FIRE = "com.mafatih.aljinan.adhan.FIRE";
    public static final String EXTRA_ID = "alarm_id";
    public static final String EXTRA_TITLE = "title";
    public static final String EXTRA_TEXT = "text";
    public static final String EXTRA_SOUND = "sound_asset";
    public static final String EXTRA_PLAY = "play_sound";
    public static final String EXTRA_KIND = "kind";

    private AdhanAlarmScheduler() {}

    private static AlarmManager am(Context ctx) {
        return (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
    }

    private static PendingIntent pending(Context ctx, int id, JSONObject alarm) {
        Intent intent = new Intent(ctx, AdhanAlarmReceiver.class);
        intent.setAction(ACTION_FIRE);
        intent.putExtra(EXTRA_ID, id);
        if (alarm != null) {
            intent.putExtra(EXTRA_TITLE, alarm.optString("title", "الكوثر"));
            intent.putExtra(EXTRA_TEXT, alarm.optString("text", "حان وقت الصلاة"));
            intent.putExtra(EXTRA_SOUND, alarm.optString("soundAsset", ""));
            intent.putExtra(EXTRA_PLAY, alarm.optBoolean("playSound", true));
            intent.putExtra(EXTRA_KIND, alarm.optString("kind", "adhan"));
        }
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }
        return PendingIntent.getBroadcast(ctx, id, intent, flags);
    }

    public static void cancelAll(Context ctx) {
        JSONArray existing = AdhanAlarmStore.loadAlarms(ctx);
        AlarmManager manager = am(ctx);
        for (int i = 0; i < existing.length(); i++) {
            JSONObject o = existing.optJSONObject(i);
            if (o == null) continue;
            int id = o.optInt("id", -1);
            if (id < 0) continue;
            PendingIntent pi = pending(ctx, id, o);
            manager.cancel(pi);
            pi.cancel();
        }
        AdhanAlarmStore.clear(ctx);
    }

    public static void scheduleAll(Context ctx, JSONArray alarms) {
        cancelAll(ctx);
        if (alarms == null || alarms.length() == 0) {
            return;
        }
        AlarmManager manager = am(ctx);
        long now = System.currentTimeMillis();
        JSONArray saved = new JSONArray();
        for (int i = 0; i < alarms.length(); i++) {
            JSONObject o = alarms.optJSONObject(i);
            if (o == null) continue;
            int id = o.optInt("id", -1);
            long at = o.optLong("at", 0);
            if (id < 0 || at <= now + 2000) continue;
            PendingIntent pi = pending(ctx, id, o);
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    AlarmManager.AlarmClockInfo info = new AlarmManager.AlarmClockInfo(at, pi);
                    manager.setAlarmClock(info, pi);
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi);
                } else {
                    manager.setExact(AlarmManager.RTC_WAKEUP, at, pi);
                }
                saved.put(o);
            } catch (SecurityException se) {
                saved.put(o);
            } catch (Exception ignored) {
            }
        }
        AdhanAlarmStore.saveAlarms(ctx, saved);
    }

    public static void rescheduleStored(Context ctx) {
        if (!AdhanAlarmStore.isEnabled(ctx)) return;
        JSONArray alarms = AdhanAlarmStore.loadAlarms(ctx);
        if (alarms.length() == 0) return;
        scheduleAll(ctx, alarms);
    }

    public static boolean canScheduleExact(Context ctx) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true;
        AlarmManager manager = am(ctx);
        return manager != null && manager.canScheduleExactAlarms();
    }
}
