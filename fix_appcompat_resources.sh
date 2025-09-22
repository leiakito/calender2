#!/bin/sh

# 修复AppCompat和Navigation资源引用的脚本

echo "开始修复资源引用问题..."

# 停止Gradle守护进程
./gradlew --stop

# 清理项目
./gradlew clean

# 删除Gradle缓存
rm -rf ~/.gradle/caches/build-cache-*
rm -rf ~/.gradle/caches/transforms-*
rm -rf ~/.gradle/caches/journal-*

# 清理项目的构建文件夹
rm -rf app/build/
rm -rf .gradle/

# 使用invalidate缓存命令，更彻底地清理 (如果使用的是命令行，这一步可能不适用)
echo "如果使用Android Studio，建议执行：File > Invalidate Caches / Restart..."

# 重建项目
echo "开始重新构建项目..."
./gradlew --refresh-dependencies assembleDebug

echo "修复完成。"
echo "如果上述命令成功，你的问题应该已解决。"
echo "如果仍有问题，请检查以下方面："
echo "1. 确保Navigation依赖版本一致"
echo "2. 尝试在Android Studio中执行：File > Invalidate Caches / Restart"
echo "3. 检查build.gradle中是否有任何其他依赖排除项" 