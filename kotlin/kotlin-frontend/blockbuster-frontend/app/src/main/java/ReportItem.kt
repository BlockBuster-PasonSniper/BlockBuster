package com.example.hackatonproject


import android.net.Uri

data class ReportItem(
    val imageUri: Uri,
    val address: String,
    val type: String,
    val timestamp: Long = System.currentTimeMillis()
)