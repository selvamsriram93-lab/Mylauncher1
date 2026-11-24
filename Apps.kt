package com.example.mylauncher

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import android.provider.Settings
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap

data class AppEntry(
    val label: String,
    val pkg: String,
    val cls: String,
    val icon: ImageBitmap
)

data class DockItem(
    val label: String,
    val intent: Intent,
    val icon: ImageBitmap
)

fun loadApps(ctx: Context): List<AppEntry> {
    val pm = ctx.packageManager
    val query = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
    return pm.queryIntentActivities(query, 0)
        .filter { it.activityInfo.packageName != ctx.packageName }
        .map {
            AppEntry(
                label = it.loadLabel(pm).toString(),
                pkg = it.activityInfo.packageName,
                cls = it.activityInfo.name,
                icon = it.loadIcon(pm).toBitmap(128, 128).asImageBitmap()
            )
        }
        .sortedBy { it.label.lowercase() }
}

fun loadDock(ctx: Context): List<DockItem> {
    val pm = ctx.packageManager
    val probes = listOf(
        Intent(Intent.ACTION_DIAL),
        Intent(Intent.ACTION_VIEW, Uri.parse("sms:")),
        Intent(Intent.ACTION_VIEW, Uri.parse("https://www.example.com")),
        Intent(MediaStore.ACTION_IMAGE_CAPTURE)
    )
    return probes.mapNotNull { probe ->
        val ri = pm.resolveActivity(probe, 0) ?: return@mapNotNull null
        if (ri.activityInfo.packageName == "android") return@mapNotNull null
        val launch = Intent(probe)
            .setComponent(ComponentName(ri.activityInfo.packageName, ri.activityInfo.name))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        DockItem(
            label = ri.loadLabel(pm).toString(),
            intent = launch,
            icon = ri.loadIcon(pm).toBitmap(128, 128).asImageBitmap()
        )
    }
}

fun launchApp(ctx: Context, app: AppEntry) {
    val intent = Intent(Intent.ACTION_MAIN)
        .addCategory(Intent.CATEGORY_LAUNCHER)
        .setComponent(ComponentName(app.pkg, app.cls))
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
    runCatching { ctx.startActivity(intent) }
}

fun openAppInfo(ctx: Context, app: AppEntry) {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${app.pkg}"))
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    runCatching { ctx.startActivity(intent) }
}
