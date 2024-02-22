# jitsi-sctp
The jitsi-sctp project creates a JNI wrapper around the usrsctp lib and provides a set of Java classes to further flesh out a convenient Java SCTP API.

### Building the jar files
* Clone the project
* Run `mvn package` (and `mvn install` to install locally)

This will install all the jars built by the project.  Depend on the `sctp` module to use jitsi-sctp in your code.

## Building with Java changes only

To avoid having to build all native libraries,
execute `resources/fetch-maven.sh` to download and extract the native binaries
from the latest release on Maven Central.

**TODO**


### (Re)Building a new JNI lib
The JNI lib will need to be rebuilt if there is a change in the usrsctp version or a change in the JNI wrapper C file.

**Please take a look at the GitHub Actions build before asking for more detailed
build instructions!**

### Ubuntu

Prerequisites:

- OpenJDK 11 (or newer)
- Maven
- Docker (including the docker-buildx plugin)
- Autoconf, automake, and libtool (for usrsctp's configure process).

* Clone the project.
* Update the usrsctp subproject with
```
jitsi-sctp> git submodule update
```
* Build the JNI headers
```
jitsi-sctp> mvn compile
```
* Build the libraries
```
jitsi-sctp> resources/ubuntu-build-all.sh
```
### macOS

Prerequisites:

- OpenJDK 11 (or newer)
- XCode
- Maven
- Autoconf, automake, and libtool (for usrsctp's configure process).

* Clone the project.
* Update the usrsctp subproject with
```
jitsi-sctp> git submodule update
```
* Build the JNI headers
```
jitsi-sctp> mvn compile
```
* Build the libraries
```
jitsi-sctp> resources/macos-build-all.sh
```

* Compile and install
```
jitsi-sctp> mvn install -DbuildSctp -DbuildNativeWrapper -DdeployNewJnilib
```
* Note: The above commands must be run separately right now due to a bug in the maven-native-plugin.  Once a new release is done which includes [this fix](https://github.com/mojohaus/maven-native/pull/27) we'll be able to just run `mvn package install -DbuildSctp -DbuildNativeWrapper -DdeployNewJnilib`.
