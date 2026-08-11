package com.mafatih.aljinan.adhan;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.PowerManager;
import android.provider.Settings;

import androidx.core.content.ContextCompat;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class AdhanAlarmPlugin extends CordovaPlugin {
    private static final int REQ_POST_NOTIF = 48121;
    private CallbackContext permissionCallback;

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        if ("schedule".equals(action)) {
            final JSONArray alarms = args.optJSONArray(0);
            cordova.getThreadPool().execute(() -> {
                try {
                    AdhanAlarmScheduler.scheduleAll(cordova.getActivity(), alarms != null ? alarms : new JSONArray());
                    callbackContext.success();
                } catch (Exception e) {
                    callbackContext.error(e.getMessage() != null ? e.getMessage() : "schedule failed");
                }
            });
            return true;
        }
        if ("cancelAll".equals(action)) {
            cordova.getThreadPool().execute(() -> {
                AdhanAlarmScheduler.cancelAll(cordova.getActivity());
                callbackContext.success();
            });
            return true;
        }
        if ("canScheduleExactAlarms".equals(action)) {
            boolean ok = AdhanAlarmScheduler.canScheduleExact(cordova.getActivity());
            callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.OK, ok));
            return true;
        }
        if ("isIgnoringBatteryOptimizations".equals(action)) {
            callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.OK, isIgnoringBattery()));
            return true;
        }
        if ("openExactAlarmSettings".equals(action)) {
            openExactAlarmSettings();
            callbackContext.success();
            return true;
        }
        if ("openBatterySettings".equals(action)) {
            openBatterySettings();
            callbackContext.success();
            return true;
        }
        if ("stopPlayback".equals(action)) {
            Intent stop = new Intent(cordova.getActivity(), AdhanPlaybackService.class);
            stop.setAction(AdhanPlaybackService.ACTION_STOP);
            cordova.getActivity().startService(stop);
            callbackContext.success();
            return true;
        }
        if ("requestPermissions".equals(action)) {
            requestAllPermissions(callbackContext);
            return true;
        }
        return false;
    }

    private boolean isIgnoringBattery() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return true;
        PowerManager pm = (PowerManager) cordova.getActivity().getSystemService(Activity.POWER_SERVICE);
        if (pm == null) return true;
        return pm.isIgnoringBatteryOptimizations(cordova.getActivity().getPackageName());
    }

    private void openExactAlarmSettings() {
        Activity a = cordova.getActivity();
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                Intent i = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
                i.setData(Uri.parse("package:" + a.getPackageName()));
                a.startActivity(i);
                return;
            }
        } catch (Exception ignored) {}
        openAppDetails();
    }

    private void openBatterySettings() {
        Activity a = cordova.getActivity();
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Intent i = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                i.setData(Uri.parse("package:" + a.getPackageName()));
                a.startActivity(i);
                return;
            }
        } catch (Exception ignored) {}
        try {
            Intent i = new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS);
            a.startActivity(i);
        } catch (Exception e) {
            openAppDetails();
        }
    }

    private void openAppDetails() {
        Activity a = cordova.getActivity();
        Intent i = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        i.setData(Uri.parse("package:" + a.getPackageName()));
        a.startActivity(i);
    }

    private void requestAllPermissions(CallbackContext callbackContext) {
        permissionCallback = callbackContext;
        Activity a = cordova.getActivity();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !isIgnoringBattery()) {
            try {
                Intent i = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                i.setData(Uri.parse("package:" + a.getPackageName()));
                a.startActivity(i);
            } catch (Exception ignored) {}
        }

        if (Build.VERSION.SDK_INT >= 33) {
            if (ContextCompat.checkSelfPermission(a, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                cordova.requestPermission(this, REQ_POST_NOTIF, Manifest.permission.POST_NOTIFICATIONS);
                return;
            }
        }

        if (!AdhanAlarmScheduler.canScheduleExact(a)) {
            openExactAlarmSettings();
        }

        sendPermissionResult(true);
    }

    private void sendPermissionResult(boolean notifOk) {
        if (permissionCallback == null) return;
        try {
            JSONObject o = new JSONObject();
            o.put("notifications", notifOk);
            o.put("exactAlarms", AdhanAlarmScheduler.canScheduleExact(cordova.getActivity()));
            o.put("batteryUnrestricted", isIgnoringBattery());
            permissionCallback.success(o);
        } catch (JSONException e) {
            permissionCallback.success();
        }
        permissionCallback = null;
    }

    @Override
    public void onRequestPermissionResult(int requestCode, String[] permissions, int[] grantResults) throws JSONException {
        if (requestCode != REQ_POST_NOTIF) return;
        boolean granted = grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED;
        if (!AdhanAlarmScheduler.canScheduleExact(cordova.getActivity())) {
            openExactAlarmSettings();
        }
        sendPermissionResult(granted);
    }
}
