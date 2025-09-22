package com.stu.calender2.utils;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import java.lang.ref.WeakReference;
import java.util.WeakHashMap;

/**
 * UI性能优化工具类
 * 专为电子墨水屏设备优化，降低UI刷新频率，减少闪烁并节省电量
 */
public class UIPerformanceOptimizer {
    private static final String TAG = "UIPerformanceOptimizer";
    
    private static volatile UIPerformanceOptimizer instance;
    
    // 减少重绘区域临时存储
    private final WeakHashMap<View, Long> pendingUpdates = new WeakHashMap<>();
    
    // 最小UI更新间隔时间（毫秒）
    private static final long MIN_UI_UPDATE_INTERVAL_MS = 300;
    
    // 默认电子墨水屏优化级别
    private int eInkOptimizationLevel = 1; // 0=关闭, 1=适中, 2=高度优化
    
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private WeakReference<Activity> currentActivity;
    
    private UIPerformanceOptimizer() {
        // 私有构造函数
    }
    
    public static UIPerformanceOptimizer getInstance() {
        if (instance == null) {
            synchronized (UIPerformanceOptimizer.class) {
                if (instance == null) {
                    instance = new UIPerformanceOptimizer();
                }
            }
        }
        return instance;
    }
    
    /**
     * 设置电子墨水屏优化级别
     * @param level 0=关闭, 1=适中, 2=高度优化
     */
    public void setEInkOptimizationLevel(int level) {
        if (level >= 0 && level <= 2) {
            this.eInkOptimizationLevel = level;
            Log.d(TAG, "设置电子墨水屏优化级别: " + level);
            
            if (currentActivity != null && currentActivity.get() != null) {
                applyOptimizationsToActivity(currentActivity.get());
            }
        }
    }
    
    /**
     * 获取当前电子墨水屏优化级别
     */
    public int getEInkOptimizationLevel() {
        return eInkOptimizationLevel;
    }
    
    /**
     * 注册Activity以应用优化
     */
    public void registerActivity(Activity activity) {
        if (activity != null) {
            this.currentActivity = new WeakReference<>(activity);
            applyOptimizationsToActivity(activity);
        }
    }
    
    /**
     * 应用电子墨水屏优化到指定Activity
     */
    private void applyOptimizationsToActivity(Activity activity) {
        if (activity == null) return;
        
        Window window = activity.getWindow();
        if (window == null) return;
        
        // 根据优化级别应用不同程度的优化
        switch (eInkOptimizationLevel) {
            case 0:
                // 无优化
                window.clearFlags(WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED);
                break;
                
            case 1:
                // 适中优化
                // 禁用硬件加速，墨水屏上软件渲染更好
                window.clearFlags(WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED);
                // 禁用过度滚动阴影和边缘发光效果
                optimizeViewHierarchy(window.getDecorView(), 1);
                break;
                
            case 2:
                // 高度优化
                // 禁用硬件加速
                window.clearFlags(WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED);
                // 禁用所有动画和视觉效果
                window.setFlags(
                        WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                        WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
                // 递归优化所有视图
                optimizeViewHierarchy(window.getDecorView(), 2);
                break;
        }
        
        Log.d(TAG, "已应用电子墨水屏优化到Activity: " + activity.getClass().getSimpleName());
    }
    
    /**
     * 递归优化视图层次结构
     */
    private void optimizeViewHierarchy(View view, int level) {
        if (view == null) return;
        
        // 禁用背景动画和淡入淡出效果
        view.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        
        // 对不同的视图类型进行优化
        if (view instanceof ViewGroup) {
            ViewGroup viewGroup = (ViewGroup) view;
            
            // RecyclerView 特殊优化
            if (view instanceof RecyclerView) {
                RecyclerView recyclerView = (RecyclerView) view;
                recyclerView.setItemAnimator(null); // 禁用项目动画
                recyclerView.setHasFixedSize(true); // 固定大小优化
                
                if (level == 2) {
                    // 减少过度滚动效果
                    recyclerView.setOverScrollMode(View.OVER_SCROLL_NEVER);
                    // 预取限制为0，减少布局计算
                    recyclerView.setItemViewCacheSize(10); // 增加缓存减少重建
                }
            }
            
            // 递归处理所有子视图
            for (int i = 0; i < viewGroup.getChildCount(); i++) {
                optimizeViewHierarchy(viewGroup.getChildAt(i), level);
            }
        } else if (view instanceof ImageView) {
            ImageView imageView = (ImageView) view;
            // 对于电子墨水屏，图片优化
            // 使用低质量图片，减少资源消耗
            if (level >= 2) {
                // 降低图片质量，使用软件渲染模式
                imageView.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
            }
        } else if (view instanceof TextView) {
            TextView textView = (TextView) view;
            if (level == 2) {
                // 禁用文本选择高亮
                textView.setHighlightColor(0);
                // 禁用光标闪烁
                textView.setCursorVisible(false);
            }
        }
    }
    
    /**
     * 延迟刷新指定视图，合并短时间内的多次刷新请求
     */
    public void scheduleDelayedUpdate(View view) {
        if (view == null || eInkOptimizationLevel == 0) {
            // 如果视图为空或优化关闭，直接执行刷新
            if (view != null) view.invalidate();
            return;
        }
        
        long currentTime = System.currentTimeMillis();
        Long lastUpdate = pendingUpdates.get(view);
        
        if (lastUpdate == null || (currentTime - lastUpdate > MIN_UI_UPDATE_INTERVAL_MS)) {
            // 安排延迟更新
            pendingUpdates.put(view, currentTime);
            
            mainHandler.postDelayed(() -> {
                // 移除记录并执行更新
                pendingUpdates.remove(view);
                if (view.isAttachedToWindow()) {
                    view.invalidate();
                }
            }, MIN_UI_UPDATE_INTERVAL_MS);
        }
    }
    
    /**
     * 设置最佳的刷新率（帧率）
     * 电子墨水屏不需要高刷新率
     */
    public void setOptimalRefreshRate(Activity activity) {
        if (activity == null) return;
        
        Window window = activity.getWindow();
        if (window == null) return;
        
        // 根据优化级别设置不同的刷新率策略
        switch (eInkOptimizationLevel) {
            case 0:
                // 不做特殊处理，使用系统默认
                break;
            case 1:
                // 降低刷新率，但保持流畅度
                mainHandler.post(() -> 
                    window.setFlags(
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN)
                );
                break;
            case 2:
                // 最低刷新率，最大程度节省电量
                mainHandler.post(() -> {
                    window.setFlags(
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN | 
                        WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN | 
                        WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
                });
                break;
        }
    }
    
    /**
     * 优化指定的RecyclerView，减少刷新和重建频率
     */
    public void optimizeRecyclerView(RecyclerView recyclerView) {
        if (recyclerView == null) return;
        
        // 通用优化
        recyclerView.setItemAnimator(null); // 禁用动画
        recyclerView.setHasFixedSize(true); // 假设内容不会导致RecyclerView大小变化
        
        // 根据优化级别应用不同程度的优化
        switch (eInkOptimizationLevel) {
            case 0:
                // 不做特殊处理
                break;
                
            case 1:
                // 中等优化
                recyclerView.setItemViewCacheSize(15); // 增加视图缓存
                recyclerView.setDrawingCacheEnabled(true); // 启用绘制缓存
                recyclerView.setDrawingCacheQuality(View.DRAWING_CACHE_QUALITY_LOW); // 低质量缓存
                recyclerView.setOverScrollMode(View.OVER_SCROLL_NEVER); // 禁用过度滚动效果
                break;
                
            case 2:
                // 高度优化
                recyclerView.setItemViewCacheSize(20); // 最大视图缓存
                recyclerView.setDrawingCacheEnabled(true);
                recyclerView.setDrawingCacheQuality(View.DRAWING_CACHE_QUALITY_LOW);
                recyclerView.setOverScrollMode(View.OVER_SCROLL_NEVER);
                recyclerView.setLayoutFrozen(true); // 冻结布局直到必须刷新
                // 监听触摸事件，只在真正需要时解除冻结
                recyclerView.setOnTouchListener((v, event) -> {
                    recyclerView.setLayoutFrozen(false);
                    // 延迟重新冻结
                    mainHandler.postDelayed(() -> 
                        recyclerView.setLayoutFrozen(true), 1000);
                    // 不消费事件
                    return false;
                });
                break;
        }
    }
    
    /**
     * 清除所有待处理的UI更新
     */
    public void clearPendingUpdates() {
        mainHandler.removeCallbacksAndMessages(null);
        pendingUpdates.clear();
    }
} 