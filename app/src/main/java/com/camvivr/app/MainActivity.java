package com.camvivr.app;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

/**
 * CamVivr — sirf 3 cheezein:
 *   1. Floating Button (ON/OFF)
 *   2. Video / Photo (upload + choose)
 *   3. Settings (permissions)
 */
public class MainActivity extends Activity {

    private static final int REQ_IMAGE = 11;
    private static final int REQ_VIDEO = 12;

    private FrameLayout root;
    private Store store;
    private Preview preview;
    private String screen = "home";
    private android.content.BroadcastReceiver panelReceiver;
    private boolean receiverOn = false;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        store = new Store(this);
        root = new FrameLayout(this);
        root.setBackgroundColor(UiKit.BG);
        setContentView(root);

        Engine.get().setControl(new Engine.MediaControl() {
            public void play() { if (preview != null) preview.play(); Engine.get().set(Engine.State.PLAYING); }
            public void pause() { if (preview != null) preview.pause(); Engine.get().set(Engine.State.PAUSED); }
            public void stop() { if (preview != null) preview.stop(); Engine.get().set(Engine.State.STOPPED); }
            public void restart() { if (preview != null) { preview.stop(); preview.play(); } }
            public void toggleLoop() { loopOn = !loopOn; if (preview != null) preview.setLoop(loopOn);
                Toast.makeText(MainActivity.this, "Loop: " + (loopOn ? "ON" : "OFF"), Toast.LENGTH_SHORT).show(); }
            public void next() { cycle(1); }
            public void previous() { cycle(-1); }
            public boolean isPlaying() { return Engine.get().state() == Engine.State.PLAYING; }
            public int positionMs() { return preview == null ? 0 : preview.position(); }
            public int durationMs() { return preview == null ? 0 : (int) preview.mediaDurationMs; }
            public String mediaLabel() {
                if (preview == null || preview.mode.equals("none")) return "Koi media nahi";
                String u = preview.mode.equals("image") ? store.imageUri() : store.videoUri();
                String n = u.substring(u.lastIndexOf('/') + 1);
                return n.isEmpty() ? preview.mode : n;
            }
            public void rotate() {
                int r = (store.rotation() + 90) % 360; store.setRotation(r);
                if (preview != null) { preview.rotationDeg = r; preview.invalidate(); }
            }
            public void flip() {
                boolean f = !store.flipH(); store.setFlipH(f);
                if (preview != null) { preview.flipH = f; preview.invalidate(); }
            }
            public void zoomIn() { if (preview != null) { preview.zoomBy(0.15f); store.setZoom((int) (preview.zoom * 100)); } }
            public void zoomOut() { if (preview != null) { preview.zoomBy(-0.15f); store.setZoom((int) (preview.zoom * 100)); } }
            public void center() { if (preview != null) { preview.centerFrame(); store.setZoom(100); } }
        });

        registerPanelReceiver();
        if (Build.VERSION.SDK_INT >= 33
                && checkSelfPermission("android.permission.POST_NOTIFICATIONS")
                   != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            try { requestPermissions(new String[]{"android.permission.POST_NOTIFICATIONS"}, 91); } catch (Exception ignored) { }
        }
        render();
    }

    private boolean loopOn = true;

    /* ================= screens ================= */
    private void render() {
        root.removeAllViews();
        View v;
        if (screen.equals("media")) v = buildMedia();
        else if (screen.equals("settings")) v = buildSettings();
        else v = buildHome();

        root.addView(v, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
    }

    private void go(String s) { screen = s; render(); }

    /* ================= HOME ================= */
    private View buildHome() {
        LinearLayout box = UiKit.column(this);

        LinearLayout head = UiKit.row(this);
        TextView logo = UiKit.title(this, "C", 26);
        logo.setTextColor(Color.WHITE);
        logo.setGravity(Gravity.CENTER);
        GradientDrawable g = new GradientDrawable();
        g.setColor(UiKit.BLUE);
        g.setCornerRadius(18f);
        logo.setBackground(g);
        logo.setWidth(dp(52)); logo.setHeight(dp(52));
        head.addView(logo);
        LinearLayout names = new LinearLayout(this);
        names.setOrientation(LinearLayout.VERTICAL);
        names.setPadding(dp(12), 0, 0, 0);
        names.addView(UiKit.title(this, "CamVivr", 23));
        names.addView(UiKit.muted(this, "Your Media, Your Virtual Camera"));
        head.addView(names);
        box.addView(head);

        box.addView(UiKit.spacer(this, 1, dp(10)));

        /* 1 — Floating button */
        LinearLayout c1 = UiKit.card(this);
        LinearLayout r1 = UiKit.row(this);
        TextView t1 = UiKit.title(this, "1.  Floating Button", 17);
        t1.setLayoutParams(UiKit.weight(1f));
        r1.addView(t1);
        TextView st1 = UiKit.body(this, OverlayPanelService.running ? "ON" : "OFF", 12.5f,
                OverlayPanelService.running ? UiKit.GREEN : UiKit.AMBER);
        st1.setTypeface(null, android.graphics.Typeface.BOLD);
        r1.addView(st1);
        c1.addView(r1);
        c1.addView(UiKit.muted(this, "Screen par gol button — usse controller panel khulta hai"));
        Button fb = UiKit.button(this,
                OverlayPanelService.running ? "Floating Button OFF karein" : "Floating Button ON karein",
                OverlayPanelService.running ? 0xFF3A1E22 : UiKit.BLUE,
                OverlayPanelService.running ? UiKit.RED : Color.WHITE);
        fb.setOnClickListener(v -> toggleFloating());
        c1.addView(fb, UiKit.matchWrap(12));
        box.addView(c1);

        /* 2 — Video / Photo */
        LinearLayout c2 = UiKit.card(this);
        c2.addView(UiKit.title(this, "2.  Video / Photo", 17));
        c2.addView(UiKit.muted(this, "Upload karein aur chunein"));
        c2.setOnClickListener(v -> go("media"));
        Button open2 = UiKit.button(this, "Kholen  ›", 0xFF1B2A3E, UiKit.TEXT);
        open2.setOnClickListener(v -> go("media"));
        c2.addView(open2, UiKit.matchWrap(12));
        box.addView(c2);

        Button share = UiKit.button(this, "📺  Screen-share mode (call mein dikhane ke liye)",
                UiKit.BLUE, Color.WHITE);
        share.setOnClickListener(v -> screenShareMode());
        box.addView(share, UiKit.matchWrap(4));

        /* 3 — Settings */
        LinearLayout c3 = UiKit.card(this);
        c3.addView(UiKit.title(this, "3.  Settings", 17));
        c3.addView(UiKit.muted(this, "Permissions aur app settings"));
        Button open3 = UiKit.button(this, "Kholen  ›", 0xFF1B2A3E, UiKit.TEXT);
        open3.setOnClickListener(v -> go("settings"));
        c3.addView(open3, UiKit.matchWrap(12));
        box.addView(c3);

        TextView diag = UiKit.muted(this, "Service: " + (OverlayPanelService.running ? "RUNNING" : "STOPPED")
                + (Engine.get().error().isEmpty() ? "" : "  ·  Error: " + Engine.get().error()));
        diag.setPadding(0, dp(10), 0, 0);
        box.addView(diag);

        return wrap(UiKit.scroll(this, box));
    }

    /* ================= MEDIA ================= */
    private View buildMedia() {
        LinearLayout box = UiKit.column(this);
        box.addView(backRow("Video / Photo"));

        LinearLayout up = UiKit.row(this);
        Button vid = UiKit.button(this, "🎬  Video upload", UiKit.BLUE, Color.WHITE);
        vid.setLayoutParams(UiKit.weight(1f));
        vid.setOnClickListener(v -> pick(REQ_VIDEO, "video/*"));
        up.addView(vid);
        up.addView(UiKit.spacer(this, 10, 1));
        Button img = UiKit.button(this, "🖼  Photo upload", 0xFF1B2A3E, UiKit.TEXT);
        img.setLayoutParams(UiKit.weight(1f));
        img.setOnClickListener(v -> pick(REQ_IMAGE, "image/*"));
        up.addView(img);
        box.addView(up);

        /* preview — ek hi instance (dobara banane se video atak jaati thi) */
        if (preview != null && preview.getParent() != null)
            ((ViewGroup) preview.getParent()).removeView(preview);
        if (preview == null) preview = new Preview(this);
        preview.zoom = store.zoom() / 100f;
        preview.rotationDeg = store.rotation();
        preview.flipH = store.flipH();
        preview.flipV = store.flipV();
        preview.brightness = store.brightness();
        preview.contrast = store.contrast();
        preview.eyesOn = store.eyesOn();
        preview.mouthOn = store.mouthOn();
        preview.autoBlink = store.autoBlink();
        preview.blinkInterval = store.blinkInterval();
        preview.animSpeed = store.animSpeed();

        FrameLayout holder = new FrameLayout(this);
        holder.setBackground(UiKit.round(0xFF050B14, 18, UiKit.LINE));
        holder.addView(preview, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        LinearLayout.LayoutParams hp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(230));
        hp.topMargin = dp(14);
        hp.bottomMargin = dp(8);
        box.addView(holder, hp);

        if (store.source().equals("image") && !store.imageUri().isEmpty())
            preview.loadImage(Uri.parse(store.imageUri()));
        else if (store.source().equals("video") && !store.videoUri().isEmpty()) {
            preview.loadVideo(Uri.parse(store.videoUri()));
        }

        TextView info = UiKit.muted(this, preview.mode.equals("none") ? "Koi media nahi" : preview.infoLine());
        info.setPadding(0, 0, 0, dp(8));
        box.addView(info);

        LinearLayout ctrl = UiKit.row(this);
        Button play = UiKit.button(this, "▶  Play", UiKit.GREEN, Color.WHITE);
        play.setLayoutParams(UiKit.weight(1f));
        play.setOnClickListener(v -> preview.play());
        ctrl.addView(play);
        ctrl.addView(UiKit.spacer(this, 8, 1));
        Button pause = UiKit.button(this, "⏸", UiKit.BLUE, Color.WHITE);
        pause.setLayoutParams(UiKit.weight(1f));
        pause.setOnClickListener(v -> preview.pause());
        ctrl.addView(pause);
        ctrl.addView(UiKit.spacer(this, 8, 1));
        Button stop = UiKit.button(this, "■", UiKit.RED, Color.WHITE);
        stop.setLayoutParams(UiKit.weight(1f));
        stop.setOnClickListener(v -> preview.stop());
        ctrl.addView(stop);
        box.addView(ctrl, UiKit.matchWrap(6));

        /* uploaded list */
        LinearLayout list = UiKit.card(this);
        list.addView(UiKit.title(this, "Uploaded media", 15));
        list.addView(UiKit.muted(this, "Ek samay par ek hi media — naya upload purane ko replace karega"));
        if (store.sources().isEmpty()) list.addView(UiKit.muted(this, "Abhi kuch upload nahi"));
        for (final String[] m : store.sources()) {
            LinearLayout r = UiKit.row(this);
            r.setPadding(0, 12, 0, 12);
            TextView n = UiKit.body(this, (m[2].equals("image") ? "🖼  " : "🎬  ") + m[0], 13f, UiKit.TEXT);
            n.setLayoutParams(UiKit.weight(1f));
            n.setOnClickListener(v -> openUri(Uri.parse(m[1]), m[2]));
            r.addView(n);

            TextView use = UiKit.body(this, "Use", 12f, UiKit.BLUE_L);
            use.setPadding(dp(12), 0, dp(12), 0);
            use.setOnClickListener(v -> openUri(Uri.parse(m[1]), m[2]));
            r.addView(use);
            TextView del = UiKit.body(this, "✕", 14f, UiKit.RED);
            del.setPadding(dp(14), 0, 0, 0);
            del.setOnClickListener(v -> new android.app.AlertDialog.Builder(this)
                    .setTitle("Hatana hai?")
                    .setMessage(m[0])
                    .setPositiveButton("Hatao", (d, w) -> { store.removeSource(m[1]); go("media"); })
                    .setNegativeButton("Nahi", null).show());
            r.addView(del);
            list.addView(r);
        }
        box.addView(list, UiKit.matchWrap(10));

        return wrap(UiKit.scroll(this, box));
    }

    /* ================= SETTINGS ================= */
    private View buildSettings() {
        LinearLayout box = UiKit.column(this);
        box.addView(backRow("Settings"));

        LinearLayout perm = UiKit.card(this);
        perm.addView(UiKit.title(this, "Permissions", 15));

        boolean overlay = Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(this);
        perm.addView(permRow("Display over other apps", overlay, () -> {
            new android.app.AlertDialog.Builder(this)
                    .setTitle("Display over other apps")
                    .setMessage("Is permission se floating button screen par dikhta hai. "
                              + "Aap ise kabhi bhi band kar sakte hain.")
                    .setPositiveButton("Permission dein", (d, w) -> {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                            startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                    Uri.parse("package:" + getPackageName())));
                    })
                    .setNegativeButton("Nahi", null).show();
        }));

        boolean notif = Build.VERSION.SDK_INT < 33
                || checkSelfPermission("android.permission.POST_NOTIFICATIONS")
                   == android.content.pm.PackageManager.PERMISSION_GRANTED;
        perm.addView(permRow("Notifications", notif, () -> {
            if (Build.VERSION.SDK_INT >= 33) {
                requestPermissions(new String[]{"android.permission.POST_NOTIFICATIONS"}, 91);
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Intent i = new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS);
                i.putExtra(Settings.EXTRA_APP_PACKAGE, getPackageName());
                startActivity(i);
            }
        }));

        perm.addView(permRow("Gallery / files access", true, () ->
                Toast.makeText(this, "Koi storage permission nahi maangi jaati — "
                        + "file picker se sirf aapki chuni hui file padhi jaati hai", Toast.LENGTH_LONG).show()));

        perm.addView(UiKit.muted(this, "Accessibility permission ki zaroorat nahi — "
                + "ye app kisi doosre app ko control nahi karta."));
        perm.addView(UiKit.muted(this, "Camera/Microphone permission bhi nahi maangi jaati — "
                + "app camera hardware use nahi karta."));
        perm.addView(UiKit.muted(this, "Saari permissions aap kabhi bhi phone settings se band kar sakte hain."));
        box.addView(perm);

        LinearLayout about = UiKit.card(this);
        about.addView(UiKit.title(this, "About", 15));
        about.addView(UiKit.body(this,
                "CamVivr v5.0\n\n"
              + "Aapki photo/video phone se bahar nahi jaati. Koi server, koi tracking nahi.\n\n"
              + "Note: doosre apps (WhatsApp etc.) ke camera mein seedha media dikhana "
              + "normal Android app se possible nahi — uske liye root/system support chahiye.",
                13f, UiKit.MUTED));
        box.addView(about);

        LinearLayout reset = UiKit.card(this);
        reset.addView(UiKit.title(this, "Reset", 15));
        Button r = UiKit.button(this, "Saara data mitayein", 0xFF3A1E22, UiKit.RED);
        r.setOnClickListener(v -> {
            store.resetAll();
            Toast.makeText(this, "Reset ho gaya", Toast.LENGTH_SHORT).show();
            go("settings");
        });
        reset.addView(r, UiKit.matchWrap(10));
        box.addView(reset);

        return wrap(UiKit.scroll(this, box));
    }

    private LinearLayout permRow(String name, boolean ok, Runnable action) {
        LinearLayout r = UiKit.row(this);
        r.setPadding(0, 12, 0, 12);
        TextView n = UiKit.body(this, name, 13.5f, UiKit.TEXT);
        n.setLayoutParams(UiKit.weight(1f));
        r.addView(n);
        TextView s = UiKit.body(this, ok ? "✓" : "Dein", 13f, ok ? UiKit.GREEN : UiKit.AMBER);
        s.setTypeface(null, android.graphics.Typeface.BOLD);
        s.setPadding(dp(10), 0, 0, 0);
        s.setOnClickListener(v -> action.run());
        r.addView(s);
        return r;
    }

    private LinearLayout backRow(String title) {
        LinearLayout row = UiKit.row(this);
        TextView back = UiKit.body(this, "‹", 22f, UiKit.TEXT);
        back.setPadding(0, 0, dp(14), 0);
        back.setOnClickListener(v -> go("home"));
        row.addView(back);
        row.addView(UiKit.title(this, title, 20));
        LinearLayout wrap = new LinearLayout(this);
        wrap.setOrientation(LinearLayout.VERTICAL);
        wrap.addView(row);
        return wrap;
    }

    /* ================= helpers ================= */
    private View wrap(View content) {
        FrameLayout f = new FrameLayout(this);
        f.setBackgroundColor(UiKit.BG);
        f.addView(content, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        return f;
    }

    private void toggleFloating() {
        if (OverlayPanelService.running) {
            stopService(new Intent(this, OverlayPanelService.class));
            Toast.makeText(this, "Floating button OFF", Toast.LENGTH_SHORT).show();
            render();
            return;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName())));
            Toast.makeText(this, "Permission dein, phir dobara dabayein", Toast.LENGTH_LONG).show();
            return;
        }
        try {
            Intent svc = new Intent(this, OverlayPanelService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(svc);
            else startService(svc);
            Toast.makeText(this, "Floating button ON — screen par gol button dekhein", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(this, "Service start nahi hui: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
        render();
    }

    private void cycle(int dir) {
        java.util.List<String[]> list = store.sources();
        if (list.isEmpty()) return;
        String cur = store.source().equals("image") ? store.imageUri() : store.videoUri();
        int idx = 0;
        for (int i = 0; i < list.size(); i++) if (list.get(i)[1].equals(cur)) { idx = i; break; }
        idx = (idx + dir + list.size()) % list.size();
        openUri(Uri.parse(list.get(idx)[1]), list.get(idx)[2]);
    }

    private void pick(int req, String type) {
        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType(type);
        i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        try { startActivityForResult(i, req); }
        catch (Exception e) { Toast.makeText(this, "File picker nahi khula", Toast.LENGTH_SHORT).show(); }
    }

    @Override
    protected void onActivityResult(int req, int res, Intent data) {
        super.onActivityResult(req, res, data);
        if (res != RESULT_OK || data == null || data.getData() == null) return;
        Uri uri = data.getData();
        try {
            getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
        } catch (Exception ignored) { }
        upload(uri, req == REQ_IMAGE ? "image" : "video");
    }

    /** Upload: file ko app ki private storage mein copy karta hai (permission expire nahi hoti). */
    private Uri importToPrivate(Uri src, String type) {
        try {
            String name = displayName(src);
            if (!name.contains(".")) name = name + (type.equals("image") ? ".jpg" : ".mp4");
            java.io.File dir = new java.io.File(getFilesDir(), "media");
            if (!dir.exists()) dir.mkdirs();
            java.io.File out = new java.io.File(dir, name);
            java.io.InputStream in = getContentResolver().openInputStream(src);
            java.io.FileOutputStream fos = new java.io.FileOutputStream(out);
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0) fos.write(buf, 0, n);
            in.close(); fos.close();
            Engine.get().log("STORAGE", "saved " + out.getName());
            return Uri.fromFile(out);
        } catch (Exception e) {
            Engine.get().log("STORAGE", "copy fail: " + e.getMessage());
            return src;
        }
    }

    private void openUri(Uri uri, String type) {
        if (preview == null) { go("media"); return; }
        boolean ok = type.equals("image") ? preview.loadImage(uri) : preview.loadVideo(uri);
        if (ok) {
            store.setSource(type);
            if (type.equals("image")) store.setImageUri(uri.toString());
            else store.setVideoUri(uri.toString());
            if (type.equals("video")) preview.play();
            go("media");
        } else {
            Toast.makeText(this, "Ye file khul nahi payi", Toast.LENGTH_LONG).show();
        }
    }

    /** Naya upload — purana replace ho jaata hai (ek samay par ek media). */
    private void upload(Uri src, String type) {
        Uri local = importToPrivate(src, type);
        String name = displayName(local);
        store.clearSources();
        store.addSource(name, local.toString(), type);
        openUri(local, type);
        Toast.makeText(this, "Upload ho gaya: " + name, Toast.LENGTH_SHORT).show();
    }

    /** Gallery file ka asli naam. */
    private String displayName(Uri uri) {
        try {
            android.database.Cursor c = getContentResolver().query(uri, null, null, null, null);
            if (c != null) {
                int idx = c.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME);
                if (c.moveToFirst() && idx >= 0) {
                    String n = c.getString(idx);
                    c.close();
                    if (n != null && !n.isEmpty()) return n;
                }
                c.close();
            }
        } catch (Exception ignored) { }
        String p = uri.getLastPathSegment();
        return p == null ? "media" : p;
    }

    /** Screen-share mode — call ke dauran video/photo poore screen par (100% working). */
    private void screenShareMode() {
        if (store.source().equals("none")) {
            Toast.makeText(this, "Pehle video ya photo upload karein", Toast.LENGTH_LONG).show();
            go("media");
            return;
        }
        final FrameLayout fs = new FrameLayout(this);
        fs.setBackgroundColor(Color.BLACK);

        Preview big = new Preview(this);
        big.zoom = store.zoom() / 100f;
        big.rotationDeg = store.rotation();
        big.flipH = store.flipH();
        big.flipV = store.flipV();
        big.brightness = store.brightness();
        big.contrast = store.contrast();
        big.eyesOn = store.eyesOn();
        big.mouthOn = store.mouthOn();
        big.autoBlink = store.autoBlink();
        big.blinkInterval = store.blinkInterval();
        big.animSpeed = store.animSpeed();

        if (store.source().equals("image") && !store.imageUri().isEmpty())
            big.loadImage(Uri.parse(store.imageUri()));
        else if (!store.videoUri().isEmpty()) {
            big.loadVideo(Uri.parse(store.videoUri()));
            big.setLoop(true);
        }

        fs.addView(big, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        /* chhota exit button — tap karne par dikhta hai */
        final TextView exit = UiKit.body(this, "✕  Band karein", 13f, Color.WHITE);
        exit.setBackground(UiKit.round(0xCC000000, 20, 0x66FFFFFF));
        exit.setPadding(dp(18), dp(10), dp(18), dp(10));
        exit.setAlpha(0.35f);
        exit.setOnClickListener(v -> {
            big.release();
            showSystemBars();
            render();
        });
        FrameLayout.LayoutParams elp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        elp.gravity = Gravity.TOP | Gravity.START;
        elp.setMargins(dp(14), dp(40), 0, 0);
        fs.addView(exit, elp);

        root.removeAllViews();
        root.addView(fs, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        hideSystemBars();
        Toast.makeText(this, "Ab call mein 'Share screen' karein — video poore screen par chalega",
                Toast.LENGTH_LONG).show();
    }

    private void hideSystemBars() {
        try {
            if (Build.VERSION.SDK_INT >= 30) {
                android.view.WindowInsetsController c = getWindow().getInsetsController();
                if (c != null) {
                    c.hide(android.view.WindowInsets.Type.systemBars());
                    c.setSystemBarsBehavior(
                            android.view.WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
                }
            } else {
                getWindow().getDecorView().setSystemUiVisibility(
                        View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
            }
        } catch (Exception ignored) { }
    }

    private void showSystemBars() {
        try {
            if (Build.VERSION.SDK_INT >= 30) {
                android.view.WindowInsetsController c = getWindow().getInsetsController();
                if (c != null) c.show(android.view.WindowInsets.Type.systemBars());
            } else {
                getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
            }
        } catch (Exception ignored) { }
    }

    private int dp(int v) { return (int) (v * getResources().getDisplayMetrics().density); }

    /* ================= panel receiver ================= */
    private void registerPanelReceiver() {
        if (receiverOn) return;
        panelReceiver = new android.content.BroadcastReceiver() {
            @Override public void onReceive(Context ctx, Intent it) {
                String cmd = it.getStringExtra("cmd");
                if (cmd == null || preview == null) return;
                if (cmd.equals("eyes")) {
                    store.setEyesOn(!store.eyesOn());
                    preview.eyesOn = store.eyesOn();
                } else if (cmd.equals("mouth")) {
                    store.setMouthOn(!store.mouthOn());
                    preview.mouthOn = store.mouthOn();
                } else if (cmd.startsWith("head:")) {
                    String[] xy = cmd.substring(5).split(",");
                    preview.setHead(Float.parseFloat(xy[0]), Float.parseFloat(xy[1]), 0);
                }
            }
        };
        IntentFilter f = new IntentFilter("com.camvivr.app.ACTION");
        if (Build.VERSION.SDK_INT >= 33) registerReceiver(panelReceiver, f, Context.RECEIVER_NOT_EXPORTED);
        else registerReceiver(panelReceiver, f);
        receiverOn = true;
    }

    @Override
    protected void onDestroy() {
        if (receiverOn && panelReceiver != null) {
            try { unregisterReceiver(panelReceiver); } catch (Exception ignored) { }
        }
        if (preview != null) preview.release();
        super.onDestroy();
    }
}
