package com.stu.calender2.utils;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import java.util.List;
import java.util.ArrayList;

/**
 * 视图分块渲染助手类
 * 用于处理过大的LinearLayout无法在软件渲染层显示的问题
 */
public class ViewChunkingHelper {
    private static final String TAG = "ViewChunkingHelper";
    private static final int DEFAULT_CHUNK_SIZE = 5; // 增加默认每块的视图数量
    private static final int DEFAULT_DELAY_MS = 10; // 减少默认延迟毫秒数
    
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    
    /**
     * 对LinearLayout的子视图进行分块添加
     * @param container 容器LinearLayout
     * @param children 要添加的子视图列表
     * @param chunkSize 每块的视图数量
     * @param delayMs 每块之间的延迟时间（毫秒）
     * @param callback 全部添加完成后的回调
     */
    public void addViewsInChunks(final LinearLayout container, 
                                final List<View> children, 
                                final int chunkSize, 
                                final int delayMs,
                                final Runnable callback) {
        if (container == null || children == null || children.isEmpty()) {
            if (callback != null) {
                callback.run();
            }
            return;
        }
        
        // 清除容器中现有的视图
        container.removeAllViews();
        
        // 禁用绘图缓存以减少内存使用
        container.setDrawingCacheEnabled(false);
        container.setWillNotCacheDrawing(true);
        container.setLayerType(View.LAYER_TYPE_NONE, null); // 禁用所有图层
        
        // 创建分块添加的任务
        addChunk(container, new ArrayList<>(children), 0, chunkSize, delayMs, callback);
    }
    
    /**
     * 使用默认参数对LinearLayout的子视图进行分块添加
     * @param container 容器LinearLayout
     * @param children 要添加的子视图列表
     * @param callback 全部添加完成后的回调
     */
    public void addViewsInChunks(final LinearLayout container, 
                                final List<View> children, 
                                final Runnable callback) {
        addViewsInChunks(container, children, DEFAULT_CHUNK_SIZE, DEFAULT_DELAY_MS, callback);
    }
    
    /**
     * 递归添加视图块
     */
    private void addChunk(final LinearLayout container, 
                         final List<View> remaining, 
                         final int startIndex,
                         final int chunkSize, 
                         final int delayMs,
                         final Runnable callback) {
        if (startIndex >= remaining.size()) {
            Log.d(TAG, "All chunks added successfully");
            
            // 强制重绘确保所有内容可见
            container.postInvalidate();
            
            if (callback != null) {
                callback.run();
            }
            
            return;
        }
        
        // 确定本次要添加的视图数量
        final int endIndex = Math.min(startIndex + chunkSize, remaining.size());
        
        // 添加这一块的视图
        for (int i = startIndex; i < endIndex; i++) {
            View child = remaining.get(i);
            // 如果已经有父视图，先移除
            if (child.getParent() != null && child.getParent() instanceof ViewGroup) {
                ((ViewGroup) child.getParent()).removeView(child);
            }
            
            // 为子视图禁用图层和缓存
            optimizeViewForMemory(child);
            
            // 添加到容器
            container.addView(child);
        }
        
        // 强制进行布局计算
        container.requestLayout();
        
        // 延迟一段时间后添加下一块，让UI线程有时间喘息
        final int nextStartIndex = endIndex;
        mainHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                // 添加下一个块
                addChunk(container, remaining, nextStartIndex, chunkSize, delayMs, callback);
            }
        }, delayMs);
    }
    
    /**
     * 优化视图，减少内存使用
     */
    private void optimizeViewForMemory(View view) {
        if (view == null) return;
        
        view.setDrawingCacheEnabled(false);
        view.setWillNotCacheDrawing(true);
        
        // 递归优化子视图
        if (view instanceof ViewGroup) {
            ViewGroup viewGroup = (ViewGroup) view;
            viewGroup.setAnimationCacheEnabled(false);
            
            for (int i = 0; i < viewGroup.getChildCount(); i++) {
                optimizeViewForMemory(viewGroup.getChildAt(i));
            }
        }
    }
    
    /**
     * 为指定布局中的大型LinearLayout设置分块渲染
     * @param rootView 根视图
     * @param linearLayoutId 目标LinearLayout的ID
     */
    public void enableChunkingForLayout(View rootView, int linearLayoutId) {
        LinearLayout targetLayout = rootView.findViewById(linearLayoutId);
        if (targetLayout == null) {
            Log.e(TAG, "Target LinearLayout not found");
            return;
        }
        
        // 设置优化属性
        targetLayout.setDrawingCacheEnabled(false);
        targetLayout.setAnimationCacheEnabled(false);
        targetLayout.setWillNotCacheDrawing(true);
        
        // 提取当前子视图
        List<View> children = new ArrayList<>();
        for (int i = 0; i < targetLayout.getChildCount(); i++) {
            children.add(targetLayout.getChildAt(i));
        }
        
        // 应用分块渲染
        addViewsInChunks(targetLayout, children, null);
    }
} 