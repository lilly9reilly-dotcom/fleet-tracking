package com.mafatih.aljinan.adhan;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class AdhanBootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || intent.getAction() == null) return;
        String a = intent.getAction();
        if (Intent.ACTION_BOOT_COMPLETED.equals(a)
                || Intent.ACTION_MY_PACKAGE_REPLACED.equals(a)
                || "android.intent.action.QUICKBOOT_POWERON".equals(a)
                || "android.app.action.SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED".equals(a)) {
            AdhanAlarmScheduler.rescheduleStored(context.getApplicationContext());
        }
    }
}
