package com.kekadoc.test.todolist

import android.content.Context
import android.content.res.Resources
import android.os.Build
import android.util.TypedValue
import androidx.annotation.Dimension
import androidx.core.os.ConfigurationCompat
import java.util.*

fun Context.dpToPx(@Dimension(unit = Dimension.DP) dp: Float): Float {
    return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, resources.displayMetrics)
}

fun getLocale(context: Context): Locale {
    return ConfigurationCompat.getLocales(context.resources.configuration).get(0)
}
