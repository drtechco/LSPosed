#!/system/bin/sh
set -e

DAND_DIR="/data/adb/dand"
DAND_ZIP="/system/etc/dand.zip"
DAND_APK="$DAND_DIR/daemon.apk"
DAND_MANAGER_APK="$DAND_DIR/manager.apk" 
DAND_DEX="$DAND_DIR/dand.dex"
PID_FILE="$DAND_DIR/dand.pid"
LOG_TAG="dand"
MAX_RETRIES=3000
RETRY_INTERVAL=2

log_msg() {
    local level="$1"
    local message="$2"
    echo "$LOG_TAG: $message"
    log -p "$level" -t "$LOG_TAG" "$message"
}

log_info() {
    log_msg "i" "$1"
}

log_error() {
    log_msg "e" "$1"
}

# 检查进程是否运行
check_process() {
    if [ -f "$PID_FILE" ] && [ -r "$PID_FILE" ]; then
        local pid
        pid=$(cat "$PID_FILE" 2>/dev/null)
        if [ -n "$pid" ]; then
            if kill -0 "$pid" 2>/dev/null; then
                if ps -p "$pid" 2>/dev/null | grep -q "dand"; then
                    return 0  # 进程正在运行
                fi
            fi
        fi
        rm -f "$PID_FILE" 2>/dev/null
    fi
    return 1  # 进程未运行
}

# 启动 DAND 进程
start_dand() {
    log_info "Starting DAND daemon"
    echo $$ > "$PID_FILE"
    
    # 使用 nohup 后台运行并重定向输出
    nohup app_process64 \
        -Djava.class.path="$DAND_APK" \
        "$DAND_DIR" \
        --nice-name=dand \
        com.google.dand.Main > "$DAND_DIR/dand.log" 2>&1 &
    
    local daemon_pid=$!
    echo $daemon_pid > "$PID_FILE"
    
    # 等待进程启动
    sleep 2
    if kill -0 $daemon_pid 2>/dev/null; then
        log_info "DAND daemon started with PID $daemon_pid"
        return 0
    else
        log_error "Failed to start DAND daemon"
        return 1
    fi
}

# 监控并在需要时重启进程
monitor_and_restart() {
    local retry_count=0
    
    while true; do
        if ! check_process; then
            retry_count=$((retry_count + 1))
            log_error "DAND process died, attempt $retry_count of $MAX_RETRIES"
            
            if [ $retry_count -le $MAX_RETRIES ]; then
                log_info "Restarting DAND daemon in $RETRY_INTERVAL seconds"
                sleep $RETRY_INTERVAL
                
                if start_dand; then
                    retry_count=0  # 重置重试计数
                    log_info "DAND daemon restarted successfully"
                else
                    log_error "Failed to restart DAND daemon"
                    [ $retry_count -eq $MAX_RETRIES ] && break
                fi
            else
                log_error "Max retry attempts ($MAX_RETRIES) reached, giving up"
                break
            fi
        fi
        sleep 10  # 检查间隔
    done
}

# 初始化
if [ ! -d "$DAND_DIR" ]; then
    if ! mkdir -p "$DAND_DIR" 2>/dev/null; then
        log_error "Failed to create directory: $DAND_DIR"
        exit 1
    fi
    chmod 755 "$DAND_DIR"
    log_info "Created DAND directory"
fi

# 检查和解压文件
if [ ! -f "$DAND_APK" ] || [ ! -f "$DAND_MANAGER_APK" ] || [ ! -f "$DAND_DEX" ]; then
    if [ ! -f "$DAND_ZIP" ]; then
        log_error "dand.zip not found at $DAND_ZIP"
        exit 1
    fi
    
    log_info "Extracting dand.zip"
    if ! unzip -o "$DAND_ZIP" -d "$DAND_DIR" >/dev/null 2>&1; then
        log_error "Failed to extract dand.zip"
        exit 1
    fi
    
    for file in "$DAND_APK" "$DAND_MANAGER_APK" "$DAND_DEX"; do
        if [ ! -f "$file" ]; then
            log_error "Required file missing after extraction: $file"
            exit 1
        fi
        chmod 644 "$file"
    done
    log_info "Files extracted successfully"
fi

cd "$DAND_DIR" || exit 1

# 检查是否已运行
if check_process; then
    log_info "DAND process already running"
    exit 0
fi

# 启动并监控进程
if start_dand; then
    monitor_and_restart
else
    log_error "Initial start failed"
    exit 1
fi