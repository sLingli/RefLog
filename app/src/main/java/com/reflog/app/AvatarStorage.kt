package com.reflog.app

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.File

/**
 * 头像本地存储工具
 *
 * 将用户选择的头像复制到 App 内部存储，
 * 避免依赖 content:// URI（Photo Picker 的 URI 是临时的，系统会回收）。
 */
object AvatarStorage {

    private const val AVATAR_FILENAME = "avatar.jpg"
    private const val JPEG_QUALITY = 85

    /**
     * 将源 URI 的图片保存到本地存储
     * @return 本地文件的绝对路径，失败返回 null
     */
    fun saveAvatar(context: Context, sourceUri: Uri): String? {
        return try {
            val bitmap = context.contentResolver.openInputStream(sourceUri)?.use { stream ->
                BitmapFactory.decodeStream(stream)
            } ?: return null

            val file = getAvatarFile(context)
            file.outputStream().use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)
            }
            bitmap.recycle()
            file.absolutePath
        } catch (_: Exception) {
            null
        }
    }

    /**
     * 获取头像文件，不存在则返回 null
     */
    fun getAvatarFile(context: Context): File {
        return File(context.filesDir, AVATAR_FILENAME)
    }

    /**
     * 检查头像文件是否存在
     */
    fun hasAvatar(context: Context): Boolean {
        return getAvatarFile(context).exists()
    }

    /**
     * 删除头像文件
     */
    fun deleteAvatar(context: Context) {
        getAvatarFile(context).delete()
    }
}
