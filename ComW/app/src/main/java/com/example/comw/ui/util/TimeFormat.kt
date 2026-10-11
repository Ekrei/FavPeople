package com.example.comw.ui.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun Long.formatDateTime(): String =
    SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(Date(this))
