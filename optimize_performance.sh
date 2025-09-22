#!/bin/bash

# 电子墨水屏应用性能优化脚本
# 此脚本用于优化应用性能，减少资源消耗，提高电子墨水屏上的响应速度

echo "开始执行性能优化..."

# 停止Gradle守护进程，释放内存
echo "停止Gradle守护进程..."
./gradlew --stop

# 清理项目
echo "清理项目..."
./gradlew clean

# 停止可能的Android Studio进程，释放资源
if pgrep -f "android studio" > /dev/null; then
    echo "检测到Android Studio正在运行，建议关闭以释放系统资源..."
fi

# 优化构建参数
echo "使用优化参数构建应用..."
./gradlew --daemon --parallel --max-workers=4 --configure-on-demand assembleRelease

# 检查是否构建成功
if [ $? -ne 0 ]; then
    echo "构建失败，请检查日志文件..."
    exit 1
fi

echo "构建成功!"

# 提供性能优化建议
echo ""
echo "=== 性能优化建议 ==="
echo "1. 内存使用优化："
echo "   - 使用DatabaseOptimizer类进行数据库操作，减少I/O操作"
echo "   - 在应用退出时调用DatabaseOptimizer.getInstance().shutdown()释放资源"
echo "   - 对大型列表使用分页加载，避免一次性加载过多数据"
echo ""
echo "2. UI性能优化："
echo "   - 使用新增的UIPerformanceOptimizer类对UI进行电子墨水屏优化"
echo "   - 在MainActivity的onCreate方法中添加:"
echo "     UIPerformanceOptimizer.getInstance().registerActivity(this);"
echo "   - 对RecyclerView应用优化:"
echo "     UIPerformanceOptimizer.getInstance().optimizeRecyclerView(yourRecyclerView);"
echo "   - 减少UI刷新频率，使用scheduleDelayedUpdate方法延迟更新视图"
echo ""
echo "3. 电池优化："
echo "   - 减少网络请求频率"
echo "   - 合并数据库操作，使用批量API"
echo "   - 推荐设置电子墨水屏优化级别为1(适中优化):"
echo "     UIPerformanceOptimizer.getInstance().setEInkOptimizationLevel(1);"
echo ""
echo "4. 编译优化："
echo "   - 已在build.gradle中添加资源缩减和代码优化"
echo "   - 定期运行 ./gradlew clean 清理缓存"
echo ""
echo "5. 布局优化："
echo "   - 减少视图层次结构嵌套"
echo "   - 使用ConstraintLayout替代复杂的嵌套布局"
echo "   - 避免过深的视图层次"
echo ""
echo "6. 图片优化："
echo "   - 对于电子墨水屏，使用黑白图片代替彩色图片"
echo "   - 减少图片分辨率和质量"
echo "   - 考虑使用矢量图形（SVG）代替位图"
echo ""
echo "7. 应用启动优化："
echo "   - 延迟加载非必要组件"
echo "   - 考虑使用预加载技术，在TaskDao中添加预加载方法"
echo ""
echo "8. 定期维护："
echo "   - 建议定期运行此脚本优化性能"
echo "   - 监控应用内存使用和CPU占用"
echo ""

echo "性能优化完成!"
exit 0 