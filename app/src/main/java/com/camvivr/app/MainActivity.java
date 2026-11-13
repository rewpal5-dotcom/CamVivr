package com.camvivr.app;

import android.app.Activity;
import android.content.Intent;
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
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

/** CamVivr — Home / Camera / Control / Streaming / Settings. */
public class MainActivity extends Activity {

    private static final int REQ_IMAGE = 11;
    private static final int REQ_VIDEO = 12;

    private FrameLayout root;
    private LinearLayout navBar;
    private Store store;
    private Preview preview;
    private String tab = "home";

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        store = new Store(this);
        root = new FrameLayout(this);
        root.setBackgroundColor(UiKit.BG);
        setContentView(root);
        show("home");
    }

    /* ================= shell ================= */
    private void show(String which) {
        tab = which;
        root.removeAllViews();

        FrameLayout content = new FrameLayout(this);
        root.addView(content, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        View v;
        switch (which) {
            case "camera": v = buildCamera(); break;
            case "control": v = buildControl(); break;
            case "streaming": v = buildStreaming(); break;
            case "settings": v = buildSettings(); break;
            default: v = buildHome(); break;
        }
        content.addView(v, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        navBar = new LinearLayout(this);
        navBar.setOrientation(LinearLayout.HORIZONTAL);
        navBar.setBackgroundColor(UiKit.BG2);
        navBar.setPadding(0, 8, 0, 12);
        String[][] items = {{"home", "Home"}, {"camera", "Camera"}, {"control", "Control"},
                {"streaming", "Streaming"}, {"settings", "Settings"}};
        for (String[] it : items) {
            final String id = it[0];
            TextView t = new TextView(this);
            t.setText(it[1]);
            t.setTextSize(12.5f);
            t.setGravity(Gravity.CENTER);
            t.setPadding(0, 14, 0, 14);
            t.setTextColor(id.equals(which) ? UiKit.BLUE_L : UiKit.MUTED);
            t.setTypeface(null, id.equals(which) ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);
            t.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            t.setOnClickListener(x -> show(id));
            navBar.addView(t);
        }
        FrameLayout.LayoutParams np = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        np.gravity = Gravity.BOTTOM;
        root.addView(navBar, np);
    }

    private View padded(View content) {
        FrameLayout wrap = new FrameLayout(this);
        wrap.addView(content, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        wrap.setPadding(0, 0, 0, 70);
        return wrap;
    }

    /* ================= HOME ================= */
    private View buildHome() {
        LinearLayout box = UiKit.column(this);

        LinearLayout head = UiKit.row(this);
        TextView logo = UiKit.title(this, "C", 30);
        logo.setTextColor(Color.WHITE);
        logo.setGravity(Gravity.CENTER);
        GradientDrawable g = new GradientDrawable();
        g.setColor(UiKit.BLUE);
        g.setCornerRadius(22f);
        logo.setBackground(g);
        logo.setWidth(dp(56)); logo.setHeight(dp(56));
        head.addView(logo);
        LinearLayout names = new LinearLayout(this);
        names.setOrientation(LinearLayout.VERTICAL);
        names.setPadding(dp(14), 0, 0, 0);
        names.addView(UiKit.title(this, "CamVivr", 22));
        names.addView(UiKit.muted(this, "Your Camera, Your Control · v1.0"));
        head.addView(names);
        box.addView(head);

        /* status */
        LinearLayout st = UiKit.card(this);
        st.addView(UiKit.title(this, "Status", 15));
        boolean svc = OverlayPanelService.running;
        st.addView(statusRow("Service", svc ? "Active" : "Inactive", svc));
        boolean cam = preview != null && !preview.mode.equals("none");
        st.addView(statusRow("Preview", cam ? "Running" : "Stopped", cam));
        st.addView(statusRow("Source", sourceLabel(), !store.source().equals("none")));
        st.addView(UiKit.muted(this, "External virtual camera: "
                + (ExternalCamera.supported() ? "supported" : "not supported on this device")));
        box.addView(st);

        /* quick actions */
        LinearLayout row = UiKit.row(this);
        Button start = UiKit.button(this, "▶  Quick Start", UiKit.BLUE, Color.WHITE);
        start.setLayoutParams(UiKit.weight(1f));
        start.setOnClickListener(v -> {
            if (store.source().equals("none")) { Toast.makeText(this, "Pehle Camera tab se source chunein", Toast.LENGTH_SHORT).show(); show("camera"); return; }
            show("camera");
        });
        row.addView(start);
        row.addView(UiKit.spacer(this, 12, 1));
        Button stop = UiKit.button(this, "■  Quick Stop", 0xFF1B2A3E, UiKit.TEXT);
        stop.setLayoutParams(UiKit.weight(1f));
        stop.setOnClickListener(v -> {
            if (preview != null) preview.release();
            Toast.makeText(this, "Preview band", Toast.LENGTH_SHORT).show();
            show("home");
        });
        row.addView(stop);
        box.addView(row, UiKit.matchWrap(6));

        /* recent */
        LinearLayout rec = UiKit.card(this);
        rec.addView(UiKit.title(this, "Recent files", 15));
        if (store.recent().isEmpty()) rec.addView(UiKit.muted(this, "Abhi koi file nahi"));
        for (final String[] r : store.recent()) {
            TextView t = UiKit.body(this, "• " + r[0] + "  (" + r[2] + ")", 13f, UiKit.TEXT);
            t.setPadding(0, 10, 0, 10);
            t.setOnClickListener(v -> openUri(Uri.parse(r[1]), r[2]));
            rec.addView(t);
        }
        box.addView(rec);

        return padded(UiKit.scroll(this, box));
    }

    private LinearLayout statusRow(String name, String value, boolean ok) {
        LinearLayout r = UiKit.row(this);
        r.setPadding(0, 10, 0, 0);
        TextView n = UiKit.body(this, name, 13.5f, UiKit.MUTED);
        n.setLayoutParams(UiKit.weight(1f));
        r.addView(n);
        TextView v = UiKit.body(this, value, 13.5f, ok ? UiKit.GREEN : UiKit.AMBER);
        v.setTypeface(null, android.graphics.Typeface.BOLD);
        r.addView(v);
        return r;
    }

    private String sourceLabel() {
        String s = store.source();
        if (s.equals("image")) return "Image";
        if (s.equals("video")) return "Video";
        return "None";
    }

    /* ================= CAMERA ================= */
    private View buildCamera() {
        LinearLayout box = UiKit.column(this);

        box.addView(UiKit.title(this, "Camera", 20));
        box.addView(UiKit.muted(this, "Gallery se photo ya video chunein"));

        preview = new Preview(this);
        FrameLayout holder = new FrameLayout(this);
        holder.setBackground(UiKit.round(0xFF050B14, 20, UiKit.LINE));
        holder.addView(preview, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(220)));
        LinearLayout.LayoutParams hp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(220));
        hp.bottomMargin = 16;
        box.addView(holder, hp);

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

        LinearLayout srcRow = UiKit.row(this);
        Button pickImg = UiKit.button(this, "🖼  Photo", UiKit.BLUE, Color.WHITE);
        pickImg.setLayoutParams(UiKit.weight(1f));
        pickImg.setOnClickListener(v -> pick(REQ_IMAGE, "image/*"));
        srcRow.addView(pickImg);
        srcRow.addView(UiKit.spacer(this, 12, 1));
        Button pickVid = UiKit.button(this, "🎬  Video", 0xFF1B2A3E, UiKit.TEXT);
        pickVid.setLayoutParams(UiKit.weight(1f));
        pickVid.setOnClickListener(v -> pick(REQ_VIDEO, "video/*"));
        srcRow.addView(pickVid);
        box.addView(srcRow);

        /* video controls */
        LinearLayout vid = UiKit.card(this);
        vid.addView(UiKit.title(this, "Video", 15));
        LinearLayout vr = UiKit.row(this);
        vr.addView(smallBtn("▶", () -> preview.play()));
        vr.addView(smallBtn("⏸", () -> preview.pause()));
        vr.addView(smallBtn("■", () -> preview.stop()));
        vr.addView(smallBtn("↻", () -> { preview.stop(); preview.play(); }));
        vr.addView(smallBtn("1x", () -> preview.setSpeed(1f)));
        vr.addView(smallBtn("2x", () -> preview.setSpeed(2f)));
        vid.addView(vr);
        box.addView(vid);

        /* image/video processing */
        LinearLayout proc = UiKit.card(this);
        proc.addView(UiKit.title(this, "Processing", 15));
        proc.addView(slider("Zoom", store.zoom(), 40, 300, v -> {
            store.setZoom(v); if (preview != null) { preview.zoom = v / 100f; preview.invalidate(); }
        }));
        proc.addView(slider("Brightness", store.brightness() + 100, 0, 200, v -> {
            int b = v - 100; store.setBrightness(b); if (preview != null) { preview.brightness = b; preview.invalidate(); }
        }));
        proc.addView(slider("Contrast", store.contrast() + 100, 0, 200, v -> {
            int c = v - 100; store.setContrast(c); if (preview != null) { preview.contrast = c; preview.invalidate(); }
        }));
        LinearLayout pr = UiKit.row(this);
        pr.addView(smallBtn("⟳ 90°", () -> {
            int r = (store.rotation() + 90) % 360; store.setRotation(r);
            if (preview != null) { preview.rotationDeg = r; preview.invalidate(); }
        }));
        pr.addView(smallBtn("⇋ Flip", () -> {
            boolean f = !store.flipH(); store.setFlipH(f);
            if (preview != null) { preview.flipH = f; preview.invalidate(); }
        }));
        pr.addView(smallBtn("⇅ Flip", () -> {
            boolean f = !store.flipV(); store.setFlipV(f);
            if (preview != null) { preview.flipV = f; preview.invalidate(); }
        }));
        proc.addView(pr);
        box.addView(proc);

        return padded(UiKit.scroll(this, box));
    }

    private Button smallBtn(String label, Runnable r) {
        Button b = UiKit.button(this, label, 0xFF1B2A3E, UiKit.TEXT);
        b.setTextSize(13f);
        b.setPadding(18, 10, 18, 10);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.rightMargin = 10;
        b.setLayoutParams(lp);
        b.setOnClickListener(v -> r.run());
        return b;
    }

    /* ================= CONTROL ================= */
    private View buildControl() {
        LinearLayout box = UiKit.column(this);
        box.addView(UiKit.title(this, "Control", 20));
        box.addView(UiKit.muted(this, "Face animation aur floating panel"));

        LinearLayout eye = UiKit.card(this);
        eye.addView(UiKit.title(this, "Eyes", 15));
        LinearLayout e1 = UiKit.row(this);
        e1.addView(smallBtn(store.eyesOn() ? "EYE ON" : "EYE OFF", () -> {
            store.setEyesOn(!store.eyesOn()); show("control");
        }));
        e1.addView(smallBtn("BLINK", () -> { if (preview != null) preview.blinkOnce(); }));
        e1.addView(smallBtn("AUTO", () -> {
            store.setAutoBlink(!store.autoBlink());
            if (preview != null) preview.autoBlink = store.autoBlink();
            Toast.makeText(this, "Auto blink: " + (store.autoBlink() ? "ON" : "OFF"), Toast.LENGTH_SHORT).show();
        }));
        eye.addView(e1);
        box.addView(eye);

        LinearLayout mouth = UiKit.card(this);
        mouth.addView(UiKit.title(this, "Mouth", 15));
        LinearLayout m1 = UiKit.row(this);
        m1.addView(smallBtn(store.mouthOn() ? "MOUTH ON" : "MOUTH OFF", () -> {
            store.setMouthOn(!store.mouthOn()); show("control");
        }));
        m1.addView(smallBtn("OPEN", () -> { if (preview != null) preview.setMouthOpen(1f); }));
        m1.addView(smallBtn("CLOSE", () -> { if (preview != null) preview.setMouthOpen(0f); }));
        m1.addView(smallBtn("SMILE", () -> { if (preview != null) preview.setSmile(1f); }));
        mouth.addView(m1);
        box.addView(mouth);

        LinearLayout head = UiKit.card(this);
        head.addView(UiKit.title(this, "Head", 15));
        LinearLayout h1 = UiKit.row(this);
        h1.addView(smallBtn("↑", () -> { if (preview != null) preview.setHead(0, -1, 0); }));
        h1.addView(smallBtn("↓", () -> { if (preview != null) preview.setHead(0, 1, 0); }));
        h1.addView(smallBtn("←", () -> { if (preview != null) preview.setHead(-1, 0, -6); }));
        h1.addView(smallBtn("→", () -> { if (preview != null) preview.setHead(1, 0, 6); }));
        h1.addView(smallBtn("CENTER", () -> { if (preview != null) preview.setHead(0, 0, 0); }));
        head.addView(h1);
        box.addView(head);

        LinearLayout fl = UiKit.card(this);
        fl.addView(UiKit.title(this, "Floating panel", 15));
        fl.addView(UiKit.muted(this, "Game/app ke upar chhota control panel"));
        LinearLayout fr = UiKit.row(this);
        fr.addView(smallBtn(OverlayPanelService.running ? "BAND KAREIN" : "CHALU KAREIN", () -> toggleFloating()));
        fl.addView(fr);
        box.addView(fl);

        return padded(UiKit.scroll(this, box));
    }

    private void toggleFloating() {
        if (OverlayPanelService.running) {
            stopService(new Intent(this, OverlayPanelService.class));
            Toast.makeText(this, "Floating panel band", Toast.LENGTH_SHORT).show();
            show("control");
            return;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName())));
            Toast.makeText(this, "Overlay permission dein, phir dobara dabayein", Toast.LENGTH_LONG).show();
            return;
        }
        startService(new Intent(this, OverlayPanelService.class));
        Toast.makeText(this, "Floating panel chalu", Toast.LENGTH_SHORT).show();
        show("control");
    }

    /* ================= STREAMING ================= */
    private View buildStreaming() {
        LinearLayout box = UiKit.column(this);
        box.addView(UiKit.title(this, "Streaming", 20));
        box.addView(UiKit.muted(this, "RTMP live streaming"));

        LinearLayout c = UiKit.card(this);
        c.addView(UiKit.title(this, "Server", 15));
        EditText url = new EditText(this);
        url.setHint("rtmp://server/app");
        url.setText(store.rtmpUrl());
        url.setTextColor(UiKit.TEXT);
        url.setHintTextColor(UiKit.MUTED);
        c.addView(url);
        EditText key = new EditText(this);
        key.setHint("Stream key");
        key.setText(store.streamKey());
        key.setTextColor(UiKit.TEXT);
        key.setHintTextColor(UiKit.MUTED);
        c.addView(key);
        Button save = UiKit.button(this, "Save config", 0xFF1B2A3E, UiKit.TEXT);
        save.setOnClickListener(v -> {
            store.setRtmpUrl(url.getText().toString().trim());
            store.setStreamKey(key.getText().toString().trim());
            Toast.makeText(this, "Config save", Toast.LENGTH_SHORT).show();
        });
        c.addView(save, UiKit.matchWrap(10));
        box.addView(c);

        LinearLayout info = UiKit.card(this);
        info.addView(UiKit.title(this, "Status", 15));
        info.addView(UiKit.body(this,
                "RTMP streaming is not available in this build.\n\n"
              + "Kyun: RTMP ke liye ek video encoder + protocol stack chahiye. "
              + "Is APK mein woh include nahi hai, isliye main fake 'Connected' status nahi dikhata.\n\n"
              + "Jo kaam karta hai: photo/video preview, face animation, processing, floating panel.",
                13f, UiKit.MUTED));
        box.addView(info);

        return padded(UiKit.scroll(this, box));
    }

    /* ================= SETTINGS ================= */
    private View buildSettings() {
        LinearLayout box = UiKit.column(this);
        box.addView(UiKit.title(this, "Settings", 20));

        LinearLayout c = UiKit.card(this);
        c.addView(UiKit.title(this, "Animation", 15));
        c.addView(slider("Animation speed", store.animSpeed(), 0, 100, v -> {
            store.setAnimSpeed(v); if (preview != null) preview.animSpeed = v;
        }));
        c.addView(slider("Blink interval", store.blinkInterval() / 50, 10, 100, v -> {
            int ms = v * 50; store.setBlinkInterval(ms); if (preview != null) preview.blinkInterval = ms;
        }));
        box.addView(c);

        LinearLayout about = UiKit.card(this);
        about.addView(UiKit.title(this, "About", 15));
        about.addView(UiKit.body(this,
                "CamVivr v1.0\n\n"
              + "Privacy: aapki photo/video phone se bahar nahi jaati. "
              + "Koi server upload nahi, koi tracking nahi. Stream key sirf phone mein save hoti hai.\n\n"
              + "Known limitations:\n"
              + "• External virtual camera: device support par depend karta hai (zyada phones par nahi hota).\n"
              + "• RTMP streaming: is build mein nahi hai.\n"
              + "• Face animation: 2D puppet (3D mesh nahi).\n"
              + "• Video color correction: tint-based (image par poora ColorMatrix).",
                13f, UiKit.MUTED));
        box.addView(about);

        LinearLayout reset = UiKit.card(this);
        reset.addView(UiKit.title(this, "Reset", 15));
        Button r = UiKit.button(this, "Reset app data", 0xFF3A1E22, UiKit.RED);
        r.setOnClickListener(v -> {
            store.resetAll();
            Toast.makeText(this, "Reset ho gaya", Toast.LENGTH_SHORT).show();
            show("settings");
        });
        reset.addView(r, UiKit.matchWrap(10));
        box.addView(reset);

        return padded(UiKit.scroll(this, box));
    }

    /* ================= helpers ================= */
    private LinearLayout slider(String label, int value, int min, int max, final OnValue cb) {
        LinearLayout wrap = new LinearLayout(this);
        wrap.setOrientation(LinearLayout.VERTICAL);
        wrap.setPadding(0, 12, 0, 2);
        TextView t = UiKit.body(this, label + ": " + value, 13f, UiKit.TEXT);
        wrap.addView(t);
        SeekBar sb = new SeekBar(this);
        sb.setMax(max - min);
        sb.setProgress(Math.max(0, Math.min(max - min, value - min)));
        sb.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar s, int p, boolean fromUser) {
                int val = min + p;
                t.setText(label + ": " + val);
                cb.onValue(val);
            }
            public void onStartTrackingTouch(SeekBar s) { }
            public void onStopTrackingTouch(SeekBar s) { }
        });
        wrap.addView(sb);
        return wrap;
    }

    interface OnValue { void onValue(int v); }

    private int dp(int v) { return (int) (v * getResources().getDisplayMetrics().density); }

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
        openUri(uri, req == REQ_IMAGE ? "image" : "video");
    }

    private void openUri(Uri uri, String type) {
        if (preview == null) { show("camera"); return; }
        boolean ok = type.equals("image") ? preview.loadImage(uri) : preview.loadVideo(uri);
        if (ok) {
            store.setSource(type);
            if (type.equals("image")) store.setImageUri(uri.toString()); else store.setVideoUri(uri.toString());
            store.addRecent(uri.getLastPathSegment() == null ? type : uri.getLastPathSegment(), uri.toString(), type);
            if (type.equals("video")) preview.play();
            Toast.makeText(this, type + " load ho gaya", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Ye file khul nahi payi (unsupported format)", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onDestroy() {
        if (preview != null) preview.release();
        super.onDestroy();
    }
}
