package com.camvivr.app;

import android.os.Build;

/**
 * ExternalCamera — kya ye device doosre apps ko hamara camera feed de sakta hai?
 *
 * Android normally kisi app ko doosre app ka camera replace karne nahi deta.
 * Ye class sirf honest detection karti hai (koi fake "supported" nahi).
 */
public class ExternalCamera {

    public static boolean supported() {
        // Android 14+ ke kuch builds mein virtual camera support hota hai,
        // par sirf system/OEM apps ke liye. Normal app ke liye nahi.
        return false;
    }

    public static String reason() {
        if (Build.VERSION.SDK_INT < 29)
            return "Android 10 se pehle virtual camera API nahi hai.";
        return "Is device par system-level virtual camera support nahi hai. "
             + "Normal apps doosre apps ka camera replace nahi kar sakte — "
             + "iske liye root/system privileges chahiye.";
    }
}
