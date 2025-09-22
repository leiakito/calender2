package com.stu.calender2.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.appcompat.app.AppCompatDelegate;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * 应用偏好设置管理类
 * 优化版本：实现内存缓存和异步存储
 */
public class PreferenceManager {
    private static final String TAG = "PreferenceManager";
    
    // 共享偏好设置名称
    private static final String PREF_NAME = "app_preferences";
    
    // 设置键名
    private static final String KEY_THEME_MODE = "theme_mode";
    private static final String KEY_NOTIFICATION_ENABLED = "notification_enabled";
    private static final String KEY_REFRESH_RATE = "refresh_rate";
    private static final String KEY_ANIMATION_ENABLED = "animation_enabled";
    private static final String KEY_FIRST_RUN = "first_run";
    private static final String KEY_TASK_AUTO_SORT = "task_auto_sort";
    
    // 默认值
    private static final int DEFAULT_THEME_MODE = AppCompatDelegate.MODE_NIGHT_NO;
    private static final boolean DEFAULT_NOTIFICATION_ENABLED = true;
    private static final int DEFAULT_REFRESH_RATE = 3; // 中等刷新率
    private static final boolean DEFAULT_ANIMATION_ENABLED = false;
    private static final boolean DEFAULT_FIRST_RUN = true;
    private static final boolean DEFAULT_TASK_AUTO_SORT = true;
    
    // 共享偏好设置实例
    private final SharedPreferences preferences;
    
    // 内存缓存
    private final Map<String, Object> cache = new HashMap<>();
    
    // 异步写入线程池
    private final Executor executor;
    
    public PreferenceManager(Context context) {
        preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        executor = Executors.newSingleThreadExecutor();
        
        // 预加载常用设置到内存缓存
        loadPreferencesToCache();
    }
    
    /**
     * 将常用偏好设置加载到内存缓存
     */
    private void loadPreferencesToCache() {
        cache.put(KEY_THEME_MODE, preferences.getInt(KEY_THEME_MODE, DEFAULT_THEME_MODE));
        cache.put(KEY_NOTIFICATION_ENABLED, preferences.getBoolean(KEY_NOTIFICATION_ENABLED, DEFAULT_NOTIFICATION_ENABLED));
        cache.put(KEY_REFRESH_RATE, preferences.getInt(KEY_REFRESH_RATE, DEFAULT_REFRESH_RATE));
        cache.put(KEY_ANIMATION_ENABLED, preferences.getBoolean(KEY_ANIMATION_ENABLED, DEFAULT_ANIMATION_ENABLED));
        cache.put(KEY_FIRST_RUN, preferences.getBoolean(KEY_FIRST_RUN, DEFAULT_FIRST_RUN));
        cache.put(KEY_TASK_AUTO_SORT, preferences.getBoolean(KEY_TASK_AUTO_SORT, DEFAULT_TASK_AUTO_SORT));
        
        Log.d(TAG, "设置已加载到内存缓存");
    }
    
    /**
     * 获取主题模式
     */
    public int getThemeMode() {
        return getIntFromCache(KEY_THEME_MODE, DEFAULT_THEME_MODE);
    }
    
    /**
     * 设置主题模式
     */
    public void setThemeMode(int themeMode) {
        putIntToCache(KEY_THEME_MODE, themeMode);
    }
    
    /**
     * 是否启用通知
     */
    public boolean isNotificationEnabled() {
        return getBooleanFromCache(KEY_NOTIFICATION_ENABLED, DEFAULT_NOTIFICATION_ENABLED);
    }
    
    /**
     * 设置通知状态
     */
    public void setNotificationEnabled(boolean enabled) {
        putBooleanToCache(KEY_NOTIFICATION_ENABLED, enabled);
    }
    
    /**
     * 获取屏幕刷新率
     */
    public int getRefreshRate() {
        return getIntFromCache(KEY_REFRESH_RATE, DEFAULT_REFRESH_RATE);
    }
    
    /**
     * 设置屏幕刷新率
     */
    public void setRefreshRate(int rate) {
        putIntToCache(KEY_REFRESH_RATE, rate);
    }
    
    /**
     * 是否启用动画
     */
    public boolean isAnimationEnabled() {
        return getBooleanFromCache(KEY_ANIMATION_ENABLED, DEFAULT_ANIMATION_ENABLED);
    }
    
    /**
     * 设置动画状态
     */
    public void setAnimationEnabled(boolean enabled) {
        putBooleanToCache(KEY_ANIMATION_ENABLED, enabled);
    }
    
    /**
     * 是否首次运行
     */
    public boolean isFirstRun() {
        return getBooleanFromCache(KEY_FIRST_RUN, DEFAULT_FIRST_RUN);
    }
    
    /**
     * 设置首次运行状态
     */
    public void setFirstRun(boolean firstRun) {
        putBooleanToCache(KEY_FIRST_RUN, firstRun);
    }
    
    /**
     * 是否自动排序任务
     */
    public boolean isTaskAutoSortEnabled() {
        return getBooleanFromCache(KEY_TASK_AUTO_SORT, DEFAULT_TASK_AUTO_SORT);
    }
    
    /**
     * 设置任务自动排序状态
     */
    public void setTaskAutoSortEnabled(boolean enabled) {
        putBooleanToCache(KEY_TASK_AUTO_SORT, enabled);
    }
    
    /**
     * 从缓存中获取整数值
     */
    private int getIntFromCache(String key, int defaultValue) {
        if (cache.containsKey(key)) {
            return (int) cache.get(key);
        }
        
        // 如果缓存中不存在，则从SharedPreferences读取并加入缓存
        int value = preferences.getInt(key, defaultValue);
        cache.put(key, value);
        return value;
    }
    
    /**
     * 将整数值放入缓存并异步写入SharedPreferences
     */
    private void putIntToCache(String key, int value) {
        // 更新缓存
        cache.put(key, value);
        
        // 异步写入存储
        executor.execute(() -> {
            preferences.edit().putInt(key, value).apply();
            Log.d(TAG, "异步保存整数设置: " + key + " = " + value);
        });
    }
    
    /**
     * 从缓存中获取布尔值
     */
    private boolean getBooleanFromCache(String key, boolean defaultValue) {
        if (cache.containsKey(key)) {
            return (boolean) cache.get(key);
        }
        
        // 如果缓存中不存在，则从SharedPreferences读取并加入缓存
        boolean value = preferences.getBoolean(key, defaultValue);
        cache.put(key, value);
        return value;
    }
    
    /**
     * 将布尔值放入缓存并异步写入SharedPreferences
     */
    private void putBooleanToCache(String key, boolean value) {
        // 更新缓存
        cache.put(key, value);
        
        // 异步写入存储
        executor.execute(() -> {
            preferences.edit().putBoolean(key, value).apply();
            Log.d(TAG, "异步保存布尔设置: " + key + " = " + value);
        });
    }
    
    /**
     * 从缓存中获取字符串值
     */
    private String getStringFromCache(String key, String defaultValue) {
        if (cache.containsKey(key)) {
            return (String) cache.get(key);
        }
        
        // 如果缓存中不存在，则从SharedPreferences读取并加入缓存
        String value = preferences.getString(key, defaultValue);
        cache.put(key, value);
        return value;
    }
    
    /**
     * 将字符串值放入缓存并异步写入SharedPreferences
     */
    private void putStringToCache(String key, String value) {
        // 更新缓存
        cache.put(key, value);
        
        // 异步写入存储
        executor.execute(() -> {
            preferences.edit().putString(key, value).apply();
            Log.d(TAG, "异步保存字符串设置: " + key + " = " + value);
        });
    }
    
    /**
     * 清除所有设置并恢复默认值
     */
    public void resetToDefaults() {
        // 清除缓存
        cache.clear();
        
        // 重新加载默认值到缓存
        cache.put(KEY_THEME_MODE, DEFAULT_THEME_MODE);
        cache.put(KEY_NOTIFICATION_ENABLED, DEFAULT_NOTIFICATION_ENABLED);
        cache.put(KEY_REFRESH_RATE, DEFAULT_REFRESH_RATE);
        cache.put(KEY_ANIMATION_ENABLED, DEFAULT_ANIMATION_ENABLED);
        cache.put(KEY_FIRST_RUN, DEFAULT_FIRST_RUN);
        cache.put(KEY_TASK_AUTO_SORT, DEFAULT_TASK_AUTO_SORT);
        
        // 异步清除存储
        executor.execute(() -> {
            preferences.edit().clear().apply();
            Log.d(TAG, "设置已重置为默认值");
        });
    }
} 