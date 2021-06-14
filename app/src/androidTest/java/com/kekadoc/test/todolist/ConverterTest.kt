package com.kekadoc.test.todolist

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.util.Log
import androidx.core.graphics.drawable.toBitmap
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kekadoc.test.todolist.repository.Converter
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ConverterTest {
    companion object {
        private const val TAG: String = "ConverterTest-TAG"
    }
    @Test
    fun bitmapToString() {
        val initBitmap: Bitmap = ColorDrawable(Color.RED).toBitmap(20, 20)
        val stringFromInitBitmap: String? = Converter.bitMapToString(initBitmap)
        assert(stringFromInitBitmap != null)

        val bimapFromString: Bitmap? = Converter.stringToBitMap(stringFromInitBitmap)
        assert(bimapFromString != null)

        val stringFromBitmap: String? = Converter.bitMapToString(bimapFromString)
        assert(stringFromBitmap != null)
        assert(stringFromInitBitmap == stringFromBitmap)
    }
}