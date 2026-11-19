#!/bin/sh
set -eu

PROJECT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
APP_NAME=smartbilling-pro
APP_VERSION=${APP_VERSION:-1.0.0}
BUILD_DIR="$PROJECT_DIR/build"
OUTPUT_DIR="$BUILD_DIR/packages"
mkdir -p "$BUILD_DIR"
TEMP_DIR=$(mktemp -d "$BUILD_DIR/deb-build.XXXXXX")
trap 'rm -rf "$TEMP_DIR"' EXIT HUP INT TERM

for tool in java javac jar jpackage dpkg-deb; do
    if ! command -v "$tool" >/dev/null 2>&1; then
        printf 'Required tool not found: %s\n' "$tool" >&2
        exit 1
    fi
done

mkdir -p "$OUTPUT_DIR" "$TEMP_DIR/classes" "$TEMP_DIR/input" "$TEMP_DIR/deb"

find "$PROJECT_DIR/src" -name '*.java' -print0 |
    xargs -0 javac -encoding UTF-8 -cp "$PROJECT_DIR/lib/*" -d "$TEMP_DIR/classes"

jar --create --file "$TEMP_DIR/input/SmartBilling.jar" \
    --main-class App -C "$TEMP_DIR/classes" .
cp "$PROJECT_DIR/lib/"*.jar "$TEMP_DIR/input/"
cp -a "$PROJECT_DIR/data" "$TEMP_DIR/input/"
cp -a "$PROJECT_DIR/database" "$TEMP_DIR/input/"
if [ -d "$PROJECT_DIR/product_images" ]; then
    cp -a "$PROJECT_DIR/product_images" "$TEMP_DIR/input/"
fi

jpackage \
    --type deb \
    --name "$APP_NAME" \
    --app-version "$APP_VERSION" \
    --input "$TEMP_DIR/input" \
    --main-jar SmartBilling.jar \
    --main-class App \
    --icon "$PROJECT_DIR/data/assets/images/app_icon_squircle.png" \
    --linux-package-name "$APP_NAME" \
    --linux-app-category Office \
    --dest "$TEMP_DIR/deb"

set -- "$TEMP_DIR"/deb/*.deb
if [ ! -f "$1" ]; then
    printf 'jpackage did not produce a Debian package.\n' >&2
    exit 1
fi

PACKAGE_ROOT="$TEMP_DIR/package-root"
dpkg-deb --raw-extract "$1" "$PACKAGE_ROOT"

if [ ! -x "$PACKAGE_ROOT/opt/$APP_NAME/bin/$APP_NAME" ]; then
    printf 'Could not locate the packaged application launcher.\n' >&2
    exit 1
fi

rm -f "$PACKAGE_ROOT/usr/bin/$APP_NAME"
install -D -m 755 "$PROJECT_DIR/scripts/launch-installed.sh" \
    "$PACKAGE_ROOT/usr/bin/$APP_NAME"
install -D -m 644 "$PROJECT_DIR/packaging/$APP_NAME.desktop" \
    "$PACKAGE_ROOT/usr/share/applications/$APP_NAME.desktop"
install -D -m 644 "$PROJECT_DIR/data/assets/images/app_icon_squircle.png" \
    "$PACKAGE_ROOT/usr/share/icons/hicolor/512x512/apps/$APP_NAME.png"

ARCH=$(dpkg --print-architecture)
OUTPUT="$OUTPUT_DIR/${APP_NAME}_${APP_VERSION}_${ARCH}.deb"
dpkg-deb --build --root-owner-group "$PACKAGE_ROOT" "$OUTPUT"
printf 'Created package: %s\n' "$OUTPUT"