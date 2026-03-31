package com.example.stylish_android_application

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent

object BrandHelper {

    /** Returns true if the input is a http/https URL. */
    fun isUrl(input: String): Boolean =
        input.startsWith("http://") || input.startsWith("https://")

    /**
     * Extracts a human-readable brand name from a URL.
     * e.g. "https://shop.mango.com/il/shirt" → "Mango"
     *      "https://www.zara.com/..."         → "Zara"
     */
    fun extractBrandName(url: String): String {
        return try {
            val host = Uri.parse(url).host ?: return url
            val parts = host.split(".")
            // Second-to-last segment is the domain name (before the TLD)
            val name = if (parts.size >= 2) parts[parts.size - 2] else parts.first()
            name.replaceFirstChar { it.uppercaseChar() }
        } catch (e: Exception) {
            url
        }
    }

    /**
     * Opens [url] using Chrome Custom Tabs.
     * Falls back to ACTION_VIEW if Custom Tabs are unavailable.
     */
    fun openUrl(context: Context, url: String) {
        val uri = Uri.parse(url)
        try {
            CustomTabsIntent.Builder()
                .setShowTitle(true)
                .build()
                .launchUrl(context, uri)
        } catch (e: ActivityNotFoundException) {
            try {
                context.startActivity(Intent(Intent.ACTION_VIEW, uri))
            } catch (ignored: ActivityNotFoundException) { }
        }
    }
}
