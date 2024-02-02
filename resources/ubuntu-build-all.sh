#!/usr/bin/env bash
PROJECT_DIR="$(realpath "$(dirname "$0")/../")"
JAVA_VERSION=11
ARCHS=(amd64 arm64 ppc64el)
DIST=focal

for ARCH in "${ARCHS[@]}"; do
    "$PROJECT_DIR/resources/ubuntu-build-docker.sh" "$ARCH" "$DIST" "$JAVA_VERSION"
done

