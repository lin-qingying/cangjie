# the minimum version of CMake.
cmake_minimum_required(VERSION 3.5.0)
project(myNpmLib)

set(NATIVERENDER_ROOT_PATH <#noparse>${CMAKE_CURRENT_SOURCE_DIR}</#noparse>)

if(DEFINED PACKAGE_FIND_FILE)
    include(<#noparse>${PACKAGE_FIND_FILE}</#noparse>)
endif()

include_directories(<#noparse>${NATIVERENDER_ROOT_PATH}
                    ${NATIVERENDER_ROOT_PATH}/include</#noparse>)

add_library(${moduleName?lower_case} SHARED napi_init.cpp)
target_link_libraries(${moduleName?lower_case} PUBLIC libace_napi.z.so)
