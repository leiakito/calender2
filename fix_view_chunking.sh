#!/bin/bash

# 视图分块渲染优化脚本
# 用于解决"LinearLayout not displayed because it is too large to fit into a software layer"问题

echo "===== 视图分块渲染优化 ====="
echo "此脚本将应用视图分块渲染优化，解决大型LinearLayout渲染问题"

# 确保目录存在
mkdir -p app/src/main/java/com/stu/calender2/utils

# 添加ViewChunkingHelper类
cat > app/src/main/java/com/stu/calender2/utils/ViewChunkingHelper.java << 'EOF'
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
    private static final int DEFAULT_CHUNK_SIZE = 5; // 默认每块的视图数量
    private static final int DEFAULT_DELAY_MS = 10; // 每块之间的延迟毫秒数
    
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    
    /**
     * 对LinearLayout的子视图进行分块添加
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
        
        // 创建分块添加的任务
        addChunk(container, new ArrayList<>(children), 0, chunkSize, delayMs, callback);
    }
    
    /**
     * 使用默认参数对LinearLayout的子视图进行分块添加
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
            container.addView(child);
        }
        
        // 强制进行布局计算，但避免全局布局请求
        container.requestLayout();
        
        // 延迟一段时间后添加下一块
        final int nextStartIndex = endIndex;
        mainHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                addChunk(container, remaining, nextStartIndex, chunkSize, delayMs, callback);
            }
        }, delayMs);
    }
    
    /**
     * 为指定布局中的大型LinearLayout设置分块渲染
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
EOF

echo "已创建 ViewChunkingHelper 工具类"

# 更新应用的 gradle.properties，增加内存优化设置
if [ -f gradle.properties ]; then
    # 检查是否已经包含这些设置
    if ! grep -q "org.gradle.jvmargs=-Xmx2048m" gradle.properties; then
        echo "" >> gradle.properties
        echo "# 增加内存设置，解决大型布局问题" >> gradle.properties
        echo "org.gradle.jvmargs=-Xmx2048m -Dfile.encoding=UTF-8" >> gradle.properties
        echo "android.enableJetifier=true" >> gradle.properties
        echo "android.useAndroidX=true" >> gradle.properties
        echo "# 禁用资源压缩，防止布局资源被错误优化" >> gradle.properties
        echo "android.enableResourceOptimizations=false" >> gradle.properties
        echo "已更新 gradle.properties 增加内存设置"
    else
        echo "gradle.properties 已包含必要设置"
    fi
fi

# 打印使用说明
echo ""
echo "===== 使用说明 ====="
echo "1. 已创建 ViewChunkingHelper 工具类，用于处理大型布局分块渲染"
echo "2. 在您的 Activity 或 Fragment 中，请按照以下方式使用："
echo ""
echo "   // 初始化 ViewChunkingHelper"
echo "   private ViewChunkingHelper viewChunkingHelper = new ViewChunkingHelper();"
echo ""
echo "   // 在 onCreate 或 onViewCreated 方法中应用到大型 LinearLayout"
echo "   LinearLayout largeLayout = findViewById(R.id.your_large_layout);"
echo "   List<View> children = new ArrayList<>();"
echo "   for (int i = 0; i < largeLayout.getChildCount(); i++) {"
echo "       children.add(largeLayout.getChildAt(i));"
echo "   }"
echo "   viewChunkingHelper.addViewsInChunks(largeLayout, children, null);"
echo ""
echo "3. 对于动态创建的内容，请使用分块方式添加子视图"
echo "4. 如有更多问题，请查看代码注释或联系开发者"
echo ""
echo "===== 优化完成 ====="

# 设置脚本为可执行
chmod +x fix_view_chunking.sh

echo "脚本创建完成，使用 ./fix_view_chunking.sh 执行优化" 