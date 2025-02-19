adb push ./out/target/product/beyond1lte/system/bin/app_process /data/local/tmp/



mount -o remount,rw /


cp -rf /data/local/tmp/app_process32 /system/bin/app_process32
cp -rf /data/local/tmp/app_process64 /system/bin/app_process64
restorecon -R /system/bin/app_process32
restorecon -R /system/bin/app_process64



adb push out/target/product/beyond1lte/obj/SHARED_LIBRARIES/libdand_intermediates/libdand.so  /data/local/tmp/libdand.so.64
adb push out/target/product/beyond1lte/obj_arm/SHARED_LIBRARIES/libdand_intermediates/libdand.so  /data/local/tmp/libdand.so.32
cp -rf /data/local/tmp/libdand.so.64   /system/lib64/libdand.so
cp -rf /data/local/tmp/libdand.so.32   /system/lib/libdand.so
adb push ./LSPosed/daemon/build/outputs/apk/debug/daemon-debug.apk  /data/local/tmp/

app_process -Djava.class.path=/data/local/tmp/daemon-debug.apk  /system/bin --nice-name=dand com.google.dand.Main

adb push ./out/target/product/beyond1lte/obj/SHARED_LIBRARIES/libandroid_runtime_intermediates/libandroid_runtime.so /data/local/tmp/libandroid_runtime.so.64
adb push ./out/target/product/beyond1lte/obj_arm/SHARED_LIBRARIES/libandroid_runtime_intermediates/libandroid_runtime.so /data/local/tmp/libandroid_runtime.so.32
cp -rf /data/local/tmp/libandroid_runtime.so.64   /system/lib64/libandroid_runtime.so
cp -rf /data/local/tmp/libandroid_runtime.so.32   /system/lib/libandroid_runtime.so


adb push out/target/product/beyond1lte/system/etc/selinux/plat_sepolicy.cil /data/local/tmp/
cp -rf /data/local/tmp/plat_sepolicy.cil system/etc/selinux/plat_sepolicy.cil





adb push  ~/Downloads/dand.dex/classes.dex /data/local/tmp/framework/dand.dex
cp -rf /data/local/tmp/dand.dex /system/framework/dand.dex

unzip -j ./LSPosed/magisk-loader/build/outputs/apk/Zygisk/debug/magisk-loader-Zygisk-debug.apk  classes.dex  -d ~/Downloads/dand.dex


secilc -M true \
    -c 31 \
    -o test_policy \
    -f file_contexts \
    -v \
    -N \
    -m \
    /vendor/etc/selinux/plat_pub_versioned.cil \
    /system/etc/selinux/plat_sepolicy.cil \
    /vendor/etc/selinux/vendor_sepolicy.cil



#setenforce 1
#setenforce 0
load_policy ./test_policy
./checkpolicy -b -F -M -c 31 -d /sys/fs/selinux/policy
#Choose:  2
#scontext?  u:r:system_server:s0
#
#sid 28
#
#Choose:  0
#source sid?  28
#target sid?  28
#target class?  process
#
#allowed { fork sigchld sigkill sigstop signull signal ptrace getsched setsched getsession getpgid setpgid getcap setcap getattr setfscreate setrlimit execmem }


make bootimage -j20
adb push out/target/product/beyond1lte/boot.img /mnt/sdcard/boot_n.img
dd if=/dev/block/sda14 of=/sdcard/boot2025-01-17.img
dd if=/sdcard/boot_n.img of=/dev/block/sda14 bs=4096


make systemimage -j20
simg2img out/target/product/beyond1lte/system.img out/target/product/beyond1lte/system_raw.img
dd if=/dev/block/sda25 of=/sdcard/system2025-01-17.img
dd if=/sdcard/system_raw_n.img of=/dev/block/sda25 bs=4096




adb push /Users/ttttt/d/lineage22/LSPosed/app/build/outputs/apk/debug/app-debug.apk  /data/local/tmp/manager.apk
