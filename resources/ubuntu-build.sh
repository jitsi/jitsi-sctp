#!/usr/bin/env bash
if [ "$#" -ne 3 ]; then
    echo "Usage: $0 <ARCH> <JAVA_VERSION> <DIR>"
    echo "  ARCH: Architecture to build for (amd64, arm64, ppc64el)"
    echo "  JAVA_VERSION: Java version (11)"
    echo "  DIR: sctp4j project directory"
    exit 1
fi;

USRSCTPPATH=usrsctp/usrsctp
JNIPATH=jniwrapper/native/src
JAVAHPATH=jniwrapper/java/target/native/javah
OUTPATH=jniwrapper/native/target/classes/lib
RESOURCESPATH=jniwrapper/native/src/main/resources/lib

set -e

ARCH=$1
JAVA_VERSION=$2
DIR="$3"

cd "$DIR"

case $ARCH in
    amd64)
	GNUARCH=x86_64
	MACHINEARCH=x86_64
	JAVAARCH=x86-64
	;;
    arm64)
	GNUARCH=aarch64
	MACHINEARCH=aarch64
	JAVAARCH=aarch64
	;;
    ppc64el)
	GNUARCH=powerpc64le
	MACHINEARCH=ppc64le
	JAVAARCH=ppc64el
	;;
    *)
	echo "ERROR: Unsupported arch $ARCH!"
	exit 1
	;;
esac

if [ $MACHINEARCH != $(uname -m) ]
then
    PREFIX=$GNUARCH-linux-gnu
    export CC=$PREFIX-gcc
else
    export CC=gcc
fi


cd $USRSCTPPATH
./bootstrap

OBJ_DIR=obj-$ARCH
rm -rf $OBJ_DIR
mkdir $OBJ_DIR
cd $OBJ_DIR
../configure --with-pic --enable-invariants
make

export JAVA_HOME=/usr/lib/jvm/java-$JAVA_VERSION-openjdk-$ARCH
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

cp $OUTPATH/$SO_DIR/libjnisctp.so $RESOURCESPATH/$SO_DIR/
