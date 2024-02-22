#!/usr/bin/env bash
if ! command -v unzip &> /dev/null; then
    echo "$0 requires unzip"
fi;

set -e

SETTINGS=$(mktemp)

cat > $SETTINGS <<EOF
<settings>
  <profiles>
    <profile>
    <id>jitsi-repo</id>
      <repositories>
        <repository>
          <id>jitsi-maven-repository-releases</id>
          <name>Jitsi Maven Repository (Releases)</name>
          <url>https://github.com/jitsi/jitsi-maven-repository/raw/master/releases/</url>
        </repository>
      </repositories>
    </profile>
  </profiles>
  <activeProfiles>
    <activeProfile>jitsi-repo</activeProfile>
  </activeProfiles>
</settings>
EOF

VER=3.6.1
PROJECT_DIR="$(cd "$(dirname "$0")/../"; pwd -P)"
EXTRACT_DEST="$PROJECT_DIR/target/latest-maven"

mkdir -p "$EXTRACT_DEST"
mvn -s $SETTINGS org.apache.maven.plugins:maven-dependency-plugin:$VER:copy \
    -Dartifact=org.jitsi:jitsi-sctp:LATEST:jar \
    -DoutputDirectory="$EXTRACT_DEST"

rm $SETTINGS

unzip -o "$EXTRACT_DEST/*.jar" "linux-*" "darwin-*" -d "$EXTRACT_DEST"
mkdir -p "$PROJECT_DIR/src/main/resources"
cp -r "$EXTRACT_DEST/"{darwin,linux}-* "$PROJECT_DIR/src/main/resources" || true
