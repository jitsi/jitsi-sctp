ARG DIST=focal

FROM ubuntu:$DIST

ARG DIST
ARG ARCH=amd64
ARG JAVA_VERSION=11

COPY sources_${DIST}_${ARCH}.list /etc/apt/sources.list

COPY --chmod=755 ubuntu-packages.sh /opt/
RUN /opt/ubuntu-packages.sh $ARCH $JAVA_VERSION
