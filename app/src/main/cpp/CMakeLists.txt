cmake_minimum_required(VERSION 3.22.1)
project(cosmium_native)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

add_library(cosmium_native SHARED
    cosmium_jni.cpp
    cosmium_renderer.cpp
    pasa_dice_renderer.cpp
    native_engines.cpp
    CosmiumTtsEngine.cpp
)

find_library(log-lib log)
find_library(glesv3-lib GLESv3)
find_library(android-lib android)
find_library(z-lib z)

target_include_directories(cosmium_native PRIVATE
    ${CMAKE_CURRENT_SOURCE_DIR}
)

target_link_libraries(cosmium_native
    ${log-lib}
    ${glesv3-lib}
    ${android-lib}
    ${z-lib}
)
