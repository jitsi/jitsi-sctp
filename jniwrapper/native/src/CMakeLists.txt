project(jni-sctp-wrapper C)
cmake_minimum_required(VERSION 3.0)


#################################################
# CMAKE CONFIGURABLE VARIABLES
################################################

# Release with debug info build type as default
if (NOT CMAKE_BUILD_TYPE)
	message(STATUS "No build type selected, using RelWithDebInfo")
	set(CMAKE_BUILD_TYPE "RelWithDebInfo")
endif ()

set(CMAKE_C_STANDARD 11)

set(JNISCTP_JNI_HEADERS_DIR "${JNISCTP_JNI_HEADERS_DIR}"
    CACHE PATH "Path to directory with javah generated headers")

set(JNISCTP_USRSCTP_DIR "${JNISCTP_USRSCTP_DIR}"
    CACHE PATH "Path to pre-built usrsctp library (directory with lib and include subdirs)")

message(STATUS "usrsctp directory (JNISCTP_USRSCTP_DIR): `${JNISCTP_USRSCTP_DIR}`")
message(STATUS "javah headers directory (JNISCTP_JNI_HEADERS_DIR): `${JNISCTP_JNI_HEADERS_DIR}`")


#################################################
# LOAD EXTERNAL LIBRARIES INFO
#################################################

find_package(JNI)
find_package(Threads)
find_library(USRSCTP_LIB usrsctp "${JNISCTP_USRSCTP_DIR}/lib")


#################################################
# COMPILER SETTINGS
#################################################

# SETTINGS FOR UNIX COMPILER
if (CMAKE_C_COMPILER_ID MATCHES "Clang" OR CMAKE_C_COMPILER_ID MATCHES "AppleClang" OR CMAKE_C_COMPILER_ID MATCHES "GNU")
	set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -pedantic -Wall -Wextra")
endif ()

# SETTINGS FOR VISUAL STUDIO COMPILER
if (CMAKE_C_COMPILER_ID MATCHES "MSVC")
	set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} /Wall")
endif ()

message(STATUS "Compiler flags (CMAKE_C_FLAGS): ${CMAKE_C_FLAGS}")


#################################################
# LIBRARY NAMING CONFIGURATION
#################################################

if (APPLE)
    set(CMAKE_SHARED_LIBRARY_SUFFIX_C ".jnilib")
endif ()

#################################################
# TARGETS DEFINITION
#################################################

list(APPEND usrsctp_jni_sources
	org_jitsi_modified_sctp4j_SctpJni.c
)

list(APPEND usrsctp_jni_dependencies
    ${USRSCTP_LIB}
    ${CMAKE_THREAD_LIBS_INIT}
)

if (WIN32)
    list(APPEND usrsctp_jni_dependencies
        ws2_32
    )
endif ()

set(JNI_TARGET_NAME jnisctp)

add_library(${JNI_TARGET_NAME} SHARED
    ${usrsctp_jni_sources}
)

target_link_libraries(${JNI_TARGET_NAME} PRIVATE
    ${usrsctp_jni_dependencies}
)

target_include_directories(${JNI_TARGET_NAME} PRIVATE
    ${JNI_INCLUDE_DIRS}
    ${JNISCTP_JNI_HEADERS_DIR}
    "${JNISCTP_USRSCTP_DIR}/include"
)


#################################################
# INSTALL LIBRARY
#################################################
install(TARGETS ${JNI_TARGET_NAME}
        RUNTIME
            DESTINATION ${CMAKE_INSTALL_PREFIX}
        LIBRARY
            DESTINATION ${CMAKE_INSTALL_PREFIX})

if (WIN32)
    install(FILES $<TARGET_PDB_FILE:${JNI_TARGET_NAME}>
            DESTINATION ${CMAKE_INSTALL_PREFIX} OPTIONAL)
endif ()