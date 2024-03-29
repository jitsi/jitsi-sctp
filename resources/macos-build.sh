#!/usr/bin/env bash

set -x
set -e

USRSCTPPATH=usrsctp
JNIPATH=src/main/native
JAVAHPATH=target/native/javah
OUTPATH=target/classes/lib
RESOURCESPATH=src/main/resources

#!/usr/bin/env bash
if [ "$#" -lt 2 -o "$#" -gt 3 ]; then
    echo "Usage: $0 <JAVA_HOME> <ARCH> [<DIR>]"
    echo "  JAVA_HOME: Path to Java installation"
    echo "  ARCH: Architecture to build for (x86_64 or arm64)"
    echo "  DIR: sctp4j project directory"
    exit 1
fi

JAVA_HOME=$1
ARCH=$2

if [ "$#" -eq 3 ]; then
    DIR="$3"
else
    DIR="$(realpath "$(dirname "$0")/../")"
fi

cd "$DIR"

if [ \! -r $JAVAHPATH/org_jitsi_modified_sctp4j_SctpJni.h ]; then
    echo "$JAVAHPATH/org_jitsi_modified_sctp4j_SctpJni.h not found: did you run mvn compile?"
    exit 1
fi

case $ARCH in
    amd64|x86-64|x86_64)
	CLANGARCH=x86_64
	JNAARCH=x86-64
	;;
    arm64|aarch64)
	CLANGARCH=arm64
	JNAARCH=aarch64
	;;
    *)
	echo "ERROR: Unsupported arch $ARCH!"
	exit 1
	;;
esac

NATIVEARCH=$(uname -m)

export CC="xcrun clang -arch $CLANGARCH"

if [ $CLANGARCH = arm64 -a $NATIVEARCH != arm64 ]
then
    HOST=aarch64-apple-darwin
    CONFIGURE_ARGS="--host $HOST"
fi

NCPU=$(sysctl -n hw.ncpu)
if [ -n "$NCPU" -a "$NCPU" -gt 1 ]
then
    MAKE_ARGS="-j $NCPU"
fi

cd $USRSCTPPATH
./bootstrap

if [ -r Makefile ]; then
    make distclean
fi

OBJ_DIR=obj-$CLANGARCH
INSTALL_DIR=$DIR/$USRSCTPPATH/install-$CLANGARCH
rm -rf $OBJ_DIR $INSTALL_DIR
mkdir $OBJ_DIR
cd $OBJ_DIR
../configure --with-pic --enable-invariants --prefix=$INSTALL_DIR $CONFIGURE_ARGS
make $MAKE_ARGS install

echo $JAVA_HOME
java -version

cd "$DIR"
mkdir -p $OUTPATH
cd $OUTPATH
DYLIB_DIR=darwin-$JNAARCH
rm -rf $DYLIB_DIR
mkdir $DYLIB_DIR
cd $DIR
$CC -c -g -fPIC -std=c99 -O2 -Wall \
    -I$JAVA_HOME/include -I$JAVA_HOME/include/darwin \
    -I$DIR/$JAVAHPATH -I$INSTALL_DIR/include \
    $JNIPATH/org_jitsi_modified_sctp4j_SctpJni.c \
    -o $OUTPATH/$DYLIB_DIR/org_jitsi_modified_sctp4j_SctpJni.o

$CC -shared \
    $OUTPATH/$DYLIB_DIR/org_jitsi_modified_sctp4j_SctpJni.o \
    $INSTALL_DIR/lib/libusrsctp.a \
    -o $OUTPATH/$DYLIB_DIR/libjnisctp.dylib

mkdir -p $RESOURCESPATH/$DYLIB_DIR/

cp $OUTPATH/$DYLIB_DIR/libjnisctp.dylib $RESOURCESPATH/$DYLIB_DIR/
