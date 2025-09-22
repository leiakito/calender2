package com.stu.calender2;

import android.app.Application;
import android.animation.ValueAnimator;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Process;
import android.util.Log;
import android.util.LruCache;
import android.util.SparseIntArray;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.room.Room;

import com.stu.calender2.data.AppDatabase;
import com.stu.calender2.service.KeepAliveService;
import com.stu.calender2.utils.EInkDisplayHelper;
import com.stu.calender2.utils.PreferenceManager;

import java.lang.reflect.Field;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class MyApplication extends Application {
    
    private static final String TAG = "MyApplication";
    private static MyApplication instance;
    private AppDatabase database;
    private PreferenceManager preferenceManager;
    private ExecutorService executorService;
    private Handler mainHandler;
    
    // 使用LruCache代替SparseIntArray，提供更好的性能和内存控制
    private LruCache<Integer, Integer> colorCache;
    private boolean resourcesInitialized = false;
    
    // 添加线程优先级管理
    private static final int THREAD_PRIORITY = Process.THREAD_PRIORITY_BACKGROUND + 2; // 略高于后台，但低于前台

    @Override
    public void onCreate() {
        long startTime = System.currentTimeMillis();
        super.onCreate();
        Log.d(TAG, "应用初始化开始");
        instance = this;
        mainHandler = new Handler(Looper.getMainLooper());
        
        // 初始化颜色缓存
        int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);
        int cacheSize = maxMemory / 16; // 使用最大内存的1/16作为缓存大小
        colorCache = new LruCache<Integer, Integer>(cacheSize);
        
        // 创建优化的线程池，使用优先级线程工厂
        int corePoolSize = Math.max(2, Runtime.getRuntime().availableProcessors() / 2);
        int maxPoolSize = Runtime.getRuntime().availableProcessors();
        executorService = new ThreadPoolExecutor(
            corePoolSize,            // 核心线程数
            maxPoolSize,             // 最大线程数
            60L,                     // 空闲线程保持时间
            TimeUnit.SECONDS,        // 时间单位
            new LinkedBlockingQueue<>(128), // 有界队列
            new PriorityThreadFactory(THREAD_PRIORITY), // 自定义线程工厂
            new ThreadPoolExecutor.DiscardOldestPolicy() // 拒绝策略
        );
        
        // 首先初始化必要组件
        preferenceManager = new PreferenceManager(this);
        
        // 设置应用主题（可能根据用户偏好设置）
        int themeMode = preferenceManager.getThemeMode();
        AppCompatDelegate.setDefaultNightMode(themeMode);
        
        // 延迟初始化数据库和其他组件
        executorService.execute(() -> {
            initializeDatabase();
            Log.d(TAG, "数据库初始化完成");
            
            // 预缓存常用资源
            mainHandler.post(this::initResources);
        });
        
        // 禁用所有动画
        disableAnimations();
        
        // 确保服务运行 - 稍微延迟以优先完成UI初始化
        mainHandler.postDelayed(this::ensureServiceRunning, 2000);
        
        Log.d(TAG, "应用初始化耗时: " + (System.currentTimeMillis() - startTime) + "ms");
    }
    
    /**
     * 优先级线程工厂，用于创建优先级较低的线程
     */
    private static class PriorityThreadFactory implements ThreadFactory {
        private final int threadPriority;
        private final AtomicInteger threadNumber = new AtomicInteger(1);
        private final String namePrefix;
        
        PriorityThreadFactory(int threadPriority) {
            this.threadPriority = threadPriority;
            this.namePrefix = "AppThread-";
        }
        
        @Override
        public Thread newThread(Runnable r) {
            Thread thread = new Thread(r, namePrefix + threadNumber.getAndIncrement()) {
                @Override
                public void run() {
                    Process.setThreadPriority(threadPriority);
                    super.run();
                }
            };
            
            // 将线程设置为后台线程，这样它就不会阻止应用程序退出
            if (!thread.isDaemon()) {
                thread.setDaemon(true);
            }
            return thread;
        }
    }
    
    /**
     * 禁用动画
     */
    private void disableAnimations() {
        try {
            // 使用反射方式设置动画时长缩放
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                try {
                    // 使用反射访问方法
                    Field field = ValueAnimator.class.getDeclaredField("sDurationScale");
                    field.setAccessible(true);
                    field.setFloat(null, 0.0f);
                } catch (Exception e) {
                    Log.e(TAG, "Failed to set animation duration scale", e);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    /**
     * 确保保活服务运行
     */
    private void ensureServiceRunning() {
        if (!isServiceRunning(KeepAliveService.class)) {
            Intent serviceIntent = new Intent(this, KeepAliveService.class);
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(serviceIntent);
                } else {
                    startService(serviceIntent);
                }
                Log.d(TAG, "KeepAliveService started from Application");
            } catch (Exception e) {
                Log.e(TAG, "启动服务失败", e);
                // 如果失败，尝试延迟重试
                mainHandler.postDelayed(this::ensureServiceRunning, 5000);
            }
        }
    }
    
    /**
     * 检查服务是否正在运行
     */
    private boolean isServiceRunning(Class<?> serviceClass) {
        try {
            ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
            if (manager != null) {
                for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
                    if (serviceClass.getName().equals(service.service.getClassName())) {
                        return true;
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "检查服务状态出错", e);
        }
        return false;
    }
    
    private synchronized void initializeDatabase() {
        if (database == null) {
            try {
                long startTime = System.currentTimeMillis();
                database = AppDatabase.getInstance(getApplicationContext());
                Log.d(TAG, "数据库初始化耗时: " + (System.currentTimeMillis() - startTime) + "ms");
            } catch (Exception e) {
                Log.e(TAG, "数据库初始化失败", e);
            }
        }
    }
    
    private void initResources() {
        if (resourcesInitialized) return;
        
        try {
            long startTime = System.currentTimeMillis();
            Resources res = getResources();
            
            // 缓存常用颜色
            int[] commonColors = new int[] {
                R.color.white,
                R.color.black,
                R.color.colorPrimary,
                R.color.colorAccent,
                // 添加其他常用颜色资源ID
            };
            
            // 批量加载颜色以减少获取调用
            for (int colorId : commonColors) {
                colorCache.put(colorId, res.getColor(colorId, null));
            }
            
            // 预热资源管理器
            res.getDisplayMetrics();
            res.getConfiguration();
            
            resourcesInitialized = true;
            Log.d(TAG, "资源预缓存完成，耗时: " + (System.currentTimeMillis() - startTime) + "ms");
        } catch (Exception e) {
            Log.e(TAG, "资源初始化失败", e);
        }
    }
    
    // 获取缓存的颜色资源
    public int getCachedColor(int colorResId) {
        Integer cachedColor = colorCache.get(colorResId);
        if (cachedColor != null) {
            return cachedColor;
        }
        
        // 如果未缓存，则获取并缓存
        try {
            int color = getResources().getColor(colorResId, null);
            colorCache.put(colorResId, color);
            return color;
        } catch (Exception e) {
            Log.e(TAG, "获取颜色资源失败: " + colorResId, e);
            return 0;
        }
    }

    public static MyApplication getInstance() {
        return instance;
    }

    public AppDatabase getDatabase() {
        // 确保数据库已初始化
        if (database == null) {
            initializeDatabase();
        }
        return database;
    }

    public PreferenceManager getPreferenceManager() {
        return preferenceManager;
    }
    
    public void executeAsync(Runnable task) {
        if (executorService.isShutdown()) {
            Log.w(TAG, "线程池已关闭，无法执行任务");
            return;
        }
        
        executorService.execute(task);
    }
    
    public void postToMainThread(Runnable task) {
        mainHandler.post(task);
    }
    
    public void postToMainThreadDelayed(Runnable task, long delayMillis) {
        mainHandler.postDelayed(task, delayMillis);
    }
    
    public void cancelDelayedTasks(Runnable task) {
        if (task != null) {
            mainHandler.removeCallbacks(task);
        }
    }
    
    /**
     * 从主线程消息队列中移除待处理的任务
     * @param task 要移除的任务
     */
    public void removeMainThreadCallback(Runnable task) {
        if (mainHandler != null && task != null) {
            mainHandler.removeCallbacks(task);
        }
    }
    
    @Override
    public void onLowMemory() {
        super.onLowMemory();
        // 清除非必要缓存
        colorCache.evictAll();
        resourcesInitialized = false;
        Log.d(TAG, "内存不足，清除资源缓存");
        
        // 清理EInkDisplayHelper缓存
        EInkDisplayHelper.clearCache();
        
        // 通知Java GC
        System.gc();
    }
    
    @Override
    public void onTrimMemory(int level) {
        super.onTrimMemory(level);
        // 在内存压力大时释放一些资源
        if (level >= TRIM_MEMORY_MODERATE) {
            colorCache.evictAll();
            resourcesInitialized = false;
            // 清理EInkDisplayHelper缓存
            EInkDisplayHelper.clearCache();
            Log.d(TAG, "内存压力大，清除资源缓存，级别: " + level);
        } else if (level >= TRIM_MEMORY_BACKGROUND) {
            // 只清理一半缓存
            colorCache.trimToSize(colorCache.size() / 2);
            Log.d(TAG, "内存压力中等，清除一半资源缓存，级别: " + level);
        }
    }
    
    @Override
    public void onTerminate() {
        super.onTerminate();
        Log.d(TAG, "应用终止，关闭线程池");
        
        // 关闭线程池
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
            try {
                // 等待任务完成
                if (!executorService.awaitTermination(500, TimeUnit.MILLISECONDS)) {
                    executorService.shutdownNow();
                }
            } catch (InterruptedException e) {
                executorService.shutdownNow();
            }
        }
        
        // 清空所有缓存
        if (colorCache != null) {
            colorCache.evictAll();
        }
        
        // 断开database引用
        database = null;
    }
}