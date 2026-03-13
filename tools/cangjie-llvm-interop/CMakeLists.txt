cmake_minimum_required(VERSION 3.20)
project(cangjie_llvm_interop LANGUAGES CXX)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_CXX_EXTENSIONS OFF)

option(CANGJIE_USE_OFFICIAL_LLVM "Bootstrap and use official Cangjie LLVM if LLVM_DIR is not provided." ON)
set(CANGJIE_LLVM_REPOSITORY "https://gitcode.com/openharmony/third_party_llvm-project.git" CACHE STRING "Official LLVM repository used by Cangjie compiler.")
set(CANGJIE_LLVM_TAG "master" CACHE STRING "Official LLVM branch/tag aligned with cangjie compiler.")
set(CANGJIE_LLVM_TARGETS "ARM;AArch64;X86" CACHE STRING "LLVM targets to build when bootstrapping official LLVM.")
set(CANGJIE_LLVM_SOURCE_DIR "" CACHE PATH "Optional pre-downloaded official llvm-project source directory.")
set(CANGJIE_LLVM_INSTALL_DIR "${CMAKE_BINARY_DIR}/official-llvm-install" CACHE PATH "Install prefix for bootstrapped official LLVM.")
set(CANGJIE_LLVM_BUILD_DIR "${CMAKE_BINARY_DIR}/official-llvm-build" CACHE PATH "Build directory for bootstrapped official LLVM.")

if(NOT LLVM_DIR AND CANGJIE_USE_OFFICIAL_LLVM)
    include(cmake/BootstrapOfficialLlvm.cmake)
    cangjie_bootstrap_official_llvm(
        REPOSITORY "${CANGJIE_LLVM_REPOSITORY}"
        TAG "${CANGJIE_LLVM_TAG}"
        SOURCE_DIR "${CANGJIE_LLVM_SOURCE_DIR}"
        BUILD_DIR "${CANGJIE_LLVM_BUILD_DIR}"
        INSTALL_DIR "${CANGJIE_LLVM_INSTALL_DIR}"
        TARGETS "${CANGJIE_LLVM_TARGETS}"
    )
    set(LLVM_DIR "${CANGJIE_LLVM_INSTALL_DIR}/lib/cmake/llvm" CACHE PATH "LLVMConfig.cmake directory" FORCE)
endif()

find_package(LLVM CONFIG REQUIRED)

message(STATUS "Found LLVM ${LLVM_PACKAGE_VERSION}")
message(STATUS "Using LLVMConfig.cmake in: ${LLVM_DIR}")

add_executable(cangjie-llvm-interop
    src/main.cpp
)

target_include_directories(cangjie-llvm-interop PRIVATE ${LLVM_INCLUDE_DIRS})
target_compile_definitions(cangjie-llvm-interop PRIVATE ${LLVM_DEFINITIONS})

if(MSVC)
    target_compile_options(cangjie-llvm-interop PRIVATE /W4 /EHsc)
else()
    target_compile_options(cangjie-llvm-interop PRIVATE -Wall -Wextra -Wpedantic)
endif()

llvm_map_components_to_libnames(LLVM_LIBS
    Core
    IRReader
    BitWriter
    Support
    AsmParser
)

target_link_libraries(cangjie-llvm-interop PRIVATE ${LLVM_LIBS})
