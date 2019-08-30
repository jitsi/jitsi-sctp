# jitsi-sctp
The `jitsi-sctp` project creates a JNI wrapper around the [`usrsctp`](https://github.com/sctplab/usrsctp) lib and provides a set of Java classes to further flesh out a convenient Java SCTP API, which can be used on `Linux`, `MacOS X`, `FreeBSD` and `Windows`.

## Project organization
Because JNI has a complex build process multiplied by being platform dependent, this project has multiple Maven modules to try and separate each of the phases necessary from start to finish. The maven modules are laid out as follows:
```
`-- jitsi-sctp
    |-- jniwrapper
    |   |-- java
    |   |-- jnilib
    |   `-- native (produces platform specific artifact classified by OS & CPU architecture)
    |-- sctp
    `-- usrsctp (produces platform specific artifact classified by OS & CPU architecture)
```

* The `usrsctp` module handles the compilation of the [`usrsctp`](https://github.com/sctplab/usrsctp) source library.
This maven module produces a platform-specific artifact containing a platform-specific build of the `usrsctp` static library and corresponding `C` API-header.
The module is built only when the `build-usrsctp` profile is enabled by passing the `-Pbuild-usrsctp` switch to `mvn`.
Execution of 
`mvn package -Pbuild-usrsctp -f pom.xml -pl org.jitsi:usrsctp` will create a jar that will include the native library and the necessary include headers for current platform.
A resulting `jar` artifact has [maven classifier](https://maven.apache.org/pom.html) specified as a concatenation of `usrsctp` commit and target platform.
For example, an artifact for `Linux` might be named as `usrsctp-1.0-SNAPSHOT-7a8bc9a-linux-x86_64.jar` with an example content:
```text
$ tree usrsctp-1.0-SNAPSHOT-7a8bc9a-linux-x86_64 --noreport
usrsctp-1.0-SNAPSHOT-7a8bc9a-linux-x86_64
|-- META-INF
|   |-- MANIFEST.MF
|   `-- maven
|       `-- org.jitsi
|           `-- usrsctp
|               `-- pom.xml
|-- git.properties
|-- include
|   `-- usrsctp.h
`-- lib
    `-- libusrsctp.a
```

* The `jniwrapper` module has 3 nested modules:
  * The `jniwrapper-java` module includes the Java portion of the JNI API and interacts with the native code.
    The artifact produced by this module has Java classes to interact with native `usrsctp` wrapper, it also has
    necessary `JNI` `C` headers, generated from Java classes. An example of artifact content:
    ```text
    $ tree jniwrapper-java-1.0-SNAPSHOT --noreport
    jniwrapper-java-1.0-SNAPSHOT
    |-- META-INF
    |   |-- MANIFEST.MF
    |   `-- maven
    |       `-- org.jitsi
    |           `-- jniwrapper-java
    |               |-- pom.properties
    |               `-- pom.xml
    |-- cz
    |   `-- adamh
    |       `-- utils
    |           `-- NativeUtils.class
    |-- native
    |   `-- headers
    |       `-- org_jitsi_modified_sctp4j_SctpJni.h
    `-- org
        `-- jitsi_modified
            `-- sctp4j
                |-- EightArgumentVoidFunc.class
                |-- FourArgumentIntFunc.class
                |-- IncomingSctpDataHandler.class
                |-- OutgoingSctpDataHandler.class
                `-- SctpJni.class
    ```
  * The `jniwrapper-native` module contains the `C` portion of the JNI API that bridges the Java and the [`usrsctp`](https://github.com/sctplab/usrsctp) native lib.
  The module is built only when the `build-jnisctp` profile is enabled by passing the `-Pbuild-jnisctp` switch to `mvn`.
  It depends on two other modules:
    * `usrsctp` module, because it needs the pre-built `usrsctp` static library and include headers. The version of `usrsctp` artifact used is specified by property `usrsctp_commit_id` in `jniwrapper/pom.xml` and having short commit hash of pre-built `usrsctp`;
    * `jniwrapper-java` module, because it need `java -h` generated `JNI` header file to match the `C` implementation.

    The `compile` build phase of `jniwrapper-native` module will create a new dynamic jni lib in `target/jnisctp_cmake/{os}-{arch}/install`. E.g. on `Linux` it produces dynamic library `target/jnisctp_cmake/linux-x86_64/install/libjnisctp.so`.

    The `package` build phase of `jniwrapper-native` module will create a platform specific `jar` that includes the java code and the shared native library for current platform. It is assumed that `usrsctp` artifact for target platform is already available in `Maven`.
    Below is an example of artifact produced by `mvn package -Pbuild-usrsctp -Pbuild-jnisctp -f pom.xml -pl org.jitsi:jniwrapper-native -am` is presented:
      ```text
      $ tree jniwrapper-native-1.0-SNAPSHOT-linux-x86_64 --noreport
      jniwrapper-native-1.0-SNAPSHOT-linux-x86_64
      |-- META-INF
      |   |-- MANIFEST.MF
      |   `-- maven
      |       `-- org.jitsi
      |           `-- jniwrapper-native
      |               `-- pom.xml
      `-- lib
          `-- linux-x86_64
              `-- libjnisctp.so

      ```
  **Note:** It is intended that platform specific `Maven` artifacts produced by `usrsctp` and `jniwrapper-native` modules are built on each supported platform independently and published ahead of time to [Maven repository](https://github.com/jitsi/jitsi-maven-repository/) before the rest of the artifacts can be built in way which allow them to be used on any of supported platform.

  * The `jnilib` maven module combines the `jniwrapper-java` and `jniwrapper-native` into a single `jar` which includes the Java API and the native JNI library that will be loaded at runtime.
  When built with `mvn package -f pom.xml -Pbuild-usrsctp -Pbuild-jnisctp -pl org.jitsi:jnilib -am` the `jnilib` artifact only include native `jnisctp` library for current platform.
  To have universal (**fat jar** or **cross-platform jar**) `jnilib` suitable to run on any supported platform it necessary to build and publish platform-specific `jniwrapper-native` artifacts for all supported platforms in advance and then build with `build-x-plat-jar` profile enabled via passing `-Pbuild-x-plat-jar` switch into Maven. For example, fat `jnilib` jar could be built with `mvn package -Pbuild-x-plat-jar -f pom.xml -pl org.jitsi:jnilib`, which will produce fat jar with example content:
    ```
    $ tree jnilib-1.0-SNAPSHOT --noreport
    jnilib-1.0-SNAPSHOT
    |-- META-INF
    |   |-- MANIFEST.MF
    |   `-- maven
    |       `-- org.jitsi
    |           |-- jnilib
    |           |   `-- pom.xml
    |           |-- jniwrapper-java
    |           |   |-- pom.properties
    |           |   `-- pom.xml
    |           `-- jniwrapper-native
    |               `-- pom.xml
    |-- cz
    |   `-- adamh
    |       `-- utils
    |           `-- NativeUtils.class
    |-- lib
    |   |-- osx-x86_64
    |   |   `-- libjnisctp.jnilib
    |   |-- linux-x86_64
    |   |   `-- libjnisctp.so
    |   |-- freebsd-x86_64
    |   |   `-- libjnisctp.so
    |   `-- windows-x86_64
    |       |-- jnisctp.dll
    |       `-- jnisctp.pdb
    |-- native
    |   `-- headers
    |       `-- org_jitsi_modified_sctp4j_SctpJni.h
    `-- org
        `-- jitsi_modified
            `-- sctp4j
                |-- EightArgumentVoidFunc.class
                |-- FourArgumentIntFunc.class
                |-- IncomingSctpDataHandler.class
                |-- OutgoingSctpDataHandler.class
                `-- SctpJni.class
    ```


* The `sctp` module contains the Java library on top of the JNI code. 
The jar built by this is what is intended to be used by other code.

## Pre-required software to build
`CMake` is required to build native libraries. 
Native compilers `GCC`/`Clang`/`Visual C++` required to build native libraries.

### Building the jar files
* Clone the project and initialize [`usrsctp`](https://github.com/sctplab/usrsctp) git submodule.
    ```
    > git clone --recurse-submodules https://github.com/jitsi/jitsi-sctp.git jitsi-sctp
    ```
* Run `mvn install -Pbuild-usrsctp -Pbuild-jnisctp -f pom.xml` to build both `Java` and native artifacts for current platform and install them into local `Maven` repository.
If you don't want to re-build native artifacts `usrsctp` and `jniwrapper-native` and prefer using pre-built version of them from `Maven` you may build and install `Java` artifacts via `mvn install -f pom.xml`.
* Depend on the `org.jitsi:sctp` artifact to use `jitsi-sctp` in your project.

### (Re)Building a new JNI lib
The JNI lib will need to be rebuilt if there is a change in the [`usrsctp`](https://github.com/sctplab/usrsctp) version or a change in the JNI wrapper `C` file.
Changes in [`usrsctp`](https://github.com/sctplab/usrsctp) handled by re-compiling `usrsctp` artifact from corresponding Maven module.
Changes in JNI wrapper `C` code are handled by recompiling `jniwrapper-native` artifact from corresponding maven module.
To re-build native libraries cross-platform [`CMake`](https://cmake.org/) build tool, `C` compiler and linker and `JDK` must be installed on system used to build.

The following steps can be done to produce an updated version of `jitsi-sctp` artifact with newer version of `usrsctp` or `jniwrapper-native`:

1. Clone the project with `git clone --recurse-submodules https://github.com/jitsi/jitsi-sctp.git`.


2. \[Optional\] initialize the [usrsctp](https://github.com/sctplab/usrsctp) submodule with `git submodule update --init --recursive`:
    ```
    jitsi-sctp/usrsctp/usrsctp>
    (check out whatever hash/version you want in case it distinct from what is defined by git submodule)
    ```

3. Produce an updated platform specific `usrsctp` artifact 
    ```
    jitsi-sctp> mvn clean package install -Pbuild-usrsctp -f pom.xml -pl org.jitsi:usrsctp
    ```

4. \[Optional\] Update `<usrsctp_commit_id>` property in `jniwrapper/pom.xml` to specify desired version of `usrsctp` to use.

5. Produce an updated platform specific `jniwrapper-native` artifact and publish it to Maven.
    ```
    jitsi-sctp> mvn clean package install -Pbuild-jnisctp -f pom.xml -pl org.jitsi:jniwrapper-native 
    ```

6. \[Optional\] Repeat steps `1 - 5` on each of supported platforms: `Linux`, `Mac OSX`, `FreeBSD`, `Windows`.

7. Once `usrsctp` and `jniwrapper-native` artifacts built and published to [Maven repository](https://github.com/jitsi/jitsi-maven-repository/) for each supported platform (`Windows`, `Linux`, `Mac`, `FreeBSD`) with steps `1 - 6`, an updated **fat jar** could be build and installed with following command:
    ```
    jitsi-sctp> mvn clean package install -Pbuild-x-plat-jar -f pom.xml
    ```

8. To produce `jitsi-sctp` artifact usable only on current platform steps `3 - 7` can be skipped and following command could be used instead:
    ```
    jitsi-sctp> mvn clean package install -Pbuild-usrsctp -Pbuild-jnisctp -f pom.xml
    ```
