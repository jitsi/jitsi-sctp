#!/usr/bin/env bash
PROJECT_DIR="$(realpath "$(dirname "$0")/../")"
JAVA_VERSION=11
ARCHS=(x86-64 arm64)

if [ "$#" -ge 1 ]; then
    JAVA_HOME=$1
fi

if [ -z "$JAVA_HOME" ]; then
    JAVA_HOME="$(/usr/libexec/java_home -v $JAVA_VERSION)"
fi

if [ -z "$JAVA_HOME" ]; then
    echo "Could not find Java home; specify it on the command line, set it in the environment,"
    echo "or install it where /usr/libexec/java_home -v $JAVA_VERSION can find it."
    exit 1
fi

for ARCH in "${ARCHS[@]}"; do
    "$PROJECT_DIR/resources/macos-build.sh" "$JAVA_HOME" "$ARCH" "$PROJECT_DIR"
done
