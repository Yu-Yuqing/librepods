package me.kavishdevar.librepods.utils

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Process
import android.provider.Settings

object AssistantPermissions {
    enum class Grant { ALLOWED, REQUIRED, UNKNOWN }
    val isXiaomi: Boolean
        get() = Build.MANUFACTURER.equals("Xiaomi", true) ||
            Build.BRAND.equals("Redmi", true) || Build.BRAND.equals("POCO", true)

    // Vendor permissions are not Android runtime permissions and have no requestPermissions UI.
    fun check(context: Context, operation: Int): Grant = runCatching {
        val manager = context.getSystemService(AppOpsManager::class.java)
        val method = AppOpsManager::class.java.getMethod(
            "checkOpNoThrow", Int::class.javaPrimitiveType,
            Int::class.javaPrimitiveType, String::class.java,
        )
        val mode = method.invoke(manager, operation, Process.myUid(), context.packageName) as Int
        if (mode == AppOpsManager.MODE_ALLOWED) Grant.ALLOWED else Grant.REQUIRED
    }.getOrDefault(Grant.UNKNOWN)

    fun openSettings(context: Context, overlay: Boolean = false): Boolean {
        val details = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.parse("package:${context.packageName}"))
        val candidates = if (overlay) listOf(
            Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:${context.packageName}")), details,
        ) else listOf(
            Intent("miui.intent.action.APP_PERM_EDITOR").apply {
                setPackage("com.miui.securitycenter")
                putExtra("extra_pkgname", context.packageName)
                putExtra("extra_package_uid", Process.myUid())
            }, details,
        )
        for (intent in candidates) {
            if (runCatching {
                context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }.isSuccess) return true
        }
        return false
    }
}
