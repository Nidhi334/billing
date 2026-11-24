#!/bin/sh
set -eu

# ==============================================================================
# SmartBilling Pro - Windows Executable (.exe) & Distribution Builder
# ==============================================================================

PROJECT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
APP_NAME="SmartBillingPro"
APP_VERSION="${APP_VERSION:-1.0.0}"
BUILD_DIR="$PROJECT_DIR/build"
DIST_DIR="$BUILD_DIR/dist"
PACKAGES_DIR="$BUILD_DIR/packages"
TOOLS_DIR="$PROJECT_DIR/tools"
LAUNCH4J_DIR="$TOOLS_DIR/launch4j"

LAUNCH4J_JAR="$LAUNCH4J_DIR/launch4j.jar"

echo "=== Building SmartBilling Pro Windows EXE (v${APP_VERSION}) ==="

# Check required tools
for tool in java javac jar zip; do
    if ! command -v "$tool" >/dev/null 2>&1; then
        echo "Error: Required tool not found: $tool" >&2
        exit 1
    fi
done

if [ ! -f "$LAUNCH4J_DIR/head/guihead.o" ] || [ ! -f "$LAUNCH4J_JAR" ]; then
    echo "Launch4j tool not found or incomplete. Setting up Launch4j 3.50..."
    mkdir -p "$TOOLS_DIR"
    curl -sL "https://sourceforge.net/projects/launch4j/files/launch4j-3/3.50/launch4j-3.50-linux-x64.tgz/download" | tar -xz -C "$TOOLS_DIR/"
    sed -i 's/\r$//' "$LAUNCH4J_DIR/launch4jc" "$LAUNCH4J_DIR/launch4j" 2>/dev/null || true
    chmod +x "$LAUNCH4J_DIR/launch4jc" "$LAUNCH4J_DIR/launch4j" "$LAUNCH4J_DIR/bin/"* 2>/dev/null || true
fi

mkdir -p "$DIST_DIR" "$PACKAGES_DIR"

TEMP_DIR=$(mktemp -d "$BUILD_DIR/exe-build.XXXXXX")
trap 'rm -rf "$TEMP_DIR"' EXIT HUP INT TERM

echo "[1/5] Compiling Java source files..."
mkdir -p "$TEMP_DIR/classes"
find "$PROJECT_DIR/src" -name '*.java' -print0 |
    xargs -0 javac -encoding UTF-8 -cp "$PROJECT_DIR/lib/*" -d "$TEMP_DIR/classes"

echo "[2/5] Extracting dependencies & building fat JAR..."
mkdir -p "$TEMP_DIR/fatjar"
cp -a "$TEMP_DIR/classes/." "$TEMP_DIR/fatjar/"

# Unpack all jars from lib/
for j in "$PROJECT_DIR/lib/"*.jar; do
    if [ -f "$j" ]; then
        (cd "$TEMP_DIR/fatjar" && jar xf "$j")
    fi
done

# Clean module-info and signing metadata from META-INF to avoid modular check and SecurityException
rm -f "$TEMP_DIR"/fatjar/module-info.class
rm -f "$TEMP_DIR"/fatjar/META-INF/*.SF "$TEMP_DIR"/fatjar/META-INF/*.DSA "$TEMP_DIR"/fatjar/META-INF/*.RSA

# Include data assets inside the JAR for fallback
if [ -d "$PROJECT_DIR/data" ]; then
    cp -a "$PROJECT_DIR/data" "$TEMP_DIR/fatjar/"
fi

# Package SmartBilling.jar
jar --create --file "$DIST_DIR/SmartBilling.jar" \
    --main-class App \
    -C "$TEMP_DIR/fatjar" .

echo "Created fat JAR: $DIST_DIR/SmartBilling.jar ($(du -h "$DIST_DIR/SmartBilling.jar" | cut -f1))"

echo "[3/5] Verifying Windows application icon..."
if [ ! -f "$PROJECT_DIR/packaging/app_icon.ico" ]; then
    echo "Generating packaging/app_icon.ico from data/assets/images/app_icon_squircle.png..."
    python3 -c "
from PIL import Image
img = Image.open('$PROJECT_DIR/data/assets/images/app_icon_squircle.png')
sizes = [(16,16), (32,32), (48,48), (64,64), (128,128), (256,256)]
img.save('$PROJECT_DIR/packaging/app_icon.ico', format='ICO', sizes=sizes)
"
fi

echo "[4/5] Running Launch4j to wrap JAR into Windows PE .EXE..."
java -jar "$LAUNCH4J_JAR" "$PROJECT_DIR/packaging/launch4j-config.xml"

if [ ! -f "$DIST_DIR/$APP_NAME.exe" ]; then
    echo "Error: Launch4j failed to generate $DIST_DIR/$APP_NAME.exe" >&2
    exit 1
fi

echo "Created Windows EXE: $DIST_DIR/$APP_NAME.exe ($(du -h "$DIST_DIR/$APP_NAME.exe" | cut -f1))"

echo "[5/5] Packaging standalone Windows distribution ZIP..."
DIST_BUNDLE_NAME="${APP_NAME}-Windows-${APP_VERSION}"
BUNDLE_DIR="$TEMP_DIR/$DIST_BUNDLE_NAME"
mkdir -p "$BUNDLE_DIR"

# Copy EXE and supporting directories
cp "$DIST_DIR/$APP_NAME.exe" "$BUNDLE_DIR/"
cp -a "$PROJECT_DIR/data" "$BUNDLE_DIR/"
cp -a "$PROJECT_DIR/database" "$BUNDLE_DIR/"

if [ -f "$PROJECT_DIR/app_settings.properties" ]; then
    cp "$PROJECT_DIR/app_settings.properties" "$BUNDLE_DIR/"
fi

if [ -f "$PROJECT_DIR/db_config.properties" ]; then
    cp "$PROJECT_DIR/db_config.properties" "$BUNDLE_DIR/"
fi

# Create Windows README
cat << 'EOF' > "$BUNDLE_DIR/README_WINDOWS.txt"
================================================================================
                    SmartBilling Pro - Retail POS System
                           Windows Distribution
================================================================================

QUICK START:
1. Double-click "SmartBillingPro.exe" to launch the application.

SYSTEM REQUIREMENTS:
- Windows 10, 11 or Windows Server (64-bit or 32-bit).
- Java Runtime Environment (JRE / JDK 8 or higher).
  If Java is not installed, the launcher will prompt to download Eclipse Temurin
  OpenJDK (https://adoptium.net/temurin/releases/).
- MySQL Database (e.g. MySQL Server 8.0, XAMPP, or MariaDB).

DATABASE SETUP:
1. Start your local MySQL service (e.g. via XAMPP Control Panel or MySQL service).
2. Open database/schema.sql and import it into your MySQL server:
   - Database name: billing_system
3. Configure your database password in "db_config.properties" or directly inside
   the application under Settings -> Database Connection.

DEFAULT LOGIN:
- Admin Username: admin
- Admin Password: password123 (or as configured in database)

SUPPORT & ASSETS:
- "data/" folder contains product images and icons.
- "app_settings.properties" stores your store name, UPI ID, and POS layout preferences.

================================================================================
EOF

OUTPUT_ZIP="$PACKAGES_DIR/${DIST_BUNDLE_NAME}.zip"
rm -f "$OUTPUT_ZIP"
(cd "$TEMP_DIR" && zip -r -q "$OUTPUT_ZIP" "$DIST_BUNDLE_NAME")

echo "=================================================================="
echo "SUCCESS: Windows release package created:"
echo "EXE file : $DIST_DIR/$APP_NAME.exe"
echo "ZIP bundle: $OUTPUT_ZIP ($(du -h "$OUTPUT_ZIP" | cut -f1))"
echo "=================================================================="
