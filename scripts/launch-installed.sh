#!/bin/sh
set -eu

APP_DIR=/opt/smartbilling-pro
APP_DATA_DIR=${XDG_DATA_HOME:-"${HOME:?HOME must be set}/.local/share"}/smartbilling-pro

mkdir -p "$APP_DATA_DIR"
for bundled_dir in data database product_images; do
    if [ -d "$APP_DIR/lib/app/$bundled_dir" ]; then
        cp -a -n "$APP_DIR/lib/app/$bundled_dir" "$APP_DATA_DIR/"
    fi
done

cd "$APP_DATA_DIR"
exec "$APP_DIR/bin/smartbilling-pro" "$@"