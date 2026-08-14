cmake_minimum_required(VERSION 3.22.1)

project("shieldcore_native")

add_library(
        shieldcore_native
        SHARED
        native_scanner.cpp
)

find_library(
        log-lib
        log
)

target_link_libraries(
        shieldcore_native
        ${log-lib}
)
