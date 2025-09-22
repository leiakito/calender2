#!/bin/sh

# 清理项目脚本，用于解决资源链接问题

echo "开始清理构建缓存..."

# 停止任何运行中的Gradle守护进程
./gradlew --stop

# 清理项目
./gradlew clean

# 清理Gradle缓存
rm -rf ~/.gradle/caches/build-cache-*
rm -rf ~/.gradle/caches/transforms-*
rm -rf ~/.gradle/caches/journal-*

# 清理项目的构建文件夹
rm -rf app/build/
rm -rf .gradle/

echo "清理完成！现在尝试重新构建项目："
echo "./gradlew assembleDebug" 