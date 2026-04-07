package com.example.stylish_android_application

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent

object BrandHelper {

    private const val COMPOSITE_SEPARATOR = "||"

    /**
     * Returns true if the input is a URL or a composite "name||url" value.
     * Composite format: "Nike||https://nike.com/..."
     */
    fun isUrl(input: String): Boolean =
        input.startsWith("http://") || input.startsWith("https://") || isComposite(input)

    private fun isComposite(input: String): Boolean {
        if (!input.contains(COMPOSITE_SEPARATOR)) return false
        val url = input.substringAfter(COMPOSITE_SEPARATOR).trim()
        return url.startsWith("http://") || url.startsWith("https://")
    }

    /**
     * Extracts the human-readable brand name.
     * - Composite "Nike||https://..." → "Nike"
     * - Plain URL "https://shop.mango.com/..." → "Mango"
     */
    fun extractBrandName(input: String): String {
        if (isComposite(input)) return input.substringBefore(COMPOSITE_SEPARATOR).trim()
        return try {
            val host = Uri.parse(input).host ?: return input
            val parts = host.split(".")
            val name = if (parts.size >= 2) parts[parts.size - 2] else parts.first()
            name.replaceFirstChar { it.uppercaseChar() }
        } catch (e: Exception) {
            input
        }
    }

    /**
     * Returns the URL part from either a plain URL or a composite "name||url" value.
     */
    fun getUrl(input: String): String =
        if (isComposite(input)) input.substringAfter(COMPOSITE_SEPARATOR).trim() else input

    /**
     * Builds a composite value when a custom name differs from the auto-extracted one.
     * If name matches auto-extracted (or is empty), returns just the URL.
     */
    fun buildValue(name: String, url: String): String {
        val autoName = extractBrandName(url)
        return if (name.isNotEmpty() && name != autoName) "$name$COMPOSITE_SEPARATOR$url" else url
    }

    /**
     * Opens [input] using Chrome Custom Tabs (supports both plain URL and composite format).
     * Falls back to ACTION_VIEW if Custom Tabs are unavailable.
     */
    fun openUrl(context: Context, input: String) {
        val url = getUrl(input)
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
