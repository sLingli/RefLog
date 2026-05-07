package com.example.myapplication

import android.content.Context
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat

/**
 * 语言选项枚举
 */
enum class AppLanguage {
    DEFAULT,
    CHINESE_SIMPLIFIED,
    ENGLISH
}

/**
 * 语言管理器
 *
 * 负责语言选项的持久化和切换，使用 AppCompatDelegate.setApplicationLocales()
 * 实现应用内语言切换，无需手动重启 Activity。
 */
object LanguageManager {

    private const val PREFS_NAME = "language_prefs"
    private const val KEY_LANGUAGE = "app_language"

    private lateinit var prefs: SharedPreferences

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        // 启动时应用已保存的语言设置
        applyLanguage(context)
    }

    /**
     * 当前语言选项
     */
    var currentLanguage: AppLanguage
        get() {
            val name = prefs.getString(KEY_LANGUAGE, AppLanguage.DEFAULT.name)
            return try {
                AppLanguage.valueOf(name!!)
            } catch (_: Exception) {
                AppLanguage.DEFAULT
            }
        }
        set(value) {
            prefs.edit().putString(KEY_LANGUAGE, value.name).apply()
        }

    /**
     * 应用语言设置
     *
     * 使用 AppCompatDelegate.setApplicationLocales()，会自动处理
     * Configuration 更新和 Activity 重建。
     */
    fun applyLanguage(context: Context) {
        val localeList = when (currentLanguage) {
            AppLanguage.DEFAULT -> LocaleListCompat.getEmptyLocaleList()
            AppLanguage.CHINESE_SIMPLIFIED -> LocaleListCompat.forLanguageTags("zh-CN")
            AppLanguage.ENGLISH -> LocaleListCompat.forLanguageTags("en")
        }
        AppCompatDelegate.setApplicationLocales(localeList)
    }

    /**
     * 获取语言显示名称的字符串资源 ID
     */
    fun getLanguageNameResId(language: AppLanguage): Int = when (language) {
        AppLanguage.DEFAULT -> R.string.language_default
        AppLanguage.CHINESE_SIMPLIFIED -> R.string.language_zh
        AppLanguage.ENGLISH -> R.string.language_en
    }

    /**
     * 获取语言描述的字符串资源 ID
     */
    fun getLanguageDescResId(language: AppLanguage): Int? = when (language) {
        AppLanguage.DEFAULT -> R.string.label_follow_system_desc
        AppLanguage.CHINESE_SIMPLIFIED -> null
        AppLanguage.ENGLISH -> null
    }
}
