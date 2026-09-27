package com.example.guardian.child.monitoring

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import com.example.guardian.core.model.InstalledApp
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/** Enumerates the child device's launchable apps so the parent can limit/block any of them. */
@Singleton
class InstalledAppsCollector @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val packageManager: PackageManager = context.packageManager

    fun collect(): List<InstalledApp> {
        val launcherIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val resolved = runCatching {
            packageManager.queryIntentActivities(launcherIntent, 0)
        }.getOrDefault(emptyList())

        return resolved
            .mapNotNull { info ->
                val pkg = info.activityInfo?.packageName ?: return@mapNotNull null
                if (pkg == context.packageName) return@mapNotNull null
                InstalledApp(pkg = pkg, label = info.loadLabel(packageManager).toString())
            }
            .distinctBy { it.pkg }
            .sortedBy { it.label.lowercase() }
    }
}
