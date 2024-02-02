#!/usr/bin/env bash
if [ "$#" -ne 3 ]; then
    echo "Usage: $0 <ARCH> <DIST> <JAVA_VERSION>"
    echo "  ARCH: Architecture to build for (amd64, arm64, ppc64el)"
    echo "  DIST: Ubuntu release for base image (focal, jammy)"
    echo "  JAVA_VERSION: Java version (11)"
    exit 1
fi;

ARCH=$1
DIST=$2
JAVA_VERSION=$3

PROJECT_DIR="$(realpath "$(dirname "$0")/../")"
SOURCES_LIST="$PROJECT_DIR/resources/sources_${DIST}_${ARCH}.list"

NATIVEARCH=$(dpkg --print-architecture)

cp "$PROJECT_DIR/resources/sources.list.template" "$SOURCES_LIST"
UBUNTU_MIRROR_AMD64=${UBUNTU_MIRROR_AMD64:-"http://archive.ubuntu.com/ubuntu/"}
sed -i "s@_MIRROR_@$UBUNTU_MIRROR_AMD64@g" "$SOURCES_LIST"
sed -i "s@_DIST_@$DIST@g" "$SOURCES_LIST"
sed -i "s@_ARCH_@amd64@g" "$SOURCES_LIST"

if [[ "$NATIVEARCH" != "amd64" ]]; then
    cat "$PROJECT_DIR/resources/sources.list.template" >> "$SOURCES_LIST"
    UBUNTU_MIRROR_PORTS=${UBUNTU_MIRROR_PORTS:-"http://ports.ubuntu.com/ubuntu-ports/"}
    sed -i "s@_MIRROR_@$UBUNTU_MIRROR_PORTS@g" "$SOURCES_LIST"
    sed -i "s@_DIST_@$DIST@g" "$SOURCES_LIST"
    sed -i "s@_ARCH_@$NATIVEARCH@g" "$SOURCES_LIST"
fi;

docker build "$PROJECT_DIR/resources" --build-arg DIST="$DIST" --build-arg ARCH="$ARCH" --build-arg JAVA_VERSION=$JAVA_VERSION -t "jitsi-srtp-$DIST-$ARCH"
docker run --rm -v "$PROJECT_DIR":/build "jitsi-srtp-$DIST-$ARCH" /build/resources/ubuntu-build.sh $ARCH $JAVA_VERSION /build
