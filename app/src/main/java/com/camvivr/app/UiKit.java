package com.camvivr.app;

import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/** CamVivr UI kit — dark navy + blue accent, rounded cards. */
public class UiKit {

    public static final int BG = 0xFF0A1220;        // dark navy
    public static final int BG2 = 0xFF0E1A2B;
    public static final int CARD = 0xFF132234;
    public static final int LINE = 0xFF20344B;
    public static final int BLUE = 0xFF3B82F6;
    public static final int BLUE_L = 0xFF60A5FA;
    public static final int GREEN = 0xFF22C55E;
    public static final int RED = 0xFFEF4444;
    public static final int AMBER = 0xFFF59E0B;
    public static final int TEXT = 0xFFF1F5F9;
    public static final int MUTED = 0xFF8FA3BF;

    public static TextView title(Context c, String t, float size) {
        TextView v = new TextView(c);
        v.setText(t);
        v.setTextColor(TEXT);
        v.setTextSize(size);
        v.setTypeface(Typeface.DEFAULT_BOLD);
        return v;
    }

    public static TextView body(Context c, String t, float size, int color) {
        TextView v = new TextView(c);
        v.setText(t);
        v.setTextColor(color);
        v.setTextSize(size);
        v.setLineSpacing(7f, 1f);
        return v;
    }

    public static TextView muted(Context c, String t) { return body(c, t, 12.5f, MUTED); }

    public static GradientDrawable round(int color, float radius, int stroke) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(color);
        g.setCornerRadius(radius);
        if (stroke != 0) g.setStroke(2, stroke);
        return g;
    }

    public static Button button(Context c, String label, int bg, int fg) {
        Button b = new Button(c);
        b.setText(label);
        b.setAllCaps(false);
        b.setTextColor(fg);
        b.setTextSize(14f);
        b.setTypeface(Typeface.DEFAULT_BOLD);
        b.setBackground(round(bg, 24, 0));
        b.setPadding(24, 14, 24, 14);
        b.setMinHeight(0);
        b.setMinimumHeight(0);
        return b;
    }

    public static LinearLayout card(Context c) {
        LinearLayout l = new LinearLayout(c);
        l.setOrientation(LinearLayout.VERTICAL);
        l.setBackground(round(CARD, 26, LINE));
        l.setPadding(30, 26, 30, 26);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = 18;
        l.setLayoutParams(lp);
        return l;
    }

    public static LinearLayout column(Context c) {
        LinearLayout l = new LinearLayout(c);
        l.setOrientation(LinearLayout.VERTICAL);
        l.setBackgroundColor(BG);
        l.setPadding(32, 26, 32, 32);
        return l;
    }

    public static LinearLayout row(Context c) {
        LinearLayout l = new LinearLayout(c);
        l.setOrientation(LinearLayout.HORIZONTAL);
        l.setGravity(Gravity.CENTER_VERTICAL);
        return l;
    }

    public static LinearLayout.LayoutParams weight(float w) {
        return new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, w);
    }

    public static LinearLayout.LayoutParams matchWrap(int top) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = top;
        return lp;
    }

    public static View spacer(Context c, int w, int h) {
        View v = new View(c);
        v.setLayoutParams(new LinearLayout.LayoutParams(w, h));
        return v;
    }

    public static ScrollView scroll(Context c, View content) {
        ScrollView sv = new ScrollView(c);
        sv.setBackgroundColor(BG);
        sv.setFillViewport(true);
        sv.addView(content);
        return sv;
    }
}
