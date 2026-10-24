#!/bin/bash
# Compile and run the Billing & Inventory Management System

cd "$(dirname "$0")"

# Auto-detect macOS Java installation if not set
if [ -d "/Library/Java/JavaVirtualMachines/openjdk-17.jdk/Contents/Home" ]; then
    export JAVA_HOME="/Library/Java/JavaVirtualMachines/openjdk-17.jdk/Contents/Home"
    export PATH="$JAVA_HOME/bin:$PATH"
elif [ -z "$JAVA_HOME" ] && [ -x "/usr/libexec/java_home" ]; then
    export JAVA_HOME="$(/usr/libexec/java_home 2>/dev/null)"
    export PATH="$JAVA_HOME/bin:$PATH"
fi

echo "=========================================================="
echo "    Compiling Billing & Inventory Management System...    "
echo "=========================================================="

mkdir -p bin
if [ -f "tools/camera_scanner.m" ] && ( [ ! -f "bin/camera_scanner" ] || [ "tools/camera_scanner.m" -nt "bin/camera_scanner" ] ); then
    clang -O2 -framework Foundation -framework AVFoundation -framework Vision -framework AppKit -framework CoreMedia -framework CoreVideo -framework CoreImage tools/camera_scanner.m -o bin/camera_scanner 2>/dev/null || true
fi
javac -cp "lib/*:src" -d bin src/config/*.java src/model/*.java src/dao/*.java src/util/*.java src/ui/*.java src/App.java

if [ $? -eq 0 ]; then
    echo "✓ Compilation Successful!"
    echo "Launching SmartBilling Pro..."
    exec java -Xdock:icon="data/assets/images/app_icon_macos.png" -Xdock:name="SmartBilling Pro" -Dsun.java2d.metal=false -Dsun.java2d.opengl=true -cp "bin:lib/*" App
else
    echo "✗ Compilation Failed!"
    exit 1
fi
