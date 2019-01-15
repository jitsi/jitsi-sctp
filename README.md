# Sctp4j
The Sctp4j project creates a JNI wrapper around the usrsctp lib and provides a set of Java classes to further flesh out a convenient Java SCTP API.

## Project organization
Because JNI has a complex build process, this project has multiple submodules to try and separate each of the phases necessary from start to finish.  The maven modules are laid out as follows:
```
`-- sctp4j
    |-- jniwrapper
    |   |-- java
    |   |-- jnilib
    |   `-- native
    |-- sctp
    `-- usrsctp
```
* The `usrsctp` submodule handles the compilation of the usrsctp source into a library.  Once the usrsctp source has been cloned to the proper place (into the src/native/usrsctp directory of this submodule), the `compile` lifecycle can be used to bootstrap, configure and make the source into a library.  `package` will create a jar that will include the native library and the necessary include headers.
* The `jniwrapper` submodule has 3 nested submodules:
  * The `jniwrapper-java` submodule includes the Java portion of the JNI API and interacts with the native code.
  * The `jniwrapper-native` submodule contains the C portion of the JNI API that bridges the Java and the usrsctp native lib.  It depends on both the `usrsctp` module (because it needs the built library and include headers) and the `jniwrapper-java` module (to generate the header file to match the C implementation from the Java file).  The `compile` lifecycle will create a new jni lib in target/.  The `package` lifecycle will create a jar that includes the java code and the native libraries. ***NOTE***: The `package` lifecycle does NOT use the created JNI lib from the target directory.  It is intended that the JNI libs are built for each platform ahead of time, and are placed in the `src/main/resources` directory.  This directory is where the `package` lifecycle will include libraries from.
  * The `jnilib` submodule combines the `jniwrapper-java` and `jniwrapper-native` submodules into a fat jar which includes the Java API and the native JNI libraries that will need to be loaded at runtime.
* The `sctp` submodule contains the library on top of the JNI code.  The jar built by this is what is intended to be used by other code.

### Building the jar files
* Clone the project
* Run `mvn package` (and `mvn install` to install locally)

This will install all the jars built by the project.  Depend on the `sctp` module to use sctp4j in your code.

### (Re)Building a new JNI lib
The JNI lib will need to be rebuilt if there is a change in the usrsctp version or a change in the JNI wrapper C file.

* Clone the project
* Clone the usrsctp src:
```
sctp4j/usrsctp> git clone https://github.com/sctplab/usrsctp
(check out whatever hash/version you want)
```
* Build the usrsctp submodule
```
sctp4j> mvn -pl usrsctp -DbuildSctp clean compile install
```
* Compile the JNI Java wrapper
```
sctp4j> mvn -pl "jniwrapper/java" clean compile install
```
* Compile the JNI C wrapper
```
sctp4j> mvn -pl "jniwrapper/native" -DbuildNativeWrapper clean compile
```
* Copy the built JNI lib into the prebuilt folder, for example on Linux:
```
sctp4j> cp jniwrapper/native/target/libjnisctp-linux-amd64.jnilib src/main/resources/lib/linux/libjnisctp.jnilib
```
* Once the lib has been put in the prebuilt directory, you can just re-run the top level `mvn package install` and it will use the new lib
