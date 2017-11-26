# Sets the minimum version of CMake required to build the native
# library. You should either keep the default value or only pass a
# value of 3.4.0 or lower.

cmake_minimum_required(VERSION 3.4.1)

# Creates and names a library, sets it as either STATIC
# or SHARED, and provides the relative paths to its source code.
# You can define multiple libraries, and CMake builds it for you.
# Gradle automatically packages shared libraries with your APK.

add_library( # Sets the name of the library.
             ffmpeg_box

             # Sets the library as a shared library.
             SHARED

             # Provides a relative path to your source file(s).
             # Associated headers in the same location as their source
             # file are automatically included.
             src/main/cpp/ffmpeg_box.cpp src/main/cpp/cmdutils.c src/main/cpp/ffmpeg.c src/main/cpp/ffmpeg_filter.c src/main/cpp/ffmpeg_opt.c
             )

# Searches for a specified prebuilt library and stores the path as a
# variable. Because system libraries are included in the search path by
# default, you only need to specify the name of the public NDK library
# you want to add. CMake verifies that the library exists before
# completing its build.


add_library( avutil-55
             SHARED
             IMPORTED )
set_target_properties( avutil-55
                       PROPERTIES IMPORTED_LOCATION
                       ../../../../libs/armeabi-v7a/libavutil-55.so )

add_library( swresample-2
             SHARED
             IMPORTED )
set_target_properties( swresample-2
                       PROPERTIES IMPORTED_LOCATION
                       ../../../../libs/armeabi-v7a/libswresample-2.so )

add_library( avcodec-57
             SHARED
             IMPORTED )
set_target_properties( avcodec-57
                       PROPERTIES IMPORTED_LOCATION
                       ../../../../libs/armeabi-v7a/libavcodec-57.so )

add_library( avfilter-6
             SHARED
             IMPORTED )
set_target_properties( avfilter-6
                       PROPERTIES IMPORTED_LOCATION
                       ../../../../libs/armeabi-v7a/libavfilter-6.so )

add_library( swscale-4
             SHARED
             IMPORTED )
set_target_properties( swscale-4
                       PROPERTIES IMPORTED_LOCATION
                       ../../../../libs/armeabi-v7a/libswscale-4.so )

add_library( avdevice-57
             SHARED
             IMPORTED )
set_target_properties( avdevice-57
                       PROPERTIES IMPORTED_LOCATION
                       ../../../../libs/armeabi-v7a/libavdevice-57.so )

add_library( avformat-57
             SHARED
             IMPORTED )
set_target_properties( avformat-57
                       PROPERTIES IMPORTED_LOCATION
                       ../../../../libs/armeabi-v7a/libavformat-57.so )

add_library( postproc-54
             SHARED
             IMPORTED )
set_target_properties( postproc-54
                       PROPERTIES IMPORTED_LOCATION
                       ../../../../libs/armeabi-v7a/libpostproc-54.so )

add_library( libmp3lame
             SHARED
             IMPORTED )
set_target_properties( libmp3lame
                       PROPERTIES IMPORTED_LOCATION
                       ../../../../libs/armeabi-v7a/libmp3lame.so )

include_directories( libs/include )


find_library( # Sets the name of the path variable.
              log-lib

              # Specifies the name of the NDK library that
              # you want CMake to locate.
              log )

# Specifies libraries CMake should link to your target library. You
# can link multiple libraries, such as libraries you define in the
# build script, prebuilt third-party libraries, or system libraries.

target_link_libraries( # Specifies the target library.
                       ffmpeg_box postproc-54 avformat-57 avdevice-57 swscale-4 avfilter-6 avcodec-57 swresample-2 avutil-55 libmp3lame

                       # Links the target library to the log library
                       # included in the NDK.
                       ${log-lib} )
