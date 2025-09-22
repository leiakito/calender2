package com.stu.calender2.utils;

import android.os.Handler;
import android.view.View;

/**
 * 水墨屏专用的布局变化监听器，用于控制刷新率
 */
public class EInkLayoutChangeListener implements View.OnLayoutChangeListener {
    
    private long lastRefreshTime = 0;
    private static final long MIN_REFRESH_INTERVAL = 300; // 最小刷新间隔，单位毫秒
    private final Handler handler = new Handler();
    private Runnable pendingRefresh = null;
    
    @Override
    public void onLayoutChange(View v, int left, int top, int right, int bottom,
                              int oldLeft, int oldTop, int oldRight, int oldBottom) {
        // 检查是否需要刷新
        if (isLayoutChanged(left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom)) {
            long currentTime = System.currentTimeMillis();
            
            // 取消之前的待定刷新
            if (pendingRefresh != null) {
                handler.removeCallbacks(pendingRefresh);
            }
            
            // 如果距离上次刷新时间不够，则延迟刷新
            if (currentTime - lastRefreshTime < MIN_REFRESH_INTERVAL) {
                pendingRefresh = new Runnable() {
                    @Override
                    public void run() {
                        refreshViewForEInk(v);
                        lastRefreshTime = System.currentTimeMillis();
                    }
                };
                
                // 在最小间隔后刷新
                handler.postDelayed(pendingRefresh, MIN_REFRESH_INTERVAL);
            } else {
                // 可以立即刷新
                refreshViewForEInk(v);
                lastRefreshTime = currentTime;
            }
        }
    }
    
    /**
     * 检查布局是否发生实质性变化
     */
    private boolean isLayoutChanged(int left, int top, int right, int bottom,
                                  int oldLeft, int oldTop, int oldRight, int oldBottom) {
        // 检查布局变化是否足够显著以值得刷新
        int widthDiff = Math.abs((right - left) - (oldRight - oldLeft));
        int heightDiff = Math.abs((bottom - top) - (oldBottom - oldTop));
        
        // 如果宽高变化超过1像素，认为需要刷新
        return widthDiff > 1 || heightDiff > 1;
    }
    
    /**
     * 使用水墨屏友好的方式刷新视图
     */
    private void refreshViewForEInk(View view) {
        if (view == null) return;
        view.invalidate();
    }
} 