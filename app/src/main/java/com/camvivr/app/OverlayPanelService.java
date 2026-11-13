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
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * OverlayPanelService — gol floating button (eye) + control panel.
 * Panel: EYE ↑ MOUT / ← CENTER → / PLAY ↓ LOOP / ROT FLIP OFF + mini info.
 */
public class OverlayPanelService extends Service {

    public static boolean running = false;

    private WindowManager wm;
    private TextView fab;            // gol floating button
    private LinearLayout panel;      // control panel
    private TextView infoLine;       // "Nature.mp4  00:12 / 00:30"
    private WindowManager.LayoutParams fabParams, panelParams;
    private boolean panelOpen = false;
    private final Handler h = new Handler(Looper.getMainLooper());

    @Override
    public void onCreate() {
        super.onCreate();
        running = true;
        notifyForeground();
        buildFab();
        buildPanel();
        h.post(tick);
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
        b.setContentTitle("CamVivr floating button chalu")
                .setContentText("Gol button se media control karein")
                .setSmallIcon(android.R.drawable.ic_menu_camera)
                .setContentIntent(pi).setOngoing(true);
        try {
            if (Build.VERSION.SDK_INT >= 34) {
                startForeground(9, b.build(),
                        android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
            } else {
                startForeground(9, b.build());
            }
        } catch (Exception e) {
            Engine.get().fail("Foreground service start fail: " + e.getMessage());
        }
    }

    /* ---------------- gol floating button ---------------- */
    private void buildFab() {
        wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);

        fab = new TextView(this);
        fab.setText("◉");
        fab.setTextSize(26f);
        fab.setTextColor(Color.WHITE);
        fab.setGravity(Gravity.CENTER);
        GradientDrawable g = new GradientDrawable();
        g.setShape(GradientDrawable.OVAL);
        g.setColor(UiKit.BLUE);
        g.setStroke(4, 0xFF93C5FD);
        fab.setBackground(g);
        int size = (int) (54 * getResources().getDisplayMetrics().density);
        fab.setWidth(size); fab.setHeight(size);

        int type = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                : WindowManager.LayoutParams.TYPE_PHONE;
        fabParams = new WindowManager.LayoutParams(size, size, type,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, PixelFormat.TRANSLUCENT);
        fabParams.gravity = Gravity.TOP | Gravity.START;
        fabParams.x = 16;
        fabParams.y = 260;

        fab.setOnTouchListener(new View.OnTouchListener() {
            float dx, dy; long down; boolean moved;
            public boolean onTouch(View v, MotionEvent e) {
                switch (e.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                        dx = e.getRawX() - fabParams.x; dy = e.getRawY() - fabParams.y;
                        down = System.currentTimeMillis(); moved = false;
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        if (Math.abs(e.getRawX() - fabParams.x - dx) > 10 || Math.abs(e.getRawY() - fabParams.y - dy) > 10) {
                            moved = true;
                            fabParams.x = (int) (e.getRawX() - dx);
                            fabParams.y = (int) (e.getRawY() - dy);
                            try { wm.updateViewLayout(fab, fabParams); } catch (Exception ignored) { }
                        }
                        return true;
                    case MotionEvent.ACTION_UP:
                        if (!moved && System.currentTimeMillis() - down < 500) togglePanel();
                        return true;
                }
                return false;
            }
        });

        try { wm.addView(fab, fabParams); } catch (Exception ignored) { }
    }

    /* ---------------- control panel ---------------- */
    private void buildPanel() {
        panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        GradientDrawable g = new GradientDrawable();
        g.setColor(0xF00A1220);
        g.setCornerRadius(28f);
        g.setStroke(3, UiKit.BLUE);
        panel.setBackground(g);
        panel.setPadding(16, 14, 16, 14);

        /* title row: − CamVivr ✕ */
        LinearLayout title = new LinearLayout(this);
        title.setOrientation(LinearLayout.HORIZONTAL);
        title.setGravity(Gravity.CENTER_VERTICAL);
        TextView min = chip("−", v -> togglePanel());
        TextView name = new TextView(this);
        name.setText("CamVivr");
        name.setTextColor(UiKit.TEXT);
        name.setTextSize(15f);
        name.setTypeface(null, android.graphics.Typeface.BOLD);
        name.setGravity(Gravity.CENTER);
        name.setLayoutParams(new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        TextView close = chip("✕", v -> stopSelf());
        title.addView(min);
        title.addView(name);
        title.addView(close);
        panel.addView(title);

        /* grid: 3 columns x 4 rows */
        panel.addView(row3("EYE", "↑", "MOUT",
                v -> toggleEyes(), v -> head(0, -1), v -> toggleMouth()));
        panel.addView(row3("←", "CENTER", "→",
                v -> head(-1, 0), v -> head(0, 0), v -> head(1, 0)));
        panel.addView(row3("PLAY", "↓", "LOOP",
                v -> control(0), v -> head(0, 1), v -> control(4)));
        panel.addView(row3("ZOOM −", "ROT", "ZOOM +",
                v -> control(9), v -> control(7), v -> control(10)));
        panel.addView(row3("FLIP", "CENTER", "OFF",
                v -> control(8), v -> control(11), v -> control(2)));

        /* info line */
        infoLine = new TextView(this);
        infoLine.setText("Koi media nahi");
        infoLine.setTextColor(UiKit.MUTED);
        infoLine.setTextSize(11.5f);
        infoLine.setPadding(6, 12, 6, 4);
        panel.addView(infoLine);

        int type = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                : WindowManager.LayoutParams.TYPE_PHONE;
        int wpx = (int) (250 * getResources().getDisplayMetrics().density);
        panelParams = new WindowManager.LayoutParams(wpx,
                WindowManager.LayoutParams.WRAP_CONTENT, type,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, PixelFormat.TRANSLUCENT);
        panelParams.gravity = Gravity.CENTER;

        panel.setOnTouchListener(new View.OnTouchListener() {
            float dx, dy; long down;
            public boolean onTouch(View v, MotionEvent e) {
                if (e.getActionMasked() == MotionEvent.ACTION_DOWN) {
                    dx = e.getRawX() - panelParams.x; dy = e.getRawY() - panelParams.y;
                    down = System.currentTimeMillis();
                } else if (e.getActionMasked() == MotionEvent.ACTION_MOVE
                        && System.currentTimeMillis() - down > 300) {
                    panelParams.x = (int) (e.getRawX() - dx);
                    panelParams.y = (int) (e.getRawY() - dy);
                    try { wm.updateViewLayout(panel, panelParams); } catch (Exception ignored) { }
                }
                return false;
            }
        });

        panel.setVisibility(View.GONE);
        try { wm.addView(panel, panelParams); } catch (Exception ignored) { }
    }

    private TextView chip(String label, View.OnClickListener l) {
        TextView t = new TextView(this);
        t.setText(label);
        t.setTextColor(UiKit.TEXT);
        t.setTextSize(16f);
        t.setGravity(Gravity.CENTER);
        t.setPadding(18, 8, 18, 8);
        t.setOnClickListener(l);
        return t;
    }

    private LinearLayout row3(String a, String b, String c,
                              View.OnClickListener la, View.OnClickListener lb, View.OnClickListener lc) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, 6, 0, 0);
        row.addView(cell(a, la));
        row.addView(cell(b, lb));
        row.addView(cell(c, lc));
        return row;
    }

    private TextView cell(String label, View.OnClickListener l) {
        TextView t = new TextView(this);
        t.setText(label);
        t.setTextColor(label.equals("PLAY") ? UiKit.GREEN
                : label.equals("OFF") ? UiKit.RED : UiKit.BLUE_L);
        t.setTextSize(label.length() > 4 ? 11f : 14f);
        t.setTypeface(null, android.graphics.Typeface.BOLD);
        t.setGravity(Gravity.CENTER);
        t.setPadding(0, 16, 0, 16);
        GradientDrawable g = new GradientDrawable();
        g.setColor(0xFF132234);
        g.setCornerRadius(16f);
        g.setStroke(2, 0xFF20344B);
        t.setBackground(g);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        lp.setMargins(4, 4, 4, 4);
        t.setLayoutParams(lp);
        t.setOnClickListener(l);
        return t;
    }

    /* ---------------- actions ---------------- */
    private void togglePanel() {
        panelOpen = !panelOpen;
        panel.setVisibility(panelOpen ? View.VISIBLE : View.GONE);
        fab.setText(panelOpen ? "✕" : "◉");
    }

    private void control(int what) {
        Engine.MediaControl c = Engine.get().control();
        if (c == null) return;
        switch (what) {
            case 0: c.play(); break;
            case 2: c.stop(); break;
            case 4: c.toggleLoop(); break;
            case 7: c.rotate(); break;
            case 8: c.flip(); break;
            case 9: c.zoomOut(); break;
            case 10: c.zoomIn(); break;
            case 11: c.center(); break;
        }
        Engine.get().log("PANEL", "action " + what);
    }

    private void head(int x, int y) { send("head:" + x + "," + y); }
    private void toggleEyes() { send("eyes"); }
    private void toggleMouth() { send("mouth"); }

    private void send(String action) {
        Intent i = new Intent("com.camvivr.app.ACTION");
        i.setPackage(getPackageName());
        i.putExtra("cmd", action);
        sendBroadcast(i);
        Engine.get().log("PANEL", action);
    }

    /* ---------------- info ticker ---------------- */
    private final Runnable tick = new Runnable() {
        public void run() {
            if (infoLine != null) {
                Engine.MediaControl c = Engine.get().control();
                if (c == null) {
                    infoLine.setText("App mein media load karein");
                } else {
                    int pos = c.positionMs(), dur = c.durationMs();
                    String t = dur > 0
                            ? String.format("%02d:%02d / %02d:%02d", pos / 60000, (pos / 1000) % 60,
                                            dur / 60000, (dur / 1000) % 60)
                            : "Photo";
                    infoLine.setText(c.mediaLabel() + "  ·  " + t);
                }
            }
            h.postDelayed(this, 500);
        }
    };

    @Override
    public void onDestroy() {
        running = false;
        h.removeCallbacks(tick);
        try { if (panel != null) wm.removeView(panel); } catch (Exception ignored) { }
        try { if (fab != null) wm.removeView(fab); } catch (Exception ignored) { }
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) { return null; }
}
