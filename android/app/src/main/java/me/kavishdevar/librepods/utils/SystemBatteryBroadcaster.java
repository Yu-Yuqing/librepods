package me.kavishdevar.librepods.utils;

import android.app.BroadcastOptions;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

/** App-side sender intentionally has no dependency on the compile-only Xposed API. */
public final class SystemBatteryBroadcaster {
    private SystemBatteryBroadcaster() { }

    public static void send(Context context, Intent state) {
        // Sender UID sharing, required for receiver authentication, was added in Android 14.
        if (Build.VERSION.SDK_INT < 34) return;
        Intent out = new Intent("me.kavishdevar.librepods.SYSTEM_BATTERY_STATE")
                .setPackage("com.android.bluetooth")
                .addFlags(Intent.FLAG_RECEIVER_FOREGROUND)
                .putExtra("address", state.getStringExtra("address"))
                .putExtra("connected", state.getBooleanExtra("connected", false));
        for (String key : new String[]{"left_battery", "right_battery", "case_battery"})
            out.putExtra(key, state.getIntExtra(key, -1));
        for (String key : new String[]{"left_charging", "right_charging", "case_charging"})
            out.putExtra(key, state.getBooleanExtra(key, false));
        BroadcastOptions options = BroadcastOptions.makeBasic();
        options.setShareIdentityEnabled(true);
        try {
            context.sendBroadcast(out, null, options.toBundle());
        } catch (RuntimeException error) {
            Log.w("LibrePodsSystemBattery", "System battery broadcast failed", error);
        }
    }
}
