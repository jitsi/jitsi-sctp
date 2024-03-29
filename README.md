# jitsi-sctp
The jitsi-sctp project creates a JNI wrapper around the usrsctp lib and provides a set of Java classes to further flesh out a convenient Java SCTP API.

## Building with Java changes only

To avoid having to build all native libraries,
execute `resources/fetch-maven.sh` to download and extract the native binaries
from the latest release on the Jitsi Maven Repository.

## Building the native libraries
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
