package com.camvivr.app;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.IBinder;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * OverlayPanelService — floating control panel (Camera / Blink / Mouth / Stop).
 * Ye panel doosre app ke upar bhi dikhta hai (overlay permission ke saath).
 */
public class OverlayPanelService extends Service {

    public static boolean running = false;
    private WindowManager wm;
    private LinearLayout panel;
    private WindowManager.LayoutParams params;

    @Override
    public void onCreate() {
        super.onCreate();
        running = true;
        notifyForeground();
        buildPanel();
    }

    private void notifyForeground() {
        String ch = "camvivr";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm != null) nm.createNotificationChannel(
                    new NotificationChannel(ch, "CamVivr", NotificationManager.IMPORTANCE_LOW));
        }
        int flags = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                ? PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
                : PendingIntent.FLAG_UPDATE_CURRENT;
        PendingIntent pi = PendingIntent.getActivity(this, 0, new Intent(this, MainActivity.class), flags);
        Notification.Builder b = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? new Notification.Builder(this, ch) : new Notification.Builder(this);
        b.setContentTitle("CamVivr panel chalu")
                .setContentText("Floating control screen par hai")
                .setSmallIcon(android.R.drawable.ic_menu_camera)
                .setContentIntent(pi).setOngoing(true);
        startForeground(9, b.build());
    }

    private void buildPanel() {
        wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);

        panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        GradientDrawable g = new GradientDrawable();
        g.setColor(0xF00A1220);
        g.setCornerRadius(24f);
        g.setStroke(3, UiKit.BLUE);
        panel.setBackground(g);
        panel.setPadding(18, 14, 18, 14);

        TextView title = new TextView(this);
        title.setText("CamVivr");
        title.setTextColor(UiKit.BLUE_L);
        title.setTextSize(12f);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        title.setPadding(0, 0, 0, 8);
        panel.addView(title);

        panel.addView(btn("🎬  Camera", v -> {
            Intent i = new Intent(this, MainActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(i);
        }));
        panel.addView(btn("◉  Blink", v -> send("blink")));
        panel.addView(btn("▭  Mouth", v -> send("mouth")));
        panel.addView(btn("✕  Band karein", v -> stopSelf()));

        int type = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                : WindowManager.LayoutParams.TYPE_PHONE;
        params = new WindowManager.LayoutParams(
                (int) (160 * getResources().getDisplayMetrics().density),
                WindowManager.LayoutParams.WRAP_CONTENT, type,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, PixelFormat.TRANSLUCENT);
        params.gravity = Gravity.TOP | Gravity.START;
        params.x = 20;
        params.y = 220;

        // drag support
        panel.setOnTouchListener(new View.OnTouchListener() {
            float dx, dy; long down;
            public boolean onTouch(View v, MotionEvent e) {
                switch (e.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                        dx = e.getRawX() - params.x; dy = e.getRawY() - params.y;
                        down = System.currentTimeMillis();
                        return false;   // children ko bhi click milne do
                    case MotionEvent.ACTION_MOVE:
                        if (System.currentTimeMillis() - down > 250) {
                            params.x = (int) (e.getRawX() - dx);
                            params.y = (int) (e.getRawY() - dy);
                            try { wm.updateViewLayout(panel, params); } catch (Exception ignored) { }
                        }
                        return false;
                }
                return false;
            }
        });

        try { wm.addView(panel, params); } catch (Exception ignored) { }
    }

    private TextView btn(String label, View.OnClickListener l) {
        TextView t = new TextView(this);
        t.setText(label);
        t.setTextColor(UiKit.TEXT);
        t.setTextSize(13f);
        t.setPadding(4, 14, 4, 14);
        t.setOnClickListener(l);
        return t;
    }

    private void send(String action) {
        Intent i = new Intent(action);
        i.setPackage(getPackageName());
        sendBroadcast(i);
    }

    @Override
    public void onDestroy() {
        running = false;
        try { if (panel != null) wm.removeView(panel); } catch (Exception ignored) { }
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) { return null; }
}
