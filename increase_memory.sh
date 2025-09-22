#!/bin/bash

# 增加应用内存分配脚本
echo "===== 增加应用内存分配 ====="
echo "此脚本将修改AndroidManifest.xml和gradle配置，增加应用可用内存"

# 确保AndroidManifest.xml存在
MANIFEST_FILE="app/src/main/AndroidManifest.xml"
if [ ! -f "$MANIFEST_FILE" ]; then
    echo "错误: AndroidManifest.xml文件不存在"
    exit 1
fi

# 检查并修改AndroidManifest.xml中的largeHeap属性
if grep -q "android:largeHeap=\"true\"" "$MANIFEST_FILE"; then
    echo "AndroidManifest.xml已包含largeHeap设置"
else
    # 使用临时文件进行修改
    TEMP_FILE=$(mktemp)
    awk '/<application/{gsub(/<application /, "<application android:largeHeap=\"true\" "); print; next} {print}' "$MANIFEST_FILE" > "$TEMP_FILE"
    mv "$TEMP_FILE" "$MANIFEST_FILE"
    echo "已添加largeHeap=\"true\"到AndroidManifest.xml"
fi

# 修改gradle.properties以增加虚拟机内存
GRADLE_PROPS="gradle.properties"
if [ -f "$GRADLE_PROPS" ]; then
    # 检查是否已包含内存设置
    if grep -q "org.gradle.jvmargs=-Xmx2048m" "$GRADLE_PROPS"; then
        echo "gradle.properties已包含内存设置"
    else
        echo "" >> "$GRADLE_PROPS"
        echo "# 增加内存设置，解决大型布局问题" >> "$GRADLE_PROPS"
        echo "org.gradle.jvmargs=-Xmx4096m -Dfile.encoding=UTF-8" >> "$GRADLE_PROPS"
        echo "android.enableJetifier=true" >> "$GRADLE_PROPS"
        echo "android.useAndroidX=true" >> "$GRADLE_PROPS"
        echo "# 禁用资源压缩，防止布局资源被错误优化" >> "$GRADLE_PROPS"
        echo "android.enableResourceOptimizations=false" >> "$GRADLE_PROPS"
        echo "已更新gradle.properties增加内存设置"
    fi
fi

# 修改app/build.gradle以优化dex选项
APP_GRADLE="app/build.gradle"
if [ -f "$APP_GRADLE" ]; then
    # 检查是否已包含dexOptions设置
    if grep -q "dexOptions" "$APP_GRADLE"; then
        echo "app/build.gradle已包含dexOptions设置"
    else
        # 使用临时文件进行修改
        TEMP_FILE=$(mktemp)
        awk '/android {/{print; print "    dexOptions {"; print "        javaMaxHeapSize \"4g\""; print "        jumboMode true"; print "        preDexLibraries true"; print "        maxProcessCount 8"; print "    }"; next} {print}' "$APP_GRADLE" > "$TEMP_FILE"
        mv "$TEMP_FILE" "$APP_GRADLE"
        echo "已添加dexOptions到app/build.gradle"
    fi
fi

# 打印使用说明
echo ""
echo "===== 使用说明 ====="
echo "1. 已增加应用的内存分配，建议清理项目后重新构建"
echo "2. 执行以下命令清理并重建项目:"
echo "   ./gradlew clean"
echo "   ./gradlew assembleDebug"
echo ""
echo "3. 如果在设备上仍然有问题，可以使用adb增加应用的内存分配:"
echo "   adb shell am set-proc-limit -c com.stu.calender2 -m 2048"
echo ""
echo "===== 优化完成 ====="

# 设置脚本为可执行
chmod +x increase_memory.sh

echo "脚本创建完成，使用 ./increase_memory.sh 执行优化" 