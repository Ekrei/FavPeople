package com.example.comw.ui.util

import android.os.Build
import android.text.Html

fun String.cleanHtmlForUi(): String {
    val decoded = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        Html.fromHtml(this, Html.FROM_HTML_MODE_LEGACY).toString()
    } else {
        @Suppress("DEPRECATION")
        Html.fromHtml(this).toString()
    }

    return decoded
        .replace(Regex("[ \\t]+"), " ")
        .replace(Regex("\\n{3,}"), "\n\n")
        .trim()
}