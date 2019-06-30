cmake_minimum_required(VERSION 3.0)
project(joycon C)

set(CMAKE_C_STANDARD 99)

link_libraries("android" "log")
aux_source_directory(${CMAKE_CURRENT_SOURCE_DIR} src)
aux_source_directory(${CMAKE_CURRENT_SOURCE_DIR}/jni jni)
add_library(${PROJECT_NAME} SHARED ${src} ${jni})