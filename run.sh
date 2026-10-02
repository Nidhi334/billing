#!/bin/bash
# Compile and run the Billing & Inventory Management System

cd "$(dirname "$0")"

echo "=========================================================="
echo "    Compiling Billing & Inventory Management System...    "
echo "=========================================================="

mkdir -p bin
javac -cp "lib/*:src" -d bin src/config/*.java src/model/*.java src/dao/*.java src/util/*.java src/ui/*.java src/App.java

if [ $? -eq 0 ]; then
    echo "✓ Compilation Successful!"
    echo "Launching SmartBilling Pro..."
    java -cp "bin:lib/*" App
else
    echo "✗ Compilation Failed!"
fi

