package com.camvivr.app;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Engine — media engine ka asli state + FPS counter + in-app logs.
 * State hamesha real hota hai (koi fake RUNNING nahi).
 */
public class Engine {

    public enum State { IDLE, INITIALIZING, READY, PLAYING, PAUSED, STOPPED, ERROR }

    public interface Listener { void onState(State s); }

    private static Engine instance;
    public static Engine get() {
        if (instance == null) instance = new Engine();
        return instance;
    }

    private State state = State.IDLE;
    private String error = "";
    private Listener listener;

    /* fps */
    private long fpsWindowStart = 0;
    private int framesInWindow = 0;
    private int fps = 0;

    /* logs */
    public static class LogLine {
        public final String time, tag, msg;
        LogLine(String t, String tag, String msg) { this.time = t; this.tag = tag; this.msg = msg; }
    }
    private final List<LogLine> logs = new ArrayList<>();

    public State state() { return state; }
    public String error() { return error; }
    public int fps() { return fps; }
    public List<LogLine> logs() { return logs; }

    public void setListener(Listener l) { listener = l; }

    public void set(State s) {
        state = s;
        log("ENGINE", "state -> " + s.name());
        if (listener != null) listener.onState(s);
    }

    public void fail(String msg) {
        error = msg;
        set(State.ERROR);
        log("ERROR", msg);
    }

    /** Har render frame par call — asli FPS nikalta hai. */
    public void frame() {
        long now = System.currentTimeMillis();
        if (fpsWindowStart == 0) fpsWindowStart = now;
        framesInWindow++;
        if (now - fpsWindowStart >= 1000) {
            fps = (int) (framesInWindow * 1000.0 / (now - fpsWindowStart));
            framesInWindow = 0;
            fpsWindowStart = now;
        }
    }

    public void log(String tag, String msg) {
        String t = new SimpleDateFormat("HH:mm:ss", Locale.US).format(new Date());
        logs.add(0, new LogLine(t, tag, msg));
        while (logs.size() > 200) logs.remove(logs.size() - 1);
    }

    public void clearLogs() { logs.clear(); }
}
