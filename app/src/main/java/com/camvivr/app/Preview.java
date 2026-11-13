package com.camvivr.app;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.SurfaceTexture;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.TextureView;
import android.view.View;
import android.widget.FrameLayout;

import java.io.InputStream;

/**
 * Preview — image/video renderer + face animation overlay.
 * Image: Canvas + ColorMatrix (brightness/contrast) + matrix (zoom/rotate/flip).
 * Video: TextureView + setTransform + tint overlay.
 */
public class Preview extends FrameLayout {

    public interface State { void onChange(); }

    private final TextureView texture;
    private final ImageView imgView;
    private final OverlayView overlay;
    private final Paint tint = new Paint();
    private final Paint animPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private Bitmap bitmap;
    private MediaPlayer player;
    private boolean prepared = false;
    private String videoUri = "";

    private final FaceAnim face = new FaceAnim();
    private final Handler h = new Handler(Looper.getMainLooper());
    private boolean animRunning = false;
    private long animStart = 0;

    public float zoom = 1f;
    public int rotationDeg = 0;
    public boolean flipH = false, flipV = false;
    public int brightness = 0, contrast = 0;
    public boolean eyesOn = true, mouthOn = true, autoBlink = false;
    public int blinkInterval = 3000, animSpeed = 50;
    public String mode = "none";   // none | image | video

    public Preview(Context c) {
        super(c);
        setBackgroundColor(0xFF050B14);

        imgView = new ImageView(c);
        addView(imgView, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));

        texture = new TextureView(c);
        texture.setVisibility(GONE);
        texture.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
            public void onSurfaceTextureAvailable(SurfaceTexture st, int w, int h) {
                if (pendingVideo != null) {
                    Uri u = pendingVideo;
                    pendingVideo = null;
                    startPlayer(u);
                }
            }
            public void onSurfaceTextureSizeChanged(SurfaceTexture st, int w, int h) { }
            public boolean onSurfaceTextureDestroyed(SurfaceTexture st) { return true; }
            public void onSurfaceTextureUpdated(SurfaceTexture st) { }
        });
        addView(texture, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));

        overlay = new OverlayView(c);
        addView(overlay, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
    }

    /* ================= image ================= */
    public boolean loadImage(Uri uri) {
        release();
        try {
            InputStream is = getContext().getContentResolver().openInputStream(uri);
            BitmapFactory.Options o = new BitmapFactory.Options();
            o.inSampleSize = 2;
            bitmap = BitmapFactory.decodeStream(is, null, o);
            if (is != null) is.close();
            if (bitmap == null) return false;
            mode = "image";
            mediaW = bitmap.getWidth(); mediaH = bitmap.getHeight();
            mediaDurationMs = 0; hasAudio = false;
            Engine.get().set(Engine.State.READY);
            Engine.get().log("SOURCE", "image " + mediaW + "x" + mediaH);
            texture.setVisibility(GONE);
            imgView.setVisibility(VISIBLE);
            face.detect(bitmap);
            applyImageTransform();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private void applyImageTransform() {
        imgView.invalidate();
    }

    /* ================= video ================= */
    private Uri pendingVideo;

    public boolean loadVideo(Uri uri) {
        release();
        mode = "video";
        imgView.setVisibility(GONE);
        texture.setVisibility(VISIBLE);
        videoUri = uri.toString();
        if (texture.getSurfaceTexture() == null) {
            // view abhi attach nahi hui — surface aane par khud load hoga
            pendingVideo = uri;
            Engine.get().log("VIDEO", "surface ka intezaar");
            return true;
        }
        return startPlayer(uri);
    }

    private boolean startPlayer(Uri uri) {
        try {
            player = new MediaPlayer();
            player.setDataSource(getContext(), uri);
            player.setSurface(new android.view.Surface(texture.getSurfaceTexture()));
            player.setLooping(true);
            player.setOnPreparedListener(mp -> {
                prepared = true;
                mediaW = mp.getVideoWidth(); mediaH = mp.getVideoHeight();
                mediaDurationMs = mp.getDuration();
                hasAudio = true;
                Engine.get().set(Engine.State.READY);
                Engine.get().log("SOURCE", "video " + mediaW + "x" + mediaH + " " + mediaDurationMs + "ms");
                mp.start();
                Engine.get().set(Engine.State.PLAYING);
            });
            player.setOnErrorListener((mp, what, extra) -> {
                Engine.get().fail("Video decoder error (" + what + "/" + extra + ")");
                return true;
            });
            player.prepareAsync();
            return true;
        } catch (Exception e) {
            Engine.get().fail("video load fail: " + e.getMessage());
            return false;
        }
    }

    public void play() { if (player != null && prepared) { player.start(); startAnim(); } }
    public void pause() { if (player != null && prepared) { player.pause(); } }
    public void stop() { if (player != null && prepared) { player.pause(); player.seekTo(0); } }
    public void seekTo(int ms) { if (player != null && prepared) player.seekTo(ms); }
    public int duration() { return (player != null && prepared) ? player.getDuration() : 0; }
    public int position() { return (player != null && prepared) ? player.getCurrentPosition() : 0; }
    public void setLoop(boolean loop) { if (player != null) player.setLooping(loop); }

    private boolean muted = false;
    public void mute() {
        muted = !muted;
        if (player != null) {
            try { player.setVolume(muted ? 0f : 1f, muted ? 0f : 1f); } catch (Exception ignored) { }
        }
    }

    public void seekPercent(int pct) {
        if (player != null && prepared && mediaDurationMs > 0) {
            player.seekTo((int) (mediaDurationMs * Math.max(0, Math.min(100, pct)) / 100.0));
        }
    }
    public void setSpeed(float s) {
        if (player == null || !prepared) return;
        try {
            if (android.os.Build.VERSION.SDK_INT >= 23) {
                player.setPlaybackParams(player.getPlaybackParams().setSpeed(Math.max(0.25f, Math.min(3f, s))));
            }
        } catch (Exception ignored) { }
    }

    public void release() {
        try { if (player != null) { player.release(); player = null; prepared = false; } } catch (Exception ignored) { }
        if (bitmap != null) { bitmap.recycle(); bitmap = null; }
        mode = "none";
        stopAnim();
    }

    /* ================= animation ================= */
    public void startAnim() {
        if (animRunning) return;
        animRunning = true;
        animStart = System.currentTimeMillis();
        h.post(tick);
    }

    public void stopAnim() { animRunning = false; h.removeCallbacks(tick); }

    private final Runnable tick = new Runnable() {
        public void run() {
            if (!animRunning) return;
            long t = System.currentTimeMillis() - animStart;
            float speed = 0.4f + (animSpeed / 100f) * 1.6f;

            // auto blink
            if (autoBlink) {
                long phase = t % Math.max(800, blinkInterval);
                float p = phase / (float) Math.max(800, blinkInterval);
                face.blink = (p > 0.92f) ? (float) Math.sin((p - 0.92f) / 0.08f * Math.PI) : 0f;
            }
            // mouth auto movement
            if (mouthOn) {
                face.mouthOpen = (float) (0.5 + 0.5 * Math.sin(t / 400.0 * speed));
            }
            overlay.invalidate();
            Engine.get().frame();
            h.postDelayed(this, 33);
        }
    };

    public void blinkOnce() {
        face.blink = 1f;
        overlay.invalidate();
        h.postDelayed(() -> { face.blink = 0f; overlay.invalidate(); }, 130);
    }

    public void setMouthOpen(float v) { face.mouthOpen = v; overlay.invalidate(); }
    public void setSmile(float v) { face.smile = v; overlay.invalidate(); }
    public void setHead(float x, float y, float rot) { face.headX = x; face.headY = y; face.headRot = rot; overlay.invalidate(); }
    public FaceAnim face() { return face; }

    /* ---------- media info (asli) ---------- */
    public String mediaName = "";
    public int mediaW = 0, mediaH = 0;
    public long mediaDurationMs = 0;
    public boolean hasAudio = false;

    public String infoLine() {
        if (mode.equals("none")) return "No source";
        if (mode.equals("image"))
            return mediaW + "×" + mediaH + " · image";
        return mediaW + "×" + mediaH + " · " + (mediaDurationMs / 1000) + "s · "
                + (hasAudio ? "audio" : "no audio");
    }

    /** Current frame ko PNG mein save karo (asli snapshot). */
    public boolean snapshot(java.io.File out) {
        try {
            int w = getWidth(), h = getHeight();
            if (w <= 0 || h <= 0) return false;
            Bitmap bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
            Canvas c = new Canvas(bmp);
            if (mode.equals("image")) {
                imgView.draw(c);
            } else {
                Bitmap frame = texture.getBitmap(w, h);
                if (frame != null) c.drawBitmap(frame, 0, 0, null);
            }
            overlay.draw(c);
            java.io.FileOutputStream fos = new java.io.FileOutputStream(out);
            bmp.compress(Bitmap.CompressFormat.PNG, 95, fos);
            fos.close();
            bmp.recycle();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /* ================= views ================= */
    private class ImageView extends View {
        private final Paint p = new Paint(Paint.FILTER_BITMAP_FLAG);

        ImageView(Context c) { super(c); }

        @Override
        protected void onDraw(Canvas c) {
            if (bitmap == null) return;
            float vw = getWidth(), vh = getHeight();
            float bw = bitmap.getWidth(), bh = bitmap.getHeight();
            float scale = Math.min(vw / bw, vh / bh) * zoom;

            Matrix m = new Matrix();
            m.postScale(scale, scale);
            m.postScale(flipH ? -1 : 1, flipV ? -1 : 1);
            m.postRotate(rotationDeg);
            m.postTranslate(vw / 2 + face.headX * 40, vh / 2 + face.headY * 40);
            m.preTranslate(-bw / 2, -bh / 2);

            ColorMatrix cm = new ColorMatrix();
            cm.set(new float[]{
                    contrastF(), 0, 0, 0, brightnessF(),
                    0, contrastF(), 0, 0, brightnessF(),
                    0, 0, contrastF(), 0, brightnessF(),
                    0, 0, 0, 1, 0});
            p.setColorFilter(new ColorMatrixColorFilter(cm));
            c.drawBitmap(bitmap, m, p);
        }

        private float contrastF() { return 1f + contrast / 100f; }
        private float brightnessF() { return brightness * 1.2f; }
    }

    private class OverlayView extends View {
        private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);

        OverlayView(Context c) { super(c); }

        @Override
        protected void onDraw(Canvas c) {
            if (mode.equals("video")) {
                // video par brightness/contrast ka asar: halka tint
                if (brightness != 0) {
                    tint.setColor(brightness > 0 ? Color.WHITE : Color.BLACK);
                    tint.setAlpha(Math.min(120, Math.abs(brightness)));
                    c.drawRect(0, 0, getWidth(), getHeight(), tint);
                }
            }
            if (eyesOn || mouthOn) {
                face.draw(c, getWidth(), getHeight(), p);
            }
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        stopAnim();
        super.onDetachedFromWindow();
    }
}
