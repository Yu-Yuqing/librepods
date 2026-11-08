package me.kavishdevar.librepods.utils;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.util.Log;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import io.github.libxposed.api.XposedModule;
import io.github.libxposed.api.XposedModuleInterface.PackageLoadedParam;

/**
 * Synchronizes live AACP battery data into the Bluetooth service's normal HFP battery cache.
 * Runs through LibrePods' existing Bluetooth LSPosed scope; creates no Bluetooth connection.
 */
public final class SystemBatterySync {
    private static final String PODS = "me.kavishdevar.librepods";
    private static final String BLUETOOTH = "com.android.bluetooth";
    private static final String ACTION = PODS + ".SYSTEM_BATTERY_STATE";
    private static final String TAG = "LibrePodsSystemBattery";
    private static final String[] LEVELS = {"left_battery", "right_battery", "case_battery"};
    private static final String[] CHARGING = {"left_charging", "right_charging", "case_charging"};
    private static BroadcastReceiver receiver;
    private static Context receiverContext;
    private static String previousMetadata;
    private static String previousLog;

    private SystemBatterySync() { }

    /** Called independently of the native L2CAP hook, which is optional on Android 17. */
    public static void install(XposedModule module, PackageLoadedParam param) {
        if (Build.VERSION.SDK_INT < 34 || !BLUETOOTH.equals(param.getPackageName())) return;
        try {
            Class<?> service = Class.forName("com.android.bluetooth.btservice.AdapterService",
                    false, param.getDefaultClassLoader());
            Method onCreate = service.getDeclaredMethod("onCreate");
            module.hook(onCreate).intercept(chain -> {
                Object result = chain.proceed();
                try {
                    Context context = (Context) chain.getThisObject();
                    Field field = service.getDeclaredField("mRemoteDevices");
                    field.setAccessible(true);
                    Object remote = field.get(context);
                    Method update = remote.getClass().getDeclaredMethod("updateBatteryLevel",
                            BluetoothDevice.class, int.class, boolean.class);
                    update.setAccessible(true);
                    register(module, context, remote, update);
                    module.log(Log.INFO, TAG, "System battery receiver ready");
                } catch (Throwable error) {
                    module.log(Log.WARN, TAG, "System battery receiver unavailable", error);
                }
                return result;
            });
        } catch (Throwable error) {
            // A ROM with a different Bluetooth implementation must not break Bluetooth startup.
            module.log(Log.WARN, TAG, "System battery hook unavailable on this ROM", error);
        }
    }

    private static void register(XposedModule module, Context context, Object remote, Method update) {
        if (receiver != null && receiverContext != null) {
            try { receiverContext.unregisterReceiver(receiver); } catch (RuntimeException ignored) { }
        }
        previousMetadata = null;
        previousLog = null;
        receiver = new BroadcastReceiver() {
            @Override public void onReceive(Context ignored, Intent intent) {
                if (intent == null || !ACTION.equals(intent.getAction())) return;
                try {
                    int uid = context.getPackageManager().getPackageUid(PODS, 0);
                    if (getSentFromUid() != uid) return;
                    if (!intent.getBooleanExtra("connected", false)) {
                        previousMetadata = null;
                        previousLog = null;
                        return;
                    }
                    String address = intent.getStringExtra("address");
                    if (!BluetoothAdapter.checkBluetoothAddress(address)) return;
                    int left = intent.getIntExtra(LEVELS[0], -1);
                    int right = intent.getIntExtra(LEVELS[1], -1);
                    int level = SystemBatteryPolicy.minimum(left, right);
                    if (level < 0) return;
                    BluetoothManager manager = context.getSystemService(BluetoothManager.class);
                    BluetoothDevice device = manager.getAdapter().getRemoteDevice(address);
                    if (device.getBondState() != BluetoothDevice.BOND_BONDED) return;
                    // Normal HFP update invalidates API caches and broadcasts to settings/widgets.
                    // Native BAS readings, if available, retain their standard priority.
                    update.invoke(remote, device, level, false);
                    String logState = address + ":" + left + ":" + right;
                    if (!logState.equals(previousLog)) {
                        module.log(Log.INFO, TAG, "Synced system battery=" + level +
                                "% (left=" + left + ", right=" + right + ")");
                        previousLog = logState;
                    }
                    try {
                        String metadataState = logState + ":" + intent.getIntExtra(LEVELS[2], -1);
                        for (String key : CHARGING)
                            metadataState += ":" + intent.getBooleanExtra(key, false);
                        if (!metadataState.equals(previousMetadata)) {
                            metadata(device, 17, "Untethered Headset");
                            metadata(device, 18, String.valueOf(level));
                            for (int i = 0; i < LEVELS.length; i++) {
                                int value = intent.getIntExtra(LEVELS[i], -1);
                                if (SystemBatteryPolicy.valid(value)) {
                                    metadata(device, 10 + i, String.valueOf(value));
                                    metadata(device, 13 + i,
                                            String.valueOf(intent.getBooleanExtra(CHARGING[i], false)));
                                }
                            }
                            previousMetadata = metadataState;
                        }
                    } catch (Throwable error) {
                        module.log(Log.DEBUG, TAG, "Optional battery metadata update failed", error);
                    }
                } catch (Throwable error) {
                    module.log(Log.WARN, TAG, "System battery update failed", error);
                }
            }
        };
        receiverContext = context;
        context.registerReceiver(receiver, new IntentFilter(ACTION),
                "android.permission.BLUETOOTH_CONNECT", null, Context.RECEIVER_EXPORTED);
    }

    private static void metadata(BluetoothDevice device, int key, String value) throws Exception {
        Method method = BluetoothDevice.class.getDeclaredMethod("setMetadata", int.class, byte[].class);
        method.setAccessible(true);
        method.invoke(device, key, value.getBytes(StandardCharsets.UTF_8));
    }
}
