#!/bin/bash
if [ "$#" -ne 2 ]; then
    echo "Usage: $0 <ARCH> <JAVA_VERSION>"
    echo "  ARCH: Architecture to build for (amd64, arm64, ppc64el)"
    echo "  JAVA_VERSION: Java version (11)"
    exit 1
fi;

ARCH=$1
JAVA_VERSION=$2

PACKAGES=()
case "$ARCH" in
  "amd64"|"x86-64"|"x86_64")
    DEBARCH=amd64
    GNUARCH=x86-64
    ;;
  "arm64"|"aarch64")
    DEBARCH=arm64
    GNUARCH=aarch64
    ;;
  "ppc64el")
    DEBARCH=ppc64el
    GNUARCH=powerpc64le
    ;;
esac

NATIVEDEBARCH=$(dpkg --print-architecture)

dpkg --add-architecture $DEBARCH

if [[ "$DEBARCH" == "$NATIVEDEBARCH" ]]; then
    PACKAGES+=(build-essential)
else
    PACKAGES+=("crossbuild-essential-$DEBARCH" "gcc-$GNUARCH-linux-gnu")
fi;

PACKAGES+=(
    "libtool" "autotools-dev" "autoconf" "automake" "m4"
    "openjdk-$JAVA_VERSION-jdk-headless")

export DEBIAN_FRONTEND=noninteractive
apt-get update && \
  apt-get install --no-install-recommends -y "${PACKAGES[@]}" && \
  rm -rf /var/lib/apt/lists/*
