package com.camvivr.app;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.RectF;
import android.media.FaceDetector;

/**
 * FaceAnim — 2D puppet animation.
 *
 * Face detection se aankh/muh ki jagah nikalte hain, phir blink / mouth / head
 * animation canvas par draw karte hain. (3D mesh nahi — honest 2D puppet.)
 */
public class FaceAnim {

    public boolean faceFound = false;
    public RectF faceBox = new RectF();      // image ke coordinates mein (0..1)
    public PointF eyeL = new PointF(), eyeR = new PointF(), mouth = new PointF();
    public float faceW = 0.3f;

    /* animation state (0..1) */
    public float blink = 0f;          // 1 = aankh band
    public float mouthOpen = 0f;
    public float smile = 0f;
    public float headX = 0f, headY = 0f, headRot = 0f;

    private Bitmap analysed;
    private int analysedW, analysedH;

    /** Bitmap par chehra dhundo (ek hi baar per bitmap). */
    public boolean detect(Bitmap bmp) {
        if (bmp == null) return false;
        if (bmp == analysed) return faceFound;
        analysed = bmp;
        faceFound = false;
        try {
            int w = bmp.getWidth(), h = bmp.getHeight();
            int maxW = 512;
            float scale = w > maxW ? maxW / (float) w : 1f;
            int dw = Math.max(2, (int) (w * scale)) & ~1;
            int dh = Math.max(2, (int) (h * scale));
            Bitmap small = Bitmap.createScaledBitmap(bmp, dw, dh, false);
            analysedW = dw; analysedH = dh;

            FaceDetector.Face[] faces = new FaceDetector.Face[4];
            FaceDetector fd = new FaceDetector(dw, dh, faces.length);
            int n = fd.findFaces(small, faces);
            if (n > 0) {
                FaceDetector.Face f = faces[0];
                PointF mid = new PointF();
                f.getMidPoint(mid);
                float eyesDist = f.eyesDistance();

                faceW = (eyesDist * 2.4f) / dw;
                float cx = mid.x / dw, cy = mid.y / dh;

                faceBox.set(cx - faceW / 2, cy - faceW * 0.6f, cx + faceW / 2, cy + faceW * 0.7f);

                float eyeY = cy - faceW * 0.10f;
                eyeL.set(cx - faceW * 0.22f, eyeY);
                eyeR.set(cx + faceW * 0.22f, eyeY);
                mouth.set(cx, cy + faceW * 0.34f);
                faceFound = true;
            }
            if (small != bmp) small.recycle();
        } catch (Exception ignored) { }
        return faceFound;
    }

    /** Animation overlay canvas par draw karo (canvas already image-space mein). */
    public void draw(Canvas c, float w, float h, Paint p) {
        if (!faceFound) return;

        float fx = faceBox.left * w, fy = faceBox.top * h;
        float fw = (faceBox.right - faceBox.left) * w;

        int skin = Color.argb(230, 235, 205, 180);
        int dark = Color.argb(235, 60, 35, 40);

        /* ---- blink: aankhon par palk ---- */
        if (blink > 0.02f) {
            p.setColor(skin);
            float ew = fw * 0.20f, eh = fw * 0.16f * Math.min(1f, blink);
            c.drawOval(eyeL.x * w - ew / 2, eyeL.y * h - eh / 2,
                    eyeL.x * w + ew / 2, eyeL.y * h + eh / 2, p);
            c.drawOval(eyeR.x * w - ew / 2, eyeR.y * h - eh / 2,
                    eyeR.x * w + ew / 2, eyeR.y * h + eh / 2, p);
        }

        /* ---- mouth ---- */
        if (mouthOpen > 0.02f) {
            p.setColor(dark);
            float mw = fw * 0.34f, mh = fw * 0.10f + fw * 0.26f * mouthOpen;
            c.drawOval(mouth.x * w - mw / 2, mouth.y * h - mh / 2,
                    mouth.x * w + mw / 2, mouth.y * h + mh / 2, p);
        }

        /* ---- smile: halki curve ---- */
        if (smile > 0.02f) {
            p.setColor(Color.argb(200, 120, 40, 60));
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(Math.max(2f, fw * 0.02f));
            RectF arc = new RectF(mouth.x * w - fw * 0.18f, mouth.y * h - fw * 0.10f,
                    mouth.x * w + fw * 0.18f, mouth.y * h + fw * 0.10f + fw * 0.10f * smile);
            c.drawArc(arc, 20, 140, false, p);
            p.setStyle(Paint.Style.FILL);
        }
    }
}
