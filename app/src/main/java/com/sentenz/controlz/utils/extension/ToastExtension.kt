package com.sentenz.controlz.utils.extension

import android.content.Context
import android.widget.Toast
import androidx.annotation.StringRes

/**
 * Extension method to show toast for Context.
 *
 * From: https://gist.github.com/BapNesS/edcd2a3dc999b9305b655a147876e3dd
 */
fun Context?.toast(@StringRes textId: Int, duration: Int = Toast.LENGTH_SHORT) = this?.let { Toast.makeText(it, textId, duration).show() }