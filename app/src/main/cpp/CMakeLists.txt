# CMakeLists.txt for Local AI C++ Native Engine
cmake_minimum_required(VERSION 3.22.1)

project("local_ai_cpp")

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# Source files for the native C++ inference bridge
add_library(
    local_ai_cpp
    SHARED
    local_ai_engine.cpp
)

# Find Android log library
find_library(
    log-lib
    log
)

# Target link libraries
target_link_libraries(
    local_ai_cpp
    ${log-lib}
)
