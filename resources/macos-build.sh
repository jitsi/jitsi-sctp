#!/usr/bin/env bash

set -x
set -e

USRSCTPPATH=usrsctp/usrsctp
JNIPATH=jniwrapper/native/src
JAVAHPATH=jniwrapper/java/target/native/javah
OUTPATH=jniwrapper/native/target/classes/lib
RESOURCESPATH=jniwrapper/native/src/main/resources/lib

#!/usr/bin/env bash
if [ "$#" -ne 3 ]; then
    echo "Usage: $0 <JAVA_HOME> <ARCH> <DIR>"
    echo "  JAVA_HOME: Path to Java installation"
    echo "  ARCH: Architecture to build for (x86_64 or arm64)"
    echo "  DIR: sctp4j project directory"
    exit 1
fi

JAVA_HOME=$1
ARCH=$2
DIR="$3"

cd "$DIR"

case $ARCH in
    amd64|x86-64|x86_64)
	CLANGARCH=x86_64
	JAVAARCH=amd64
	JNAARCH=x86-64
	;;
    arm64|aarch64)
	CLANGARCH=arm64
	JAVAARCH=aarch64
	JNAARCH=aarch64
	;;
    *)
	echo "ERROR: Unsupported arch $ARCH!"
	exit 1
	;;
esac

NATIVEARCH=$(uname -m)

export CC="clang -arch $CLANGARCH"

if [ $CLANGARCH = arm64 -a $NATIVEARCH != arm64 ]
then
    PREFIX=aarch64-apple-darwin
    CONFIGURE_ARGS="--host $PREFIX"
fi

NCPU=$(sysctl -n hw.ncpu)
if [ -n "$NCPU" -a "$NCPU" -gt 1 ]
then
    MAKE_ARGS="-j $NCPU"
fi

cd $USRSCTPPATH
./bootstrap

OBJ_DIR=obj-$CLANGARCH
rm -rf $OBJ_DIR
mkdir $OBJ_DIR
cd $OBJ_DIR
../configure --with-pic --enable-invariants $CONFIGURE_ARGS
make $MAKE_ARGS

echo $JAVA_HOME
java -version

cd "$DIR"/$OUTPATH
DYLIB_DIR=darwin-$JNAARCH
rm -rf $DYLIB_DIR
mkdir $DYLIB_DIR
cd $DIR
$CC -c -g -fPIC -std=c99 -O2 -Wall \
    -I$JAVA_HOME/include -I$JAVA_HOME/include/darwin \
    -I$DIR/$JAVAHPATH -I$DIR/$USRSCTPPATH/usrsctplib \
    $JNIPATH/org_jitsi_modified_sctp4j_SctpJni.c \
    -o $OUTPATH/$DYLIB_DIR/org_jitsi_modified_sctp4j_SctpJni.o

$CC -shared \
    $OUTPATH/$DYLIB_DIR/org_jitsi_modified_sctp4j_SctpJni.o \
    $DIR/$USRSCTPPATH/$OBJ_DIR/usrsctplib/.libs/libusrsctp.a \
    -o $OUTPATH/$DYLIB_DIR/libjnisctp.dylib

mkdir -p $RESOURCESPATH/$DYLIB_DIR/

cp $OUTPATH/$DYLIB_DIR/libjnisctp.dylib $RESOURCESPATH/$DYLIB_DIR/
