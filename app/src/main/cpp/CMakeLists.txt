cmake_minimum_required(VERSION 3.22.1)
project(cosmium_native)

add_library(cosmium_native SHARED
    cosmium_jni.cpp
    cosmium_renderer.cpp
    pasa_dice_renderer.cpp
    native_engines.cpp
)

find_library(log-lib log)
find_library(glesv3-lib GLESv3)

target_link_libraries(cosmium_native
    ${log-lib}
    ${glesv3-lib}
)
