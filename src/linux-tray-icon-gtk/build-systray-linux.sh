gcc -fPIC -I"$JAVA_HOME/include" \
 `pkg-config --cflags gtk+-2.0` \
 -I/usr/lib/jvm/java-11-oracle/include/linux/ \
 -shared \
 -o stage/lib/nix/tray_icon_jni64.so \
 src/linux-tray-icon-c-gtk/LinuxTrayIcon.c \
 `pkg-config --libs gtk+-2.0`
