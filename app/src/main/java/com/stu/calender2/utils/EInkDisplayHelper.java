package com.stu.calender2.utils;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.os.Build;
import android.provider.Settings;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AbsListView;
import android.widget.EdgeEffect;
import android.widget.ListView;
import android.widget.ScrollView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager.widget.ViewPager;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.WeakHashMap;

/**
 * 水墨屏设备优化助手类
 */
public class EInkDisplayHelper {
    // 使用WeakHashMap缓存已处理的视图，避免重复处理
    private static final WeakHashMap<View, Boolean> processedViews = new WeakHashMap<>();
    
    // 添加锁对象用于同步
    private static final Object lock = new Object();
    
    // 添加刷新状态追踪
    private static long lastGlobalRefreshTime = 0;
    private static final long MIN_REFRESH_INTERVAL = 500; // 毫秒
    
    public static void disableSystemAnimations(Context context) {
        try {
            // 尝试通过开发者选项禁用动画
            Settings.Global.putFloat(context.getContentResolver(),
                    Settings.Global.ANIMATOR_DURATION_SCALE, 0.0f);
            Settings.Global.putFloat(context.getContentResolver(),
                    Settings.Global.TRANSITION_ANIMATION_SCALE, 0.0f);
            Settings.Global.putFloat(context.getContentResolver(),
                    Settings.Global.WINDOW_ANIMATION_SCALE, 0.0f);
        } catch (Exception e) {
            // 忽略权限问题
        }
    }
    /**
     * 应用全局水墨屏优化
     */
    public static void applyEInkOptimizations(Activity activity) {
        if (activity == null) return;
        
        // 禁用所有窗口动画
        activity.getWindow().setWindowAnimations(0);
        
        // 禁用过渡动画
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            activity.getWindow().setAllowEnterTransitionOverlap(false);
            activity.getWindow().setAllowReturnTransitionOverlap(false);
            activity.getWindow().setSharedElementsUseOverlay(false);
        }
        
        // 设置窗口为不透明，避免透明度引起的闪烁
        Window window = activity.getWindow();
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
        
        // 使用软件渲染，提高水墨屏兼容性
        View decorView = window.getDecorView();
        if (decorView != null) {
            decorView.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        }
    }
    
    /**
     * 递归禁用视图层次结构中的所有动画
     */
    public static void disableAnimationsRecursively(View view) {
        if (view == null) return;
        
        synchronized (lock) {
            // 检查此视图是否已处理
            if (processedViews.containsKey(view)) {
                return;
            }
            
            // 记录此视图已处理
            processedViews.put(view, Boolean.TRUE);
        }
        
        // 禁用视图动画
        view.setAnimation(null);
        
        // 不再禁用ScrollView的平滑滚动，仅优化其他属性
        if (view instanceof ScrollView) {
            // 保留平滑滚动，但设置其他优化
            ScrollView scrollView = (ScrollView) view;
            
            try {
                Method method = ScrollView.class.getMethod("setScrollingCacheEnabled", boolean.class);
                method.invoke(scrollView, false);
            } catch (Exception e) {
                // 忽略反射错误
            }
        }
        
        // 特殊处理列表视图
        if (view instanceof ListView) {
            ListView listView = (ListView) view;
            listView.setLayoutAnimation(null);
            listView.setSmoothScrollbarEnabled(false);
        }
        
        // 特殊处理RecyclerView
        if (view instanceof RecyclerView) {
            RecyclerView recyclerView = (RecyclerView) view;
            recyclerView.setItemAnimator(null);
            recyclerView.setLayoutAnimation(null);
        }
        
        // 特殊处理ViewPager
        if (view instanceof ViewPager) {
            ViewPager viewPager = (ViewPager) view;
            viewPager.setPageTransformer(false, null);
        }
        
        // 设置淡化边缘效果但不完全禁用过度滚动
        view.setOverScrollMode(View.OVER_SCROLL_IF_CONTENT_SCROLLS);
        
        // 使用软件渲染提高兼容性
        view.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        
        // 递归处理子视图
        if (view instanceof ViewGroup) {
            ViewGroup viewGroup = (ViewGroup) view;
            for (int i = 0; i < viewGroup.getChildCount(); i++) {
                disableAnimationsRecursively(viewGroup.getChildAt(i));
            }
        }
    }
    
    /**
     * 设置水墨屏优化的刷新模式
     */
    public static void setEInkRefreshMode(Context context) {
        try {
            // 尝试减少默认动画时间
            Field sDefaultAnimationDurationField = Field.class.getDeclaredField("sDefaultAnimationDuration");
            sDefaultAnimationDurationField.setAccessible(true);
            sDefaultAnimationDurationField.setInt(null, 0);
        } catch (Exception e) {
            // 忽略反射错误
        }
        
        try {
            // 尝试调用博阅等设备的特定API
            Class<?> epdControllerClass = Class.forName("android.view.EpdController");
            
            // 设置为最适合UI的刷新模式
            Method setModeMethod = epdControllerClass.getMethod("setMode", int.class);
            setModeMethod.invoke(null, 2); // 2通常代表GC16全刷新模式，减少重影
        } catch (Exception e) {
            // 忽略不支持的设备
            try {
                // 尝试文石设备的API
                Class<?> epdClass = Class.forName("android.hardware.eink.EinkManager");
                Method getInstanceMethod = epdClass.getMethod("getInstance");
                Object einkManager = getInstanceMethod.invoke(null);
                
                // 设置UI刷新模式
                Method setUiModeMethod = einkManager.getClass().getMethod("setUiMode", int.class);
                setUiModeMethod.invoke(einkManager, 2); // 通常2表示UI模式
            } catch (Exception e2) {
                // 忽略不支持的设备
            }
        }
    }

    /**
     * 禁用 ScrollView 的滚动动画
     */
    public static void disableScrollViewAnimations(ScrollView scrollView) {
        if (scrollView == null) return;
        
        // 检查是否已处理
        synchronized (lock) {
            if (processedViews.containsKey(scrollView)) {
                return;
            }
            processedViews.put(scrollView, Boolean.TRUE);
        }
        
        // 禁用平滑滚动
        scrollView.setSmoothScrollingEnabled(false);
        
        try {
            Method method = ScrollView.class.getMethod("setScrollingCacheEnabled", boolean.class);
            method.invoke(scrollView, false);
        } catch (Exception e) {
            // 忽略反射错误
        }
        
        // 禁用过度滚动效果
        scrollView.setOverScrollMode(View.OVER_SCROLL_NEVER);
        
        // 禁用边缘效果
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            scrollView.setEdgeEffectColor(Color.TRANSPARENT);
        }
    }

    /**
     * 禁用 RecyclerView 的所有动画
     */
    public static void disableRecyclerViewAnimations(RecyclerView recyclerView) {
        if (recyclerView == null) return;
        
        // 检查是否已处理
        synchronized (lock) {
            if (processedViews.containsKey(recyclerView)) {
                return;
            }
            processedViews.put(recyclerView, Boolean.TRUE);
        }
        
        recyclerView.setItemAnimator(null);
        if (recyclerView.getItemAnimator() != null) {
            recyclerView.getItemAnimator().setAddDuration(0);
            recyclerView.getItemAnimator().setChangeDuration(0);
            recyclerView.getItemAnimator().setMoveDuration(0);
            recyclerView.getItemAnimator().setRemoveDuration(0);
        }
        
        // 禁用边缘效果
        recyclerView.setEdgeEffectFactory(new RecyclerView.EdgeEffectFactory() {
            @NonNull
            @Override
            protected EdgeEffect createEdgeEffect(@NonNull RecyclerView view, int direction) {
                EdgeEffect edgeEffect = new EdgeEffect(view.getContext());
                edgeEffect.setSize(0, 0); // 最小化效果
                return edgeEffect;
            }
        });
        
        // 减少刷新频率，提高性能
        recyclerView.setItemViewCacheSize(20);
        recyclerView.setDrawingCacheEnabled(true);
        recyclerView.setDrawingCacheQuality(View.DRAWING_CACHE_QUALITY_LOW);
    }

    /**
     * 禁用 ViewPager 的滑动动画
     */
    public static void disableViewPagerAnimations(ViewPager viewPager) {
        if (viewPager == null) return;
        
        // 检查是否已处理
        synchronized (lock) {
            if (processedViews.containsKey(viewPager)) {
                return;
            }
            processedViews.put(viewPager, Boolean.TRUE);
        }
        
        // 禁用页面转换动画
        viewPager.setPageTransformer(false, null);
        
        // 设置较大的偏移阈值，减少意外滑动
        try {
            Field mTouchSlop = ViewPager.class.getDeclaredField("mTouchSlop");
            mTouchSlop.setAccessible(true);
            int touchSlop = (int) mTouchSlop.get(viewPager);
            mTouchSlop.set(viewPager, touchSlop * 3);
        } catch (Exception e) {
            // 忽略反射错误
        }
    }

    /**
     * 禁用 ListView 的动画
     */
    public static void disableListViewAnimations(ListView listView) {
        if (listView == null) return;
        
        // 检查是否已处理
        synchronized (lock) {
            if (processedViews.containsKey(listView)) {
                return;
            }
            processedViews.put(listView, Boolean.TRUE);
        }
        
        // 禁用滚动缓存
        listView.setScrollingCacheEnabled(false);
        
        // 禁用过度滚动效果
        listView.setOverScrollMode(View.OVER_SCROLL_NEVER);
        
        // 禁用边缘效果
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                Method setEdgeEffectColor = AbsListView.class.getMethod("setEdgeEffectColor", int.class);
                setEdgeEffectColor.invoke(listView, Color.TRANSPARENT);
            } catch (Exception e) {
                // 忽略反射错误
            }
        }
    }

    /**
     * 触发设备级别的全局刷新，添加频率限制
     */
    public static void triggerGlobalRefresh(Context context) {
        long currentTime = System.currentTimeMillis();
        
        // 限制刷新频率，避免过度刷新
        synchronized (lock) {
            if (currentTime - lastGlobalRefreshTime < MIN_REFRESH_INTERVAL) {
                return;
            }
            lastGlobalRefreshTime = currentTime;
        }
        
        try {
            // 尝试使用博阅、汉王、文石等电纸书设备特定的刷新API
            // 以下是模拟的代码，实际实现需要针对特定设备
            
            // 博阅设备刷新API示例
            try {
                Class<?> epdClass = Class.forName("android.view.EpdController");
                Method epdcMethod = epdClass.getMethod("invalidate", View.class, int.class, int.class, int.class, int.class, int.class);
                epdcMethod.invoke(null, null, 0, 0, 0, 0, 2); // 2表示GC16/DU4刷新模式
                return; // 成功刷新后返回
            } catch (Exception e1) {
                // 博阅API不可用，尝试其他设备API
            }
            
            // 文石设备刷新API示例
            try {
                Class<?> epdClass = Class.forName("android.hardware.eink.EinkManager");
                Method getInstanceMethod = epdClass.getMethod("getInstance");
                Object einkManager = getInstanceMethod.invoke(null);
                
                Method refreshMethod = einkManager.getClass().getMethod("fullRefresh");
                refreshMethod.invoke(einkManager);
                return; // 成功刷新后返回
            } catch (Exception e2) {
                // 文石API不可用，尝试其他通用方法
            }
            
            // 其他制造商设备，如海信等
            try {
                Class<?> cls = Class.forName("android.eink.EinkManager");
                Method getInstanceMethod = cls.getMethod("getInstance");
                Object einkManager = getInstanceMethod.invoke(null);
                
                Method refreshMethod = einkManager.getClass().getMethod("fullRefresh");
                refreshMethod.invoke(einkManager);
                return; // 成功刷新后返回
            } catch (Exception e3) {
                // 通用刷新方法不可用
            }
            
            // 在没有任何特殊API可用的情况下使用系统通用刷新方法
            if (context instanceof Activity) {
                Activity activity = (Activity) context;
                View decorView = activity.getWindow().getDecorView();
                if (decorView != null) {
                    decorView.invalidate();
                }
            }
        } catch (Exception e) {
            // 忽略所有刷新错误
        }
    }
    
    /**
     * 刷新特定视图，添加频率限制
     */
    public static void refreshView(View view) {
        if (view == null) return;
        
        long currentTime = System.currentTimeMillis();
        
        // 限制刷新频率，避免过度刷新
        synchronized (lock) {
            if (currentTime - lastGlobalRefreshTime < MIN_REFRESH_INTERVAL) {
                return;
            }
            lastGlobalRefreshTime = currentTime;
        }
        
        try {
            // 使用博阅等设备特定的API刷新视图
            try {
                Class<?> epdClass = Class.forName("android.view.EpdController");
                Method updateMethod = epdClass.getMethod("invalidate", View.class, int.class);
                // 进行局部刷新
                updateMethod.invoke(null, view, 1); // 1表示A2/DU模式，更快的局部刷新
                return; // 成功后返回
            } catch (Exception e) {
                // 特定API不可用，使用通用方法
            }
            
            // 其他设备的通用局部刷新方法
            view.invalidate();
            
        } catch (Exception e) {
            // 忽略刷新错误
        }
    }
    
    /**
     * 清除处理缓存，释放内存
     */
    public static void clearCache() {
        synchronized (lock) {
            processedViews.clear();
        }
    }
} 