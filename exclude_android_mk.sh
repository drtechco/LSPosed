#!/bin/bash

SCRIPT_DIR="$( cd "$( dirname "$0" )" && pwd )"

echo "脚本所在目录: $SCRIPT_DIR"
 
# 设置需要排除的根目录
EXCLUDE_ROOT=$SCRIPT_DIR

# 检查根目录是否存在
if [ ! -d "$EXCLUDE_ROOT" ]; then
    echo "错误: 目录 $EXCLUDE_ROOT 不存在。"
    exit 1
fi

# 查找所有包含 Android.mk 文件的目录
directories=$(find "$EXCLUDE_ROOT" -type f -name "Android.mk" -exec dirname {} \;)

# 遍历每个目录，创建一个空的 android.bp 文件
for dir in $directories; do
    if [ ! -f "$dir/android.bp" ]; then
        touch "$dir/android.bp"
        echo "已在目录 $dir 中创建空的 android.bp 文件以排除 Android.mk"
    else
        echo "目录 $dir 中已存在 android.bp 文件，跳过创建"
    fi
done

echo "所有包含 Android.mk 的目录均已处理完毕。"
