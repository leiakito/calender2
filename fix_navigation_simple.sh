#!/bin/sh

# 简化版Navigation资源修复脚本

echo "开始修复Navigation资源问题..."

# 停止Gradle守护进程
./gradlew --stop

# 清理项目
./gradlew clean

# 删除build文件夹
rm -rf app/build/
rm -rf .gradle/

# 强制更新依赖
echo "重新构建项目并刷新依赖..."
./gradlew --refresh-dependencies assembleDebug

echo "修复完成，如果问题仍然存在，请尝试以下操作："
echo "1. 在Android Studio中执行：File > Invalidate Caches / Restart"
echo "2. 确保activity_main.xml中app:navGraph和app:defaultNavHost属性正确引用"
echo "3. 确保nav_graph.xml中的app:startDestination和app:destination等属性正确引用" 