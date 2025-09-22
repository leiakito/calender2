package com.stu.calender2.utils;

import android.content.Context;
import android.os.Handler;
import android.provider.Settings;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.LayoutAnimationController;
import android.widget.AbsListView;
import android.widget.ListView;
import android.view.animation.AlphaAnimation;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * 水墨屏动画优化工具类
 */
public class EInkAnimationUtils {

    /**
     * 全局禁用动画
     */
    public static void disableAllAnimationsGlobally(Context context) {
        try {
            // 尝试使用系统API禁用动画缩放
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
     * 递归禁用视图层次结构中的所有动画
     */
    public static void disableAnimationsRecursively(View view) {
        if (view == null) return;
        
        // 禁用此视图的动画
        view.setAnimation(null);
        view.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        
        try {
            Method setAnimationCacheEnabled = View.class.getMethod("setAnimationCacheEnabled", boolean.class);
            setAnimationCacheEnabled.invoke(view, false);
        } catch (Exception e) {
            // 忽略不支持的方法
        }
        
        // 特殊处理ListView和类似组件
        if (view instanceof AbsListView) {
            AbsListView listView = (AbsListView) view;
            listView.setLayoutAnimation(null);
            listView.setScrollingCacheEnabled(false);
        }
        
        // 递归处理子视图
        if (view instanceof ViewGroup) {
            ViewGroup viewGroup = (ViewGroup) view;
            for (int i = 0; i < viewGroup.getChildCount(); i++) {
                disableAnimationsRecursively(viewGroup.getChildAt(i));
            }
            
            // 禁用布局动画
            viewGroup.setLayoutAnimation(null);
            viewGroup.setAnimationCacheEnabled(false);
        }
    }
    
    /**
     * 创建适合水墨屏的最小化过渡动画
     */
    public static Animation createMinimalEInkAnimation(Context context) {
        Animation anim = new AlphaAnimation(1.0f, 1.0f);
        anim.setDuration(10);
        return anim;
    }
    
    /**
     * 强制刷新视图，适用于水墨屏
     */
    public static void forceRefreshView(final View view) {
        if (view == null) return;
        
        // 使视图短暂不可见然后再可见，触发重绘
        view.setVisibility(View.INVISIBLE);
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                view.setVisibility(View.VISIBLE);
                view.invalidate();
            }
        }, 50);
    }
} 