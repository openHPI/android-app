package de.xikolo.lanalytics.util;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.WindowManager;

import java.util.LinkedHashMap;
import java.util.Map;

import de.xikolo.lanalytics.Lanalytics;

public class ContextUtil {

    public static Map<String, String> getDefaultContextData(Context context) {
        Map<String, String> contextMap = new LinkedHashMap<>();

        // os details
        contextMap.put("platform", "Android");
        contextMap.put("platform_version", Build.VERSION.RELEASE);
        contextMap.put("runtime", "Android");
        contextMap.put("runtime_version", Build.VERSION.RELEASE);
        contextMap.put("runtime_api_level", String.valueOf(Build.VERSION.SDK_INT));

        // device details
        contextMap.put("device", DeviceUtil.getDeviceName());

        // app details
        try {
            PackageInfo pi = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            contextMap.put("build_version", String.valueOf(pi.versionCode));
            contextMap.put("build_version_name", String.valueOf(pi.versionName));
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(Lanalytics.class.getSimpleName(), "Could not get package info for " + context.getPackageName(), e);
        }

        // screen details
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        if (wm != null) {
            DisplayMetrics metrics = new DisplayMetrics();
            wm.getDefaultDisplay().getRealMetrics(metrics);
            contextMap.put("screen_width", String.valueOf(metrics.widthPixels));
            contextMap.put("screen_height", String.valueOf(metrics.heightPixels));
            contextMap.put("screen_density", String.valueOf((int) metrics.density * 160));
        }

        // network details
        String network = "unknown";
        switch (NetworkUtil.getConnectivityStatus(context)) {
            case WIFI:
                network = "wifi";
                break;
            case MOBILE:
                network = "mobile";
                break;
            case NOT_CONNECTED:
                network = "offline";
                break;
        }
        contextMap.put("network", network);

        return contextMap;
    }

}
