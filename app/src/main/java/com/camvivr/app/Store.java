package com.camvivr.app;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/** Store — settings, recent media aur stream config (local only). */
public class Store {

    private static final String P = "camvivr";
    private final SharedPreferences prefs;

    public Store(Context c) { prefs = c.getApplicationContext().getSharedPreferences(P, Context.MODE_PRIVATE); }

    /* ---------- source ---------- */
    public String imageUri() { return prefs.getString("image_uri", ""); }
    public void setImageUri(String u) { prefs.edit().putString("image_uri", u).apply(); }

    public String videoUri() { return prefs.getString("video_uri", ""); }
    public void setVideoUri(String u) { prefs.edit().putString("video_uri", u).apply(); }

    public String source() { return prefs.getString("source", "none"); }   // none | image | video
    public void setSource(String s) { prefs.edit().putString("source", s).apply(); }

    /* ---------- processing ---------- */
    public int brightness() { return prefs.getInt("brightness", 0); }        // -100..100
    public void setBrightness(int v) { prefs.edit().putInt("brightness", v).apply(); }

    public int contrast() { return prefs.getInt("contrast", 0); }
    public void setContrast(int v) { prefs.edit().putInt("contrast", v).apply(); }

    public int zoom() { return prefs.getInt("zoom", 100); }
    public void setZoom(int v) { prefs.edit().putInt("zoom", v).apply(); }

    public int rotation() { return prefs.getInt("rotation", 0); }
    public void setRotation(int v) { prefs.edit().putInt("rotation", v).apply(); }

    public boolean flipH() { return prefs.getBoolean("flipH", false); }
    public void setFlipH(boolean v) { prefs.edit().putBoolean("flipH", v).apply(); }

    public boolean flipV() { return prefs.getBoolean("flipV", false); }
    public void setFlipV(boolean v) { prefs.edit().putBoolean("flipV", v).apply(); }

    /* ---------- animation ---------- */
    public boolean eyesOn() { return prefs.getBoolean("eyes", true); }
    public void setEyesOn(boolean v) { prefs.edit().putBoolean("eyes", v).apply(); }

    public boolean mouthOn() { return prefs.getBoolean("mouth", true); }
    public void setMouthOn(boolean v) { prefs.edit().putBoolean("mouth", v).apply(); }

    public boolean autoBlink() { return prefs.getBoolean("autoBlink", false); }
    public void setAutoBlink(boolean v) { prefs.edit().putBoolean("autoBlink", v).apply(); }

    public int blinkInterval() { return prefs.getInt("blinkInterval", 3000); }
    public void setBlinkInterval(int v) { prefs.edit().putInt("blinkInterval", v).apply(); }

    public int animSpeed() { return prefs.getInt("animSpeed", 50); }
    public void setAnimSpeed(int v) { prefs.edit().putInt("animSpeed", v).apply(); }

    /* ---------- stream ---------- */
    public String rtmpUrl() { return prefs.getString("rtmp_url", ""); }
    public void setRtmpUrl(String v) { prefs.edit().putString("rtmp_url", v).apply(); }

    public String streamKey() { return prefs.getString("stream_key", ""); }
    public void setStreamKey(String v) { prefs.edit().putString("stream_key", v).apply(); }

    /* ---------- recent files ---------- */
    public void addRecent(String name, String uri, String type) {
        try {
            JSONArray arr = new JSONArray(prefs.getString("recent", "[]"));
            JSONArray out = new JSONArray();
            JSONObject o = new JSONObject();
            o.put("name", name); o.put("uri", uri); o.put("type", type);
            out.put(o);
            for (int i = 0; i < arr.length() && out.length() < 8; i++) {
                JSONObject e = arr.getJSONObject(i);
                if (!e.optString("uri").equals(uri)) out.put(e);
            }
            prefs.edit().putString("recent", out.toString()).apply();
        } catch (JSONException ignored) { }
    }

    public List<String[]> recent() {
        List<String[]> list = new ArrayList<>();
        try {
            JSONArray arr = new JSONArray(prefs.getString("recent", "[]"));
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                list.add(new String[]{o.optString("name"), o.optString("uri"), o.optString("type")});
            }
        } catch (JSONException ignored) { }
        return list;
    }

    /* ---------- sources (multiple) ---------- */
    public void addSource(String name, String uri, String type) {
        try {
            JSONArray arr = new JSONArray(prefs.getString("sources", "[]"));
            JSONArray out = new JSONArray();
            JSONObject o = new JSONObject();
            o.put("name", name); o.put("uri", uri); o.put("type", type);
            out.put(o);
            for (int i = 0; i < arr.length() && out.length() < 12; i++) {
                JSONObject e = arr.getJSONObject(i);
                if (!e.optString("uri").equals(uri)) out.put(e);
            }
            prefs.edit().putString("sources", out.toString()).apply();
        } catch (JSONException ignored) { }
    }

    public List<String[]> sources() {
        List<String[]> list = new ArrayList<>();
        try {
            JSONArray arr = new JSONArray(prefs.getString("sources", "[]"));
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                list.add(new String[]{o.optString("name"), o.optString("uri"), o.optString("type")});
            }
        } catch (JSONException ignored) { }
        return list;
    }

    public void removeSource(String uri) {
        try {
            JSONArray arr = new JSONArray(prefs.getString("sources", "[]"));
            JSONArray out = new JSONArray();
            for (int i = 0; i < arr.length(); i++) {
                JSONObject e = arr.getJSONObject(i);
                if (!e.optString("uri").equals(uri)) out.put(e);
            }
            prefs.edit().putString("sources", out.toString()).apply();
        } catch (JSONException ignored) { }
    }

    /* ---------- action timings ---------- */
    public void addTiming(String action, int startMs, int endMs) {
        try {
            JSONArray arr = new JSONArray(prefs.getString("timings", "[]"));
            JSONObject o = new JSONObject();
            o.put("action", action); o.put("start", startMs); o.put("end", endMs);
            arr.put(o);
            prefs.edit().putString("timings", arr.toString()).apply();
        } catch (JSONException ignored) { }
    }

    public List<Object[]> timings() {
        List<Object[]> list = new ArrayList<>();
        try {
            JSONArray arr = new JSONArray(prefs.getString("timings", "[]"));
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                list.add(new Object[]{o.optString("action"), o.optInt("start"), o.optInt("end")});
            }
        } catch (JSONException ignored) { }
        return list;
    }

    public void clearTimings() { prefs.edit().putString("timings", "[]").apply(); }

    public void resetAll() { prefs.edit().clear().apply(); }
}
