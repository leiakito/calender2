#!/bin/sh

# 专门针对Navigation资源问题的修复脚本

echo "开始修复Navigation资源问题..."

# 停止Gradle守护进程
./gradlew --stop

# 清理项目
./gradlew clean

# 确保NavHostFragment正确加载

# 创建临时目录以存储依赖项
mkdir -p temp_deps

# 下载特定版本的Navigation依赖到临时目录
echo "下载Navigation依赖..."
cd temp_deps
wget -q https://maven.google.com/androidx/navigation/navigation-fragment/2.7.7/navigation-fragment-2.7.7.aar
wget -q https://maven.google.com/androidx/navigation/navigation-ui/2.7.7/navigation-ui-2.7.7.aar
wget -q https://maven.google.com/androidx/navigation/navigation-runtime/2.7.7/navigation-runtime-2.7.7.aar
wget -q https://maven.google.com/androidx/navigation/navigation-common/2.7.7/navigation-common-2.7.7.aar
cd ..

# 创建本地仓库目录
mkdir -p local_repo

# 复制依赖项到本地仓库
echo "安装Navigation依赖到本地仓库..."
./gradlew publishToMavenLocal

# 使用指定的Navigation版本构建应用
echo "使用特定版本的Navigation构建应用..."
./gradlew --refresh-dependencies assembleDebug

# 清理临时文件
rm -rf temp_deps
echo "临时文件已清理"

echo "修复完成。如果仍有问题，请尝试以下操作："
echo "1. 在Android Studio中执行：File > Invalidate Caches / Restart"
echo "2. 检查布局文件中Navigation命名空间的引用"
echo "3. 确保导航图(nav_graph.xml)没有语法错误" 