#!/bin/bash
set -eu

PROJECT_DIR="/home/nidhi/Projects/billing"
cd "$PROJECT_DIR"

# Ensure classes are compiled
if [ ! -f "$PROJECT_DIR/build/classes/App.class" ]; then
    mkdir -p "$PROJECT_DIR/build/classes"
    find "$PROJECT_DIR/src" -name '*.java' -print0 | xargs -0 javac -encoding UTF-8 -cp "$PROJECT_DIR/lib/*" -d "$PROJECT_DIR/build/classes"
fi

exec java -Dfile.encoding=UTF-8 \
          -Dsun.java2d.opengl=true \
          -Dawt.useSystemAAFontSettings=on \
          -Dswing.aatext=true \
          -cp "$PROJECT_DIR/build/classes:$PROJECT_DIR/lib/*" \
          App "$@"
