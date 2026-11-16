package com.mafatih.aljinan.adhan;

import android.app.Activity;
import android.app.KeyguardManager;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * Full-screen alarm UI shown over the lock screen so adhan can wake the device
 * even when the Cordova WebView is not open.
 */
public class AdhanAlarmActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        turnScreenOnAndUnlock();

        Intent src = getIntent();
        String title = src != null ? src.getStringExtra(AdhanAlarmScheduler.EXTRA_TITLE) : null;
        String text = src != null ? src.getStringExtra(AdhanAlarmScheduler.EXTRA_TEXT) : null;
        if (title == null || title.isEmpty()) title = "الكوثر — حان وقت الصلاة";
        if (text == null || text.isEmpty()) text = "منبّه الأذان";

        // Ensure playback is running even if the service start from the receiver was delayed.
        Intent svc = new Intent(this, AdhanPlaybackService.class);
        svc.setAction(AdhanPlaybackService.ACTION_PLAY);
        if (src != null) {
            svc.putExtras(src);
        }
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(svc);
            } else {
                startService(svc);
            }
        } catch (Exception ignored) {}

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);
        root.setPadding(48, 48, 48, 48);
        root.setBackgroundColor(0xFF06251C);

        TextView t1 = new TextView(this);
        t1.setText(title);
        t1.setTextColor(0xFFE6C766);
        t1.setTextSize(22f);
        t1.setGravity(Gravity.CENTER);
        t1.setPadding(0, 0, 0, 16);

        TextView t2 = new TextView(this);
        t2.setText(text);
        t2.setTextColor(0xFFFFFFFF);
        t2.setTextSize(16f);
        t2.setGravity(Gravity.CENTER);
        t2.setPadding(0, 0, 0, 36);

        Button stop = new Button(this);
        stop.setText("إيقاف الأذان");
        stop.setTextSize(18f);
        stop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent stopIntent = new Intent(AdhanAlarmActivity.this, AdhanPlaybackService.class);
                stopIntent.setAction(AdhanPlaybackService.ACTION_STOP);
                try {
                    startService(stopIntent);
                } catch (Exception ignored) {}
                finish();
            }
        });

        Button open = new Button(this);
        open.setText("فتح التطبيق");
        open.setTextSize(16f);
        open.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent launch = getPackageManager().getLaunchIntentForPackage(getPackageName());
                if (launch != null) {
                    launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(launch);
                }
                finish();
            }
        });

        root.addView(t1);
        root.addView(t2);
        root.addView(stop);
        root.addView(open);
        setContentView(root);
    }

    private void turnScreenOnAndUnlock() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true);
            setTurnScreenOn(true);
            KeyguardManager km = (KeyguardManager) getSystemService(KEYGUARD_SERVICE);
            if (km != null) {
                km.requestDismissKeyguard(this, null);
            }
        }
        getWindow().addFlags(
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                        | WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON
                        | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                        | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                        | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
        );
    }
}
