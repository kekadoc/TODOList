package com.kekadoc.test.todolist.repository

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import java.io.File
import java.io.FileOutputStream

/**
 * Manager for images
 */
class ImageStorage private constructor(context: Context, name: String = IMAGE_DIRECTORY) {

    companion object {
        private const val TAG: String = "ImageStorage-TAG"
        @Volatile private var instance: ImageStorage? = null
        fun getInstance(context: Context, name: String = IMAGE_DIRECTORY): ImageStorage {
            return instance ?: synchronized(this) {
                instance ?: ImageStorage(context).also { instance = it }
            }
        }
        private const val IMAGE_DIRECTORY = "Images"
    }

    private val directory = context.getDir(name, Context.MODE_PRIVATE)

    private fun getNameWithImageExtension(name: String): String {
        return "$name.jpg"
    }

    /**
     * Get image from storage
     *
     * @param name Image name without extension
     *
     * @return File of image
     */
    fun getImage(name: String): File {
        return File(directory, getNameWithImageExtension(name))
    }

    /**
     *
     * Save image in storage
     *
     * @param bitmap Image
     * @param name Image name without extension
     * @param override Override image if already exist
     *
     * @return true if added successfully
     */
    fun addImage(bitmap: Bitmap, name: String, override: Boolean = false): Boolean {
        val file = getImage(name)
        val result = runCatching {
            if (!override && file.exists()) throw FileAlreadyExistsException(file)
            val fos = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos)
            fos.flush()
            fos.close()
        }
        return result.isSuccess
    }
    /**
     *
     * Remove image from storage
     *
     * @param name Image name without extension
     *
     * @return true if removed successfully
     */
    fun removeImage(name: String?): Boolean {
        if (name == null) return false
        val file = File(directory, "$name.jpg")
        if (!file.exists()) return false
        return file.delete()
    }

    /**
     *
     * Rename image in storage
     *
     * @param old Current image name
     * @param new New image name
     *
     * @return true if renamed successfully
     */
    fun renameImage(old: String, new: String): Boolean {
        try {
            val oldFile = getImage(old)
            if (!oldFile.exists()) return false
            val newFile = File("${directory.path}/${getNameWithImageExtension(new)}")
            return oldFile.renameTo(newFile)
        }catch (e: Throwable) {
            Log.e(TAG, "rename: $e")
            return false
        }
    }

}