package com.mafatih.aljinan.adhan;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public final class AdhanAlarmStore {
    private static final String PREFS = "alkawthar_adhan_alarms";
    private static final String KEY_ALARMS = "alarms_json";
    private static final String KEY_ENABLED = "enabled";

    private AdhanAlarmStore() {}

    private static SharedPreferences prefs(Context ctx) {
        return ctx.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public static void saveAlarms(Context ctx, JSONArray alarms) {
        prefs(ctx).edit()
                .putString(KEY_ALARMS, alarms != null ? alarms.toString() : "[]")
                .putBoolean(KEY_ENABLED, alarms != null && alarms.length() > 0)
                .apply();
    }

    public static JSONArray loadAlarms(Context ctx) {
        String raw = prefs(ctx).getString(KEY_ALARMS, "[]");
        try {
            return new JSONArray(raw);
        } catch (JSONException e) {
            return new JSONArray();
        }
    }

    public static boolean isEnabled(Context ctx) {
        return prefs(ctx).getBoolean(KEY_ENABLED, false);
    }

    public static void clear(Context ctx) {
        prefs(ctx).edit().putString(KEY_ALARMS, "[]").putBoolean(KEY_ENABLED, false).apply();
    }

    public static JSONObject findById(Context ctx, int id) {
        JSONArray arr = loadAlarms(ctx);
        for (int i = 0; i < arr.length(); i++) {
            JSONObject o = arr.optJSONObject(i);
            if (o != null && o.optInt("id", -1) == id) {
                return o;
            }
        }
        return null;
    }
}
