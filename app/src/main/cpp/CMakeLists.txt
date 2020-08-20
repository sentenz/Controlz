# Sets the minimum version of CMake required to build your native library.
# This ensures that a certain set of CMake features is available to your build.

cmake_minimum_required(VERSION 3.4.1)

# configure import libs
set(distribution_DIR ${CMAKE_CURRENT_SOURCE_DIR}/../../../../distribution)

# Lib open62541
# ${ANDROID_ABI} is handy for our purpose here. Probably this ${ANDROID_ABI} is
# the most valuable thing of this sample, the rest are pretty much normal cmake
add_library(lib_open62541 STATIC IMPORTED)
set_target_properties(lib_open62541 PROPERTIES IMPORTED_LOCATION
        ${distribution_DIR}/open62541/lib/${ANDROID_ABI}/libopen62541.a)

# Lib gmath
add_library(lib_gmath STATIC IMPORTED)
set_target_properties(lib_gmath PROPERTIES IMPORTED_LOCATION
        ${distribution_DIR}/gmath/lib/${ANDROID_ABI}/libgmath.a)

# Lib gperf
add_library(lib_gperf SHARED IMPORTED)
set_target_properties(lib_gperf PROPERTIES IMPORTED_LOCATION
        ${distribution_DIR}/gperf/lib/${ANDROID_ABI}/libgperf.so)

# build application's shared lib
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -std=c++11 -Werror -fvisibility=hidden -DUA_ARCHITECTURE_POSIX")
#set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -std=gnu99 -DUA_ARCHITECTURE_POSIX")

# Specifies a library name, specifies whether the library is STATIC or
# SHARED, and provides relative paths to the source code. You can
# define multiple libraries by adding multiple add_library() commands,
# and CMake builds them for you. When you build your app, Gradle
# automatically packages shared libraries with your APK.

add_library(native-lib SHARED native-lib.cpp)

target_include_directories(native-lib PRIVATE
        ${distribution_DIR}/gmath/include
        ${distribution_DIR}/gperf/include
        ${distribution_DIR}/open62541/include)

find_library(
        # Defines the name of the path variable that stores the
        # location of the NDK library.
        log-lib
        # Specifies the name of the NDK library that
        # CMake needs to locate.
        log )

# Links your native library against one or more other native libraries.
target_link_libraries(
        # Specifies the target library.
        native-lib
        android
        lib_gmath
        lib_gperf
        lib_open62541
        # Links the log library to the target library.
        ${log-lib} )