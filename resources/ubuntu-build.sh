#!/usr/bin/env bash

set -x
set -e

USRSCTPPATH=usrsctp
JNIPATH=src/main/native
JAVAHPATH=target/native/javah
OUTPATH=target/classes/lib
RESOURCESPATH=src/main/resources/lib

if [ "$#" -ne 3 ]; then
    echo "Usage: $0 <ARCH> <JAVA_VERSION> <DIR>"
    echo "  ARCH: Architecture to build for (amd64, arm64, ppc64el)"
    echo "  JAVA_VERSION: Java version (11)"
    echo "  DIR: sctp4j project directory"
    exit 1
fi

DEBARCH=$1
JAVA_VERSION=$2
DIR="$3"

cd "$DIR"

case $DEBARCH in
    amd64)
	GNUARCH=x86_64
	JAVAARCH=amd64
	JNAARCH=x86-64
	;;
    arm64)
	GNUARCH=aarch64
	JAVAARCH=aarch64
	JNAARCH=aarch64
	;;
    ppc64el)
	GNUARCH=powerpc64le
	JAVAARCH=ppc64el
	JNAARCH=ppc64le
	;;
    *)
	echo "ERROR: Unsupported arch $DEBARCH!"
	exit 1
	;;
esac

NATIVEDEBARCH=$(dpkg --print-architecture)

if [ $DEBARCH != $NATIVEDEBARCH ]
then
    PREFIX=$GNUARCH-linux-gnu
    CONFIGURE_ARGS="--host $PREFIX"
    export CC=$PREFIX-gcc
else
    export CC=gcc
fi

NCPU=$(grep -c processor /proc/cpuinfo)
if [ -n "$NCPU" -a "$NCPU" -gt 1 ]
then
    MAKE_ARGS="-j $NCPU"
fi

cd $USRSCTPPATH
./bootstrap

OBJ_DIR=obj-$DEBARCH
rm -rf $OBJ_DIR
mkdir $OBJ_DIR
cd $OBJ_DIR
../configure --with-pic --enable-invariants $CONFIGURE_ARGS
make $MAKE_ARGS

export JAVA_HOME=/usr/lib/jvm/java-$JAVA_VERSION-openjdk-$NATIVEDEBARCH
echo $JAVA_HOME
java -version

cd "$DIR"/$OUTPATH
SO_DIR=linux-$JAVAARCH
rm -rf $SO_DIR
mkdir $SO_DIR
cd $DIR
$CC -c -g -fPIC -std=c99 -O2 -Wall \
    -I$JAVA_HOME/include -I$JAVA_HOME/include/linux \
    -I$DIR/$JAVAHPATH -I$DIR/$USRSCTPPATH/usrsctplib \
    $JNIPATH/org_jitsi_modified_sctp4j_SctpJni.c \
    -o $OUTPATH/$SO_DIR/org_jitsi_modified_sctp4j_SctpJni.o

$CC -shared -L$DIR/$USRSCTPPATH/$OBJ_DIR/usrsctplib/.libs \
    $OUTPATH/$SO_DIR/org_jitsi_modified_sctp4j_SctpJni.o \
    -Wl,-Bstatic -lusrsctp -Wl,-Bdynamic -pthread \
    -o $OUTPATH/$SO_DIR/libjnisctp.so

mkdir -p $RESOURCESPATH/$SO_DIR/

cp $OUTPATH/$SO_DIR/libjnisctp.so $RESOURCESPATH/$SO_DIR/
