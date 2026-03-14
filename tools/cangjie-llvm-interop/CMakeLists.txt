cmake_minimum_required(VERSION 3.20)
project(cangjie_llvm_interop LANGUAGES C CXX)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_CXX_EXTENSIONS OFF)

option(CANGJIE_USE_OFFICIAL_LLVM "Bootstrap and use official LLVM source for this build." OFF)
set(CANGJIE_LLVM_REPOSITORY "https://gitcode.com/Cangjie/llvm-project.git" CACHE STRING "Official LLVM repository used by Cangjie compiler.")
set(CANGJIE_LLVM_TAG "main" CACHE STRING "Official LLVM branch/tag aligned with cangjie compiler.")
set(CANGJIE_LLVM_TARGETS "ARM;AArch64;X86" CACHE STRING "LLVM targets to build when bootstrapping official LLVM.")
set(CANGJIE_LLVM_SOURCE_DIR "" CACHE PATH "Optional pre-downloaded official llvm-project source directory.")
set(CANGJIE_LLVM_INSTALL_DIR "${CMAKE_BINARY_DIR}/official-llvm-install" CACHE PATH "Install prefix for bootstrapped official LLVM.")
set(CANGJIE_LLVM_BUILD_DIR "${CMAKE_BINARY_DIR}/official-llvm-build" CACHE PATH "Build directory for bootstrapped official LLVM.")

if(CMAKE_SYSTEM_NAME STREQUAL "Windows")
    set(CANGJIE_TARGET_OS "windows")
elseif(CMAKE_SYSTEM_NAME STREQUAL "Darwin")
    set(CANGJIE_TARGET_OS "macos")
else()
    set(CANGJIE_TARGET_OS "linux")
endif()

if(CMAKE_SYSTEM_PROCESSOR MATCHES "aarch64|arm64|ARM64")
    set(CANGJIE_TARGET_ARCH "arm64")
elseif(CMAKE_SYSTEM_PROCESSOR MATCHES "armv7|arm")
    set(CANGJIE_TARGET_ARCH "arm32")
elseif(CMAKE_SYSTEM_PROCESSOR MATCHES "x86_64|AMD64|amd64")
    set(CANGJIE_TARGET_ARCH "amd64")
elseif(CMAKE_SYSTEM_PROCESSOR MATCHES "i686|i386|x86")
    set(CANGJIE_TARGET_ARCH "x86")
else()
    string(TOLOWER "${CMAKE_SYSTEM_PROCESSOR}" CANGJIE_TARGET_ARCH)
endif()

set(CANGJIE_OUTPUT_SUFFIX "${CANGJIE_TARGET_OS}_${CANGJIE_TARGET_ARCH}")

set(CANGJIE_EXPECTED_LLVM_DIR "${CANGJIE_LLVM_INSTALL_DIR}/lib/cmake/llvm")
if(CANGJIE_USE_OFFICIAL_LLVM)
    if(LLVM_DIR AND NOT LLVM_DIR STREQUAL CANGJIE_EXPECTED_LLVM_DIR)
        message(WARNING "Ignoring externally provided LLVM_DIR='${LLVM_DIR}'. Using bootstrapped official LLVM from ${CANGJIE_LLVM_REPOSITORY} instead.")
    endif()

    include(cmake/BootstrapOfficialLlvm.cmake)
    cangjie_bootstrap_official_llvm(
        REPOSITORY "${CANGJIE_LLVM_REPOSITORY}"
        TAG "${CANGJIE_LLVM_TAG}"
        SOURCE_DIR "${CANGJIE_LLVM_SOURCE_DIR}"
        BUILD_DIR "${CANGJIE_LLVM_BUILD_DIR}"
        INSTALL_DIR "${CANGJIE_LLVM_INSTALL_DIR}"
        TARGETS "${CANGJIE_LLVM_TARGETS}"
    )
    set(LLVM_DIR "${CANGJIE_EXPECTED_LLVM_DIR}" CACHE PATH "LLVMConfig.cmake directory" FORCE)
elseif(NOT LLVM_DIR)
    message(FATAL_ERROR "LLVM_DIR is required when CANGJIE_USE_OFFICIAL_LLVM=OFF.")
endif()

find_package(LLVM CONFIG REQUIRED)

message(STATUS "Found LLVM ${LLVM_PACKAGE_VERSION}")
message(STATUS "Using LLVMConfig.cmake in: ${LLVM_DIR}")
message(STATUS "Target suffix: ${CANGJIE_OUTPUT_SUFFIX}")

set(CANGJIE_EXECUTABLE_NAME "cangjie-llvm-interop_${CANGJIE_OUTPUT_SUFFIX}")
add_executable(${CANGJIE_EXECUTABLE_NAME}
    src/main.cpp
)

target_include_directories(${CANGJIE_EXECUTABLE_NAME} PRIVATE ${LLVM_INCLUDE_DIRS})
target_compile_definitions(${CANGJIE_EXECUTABLE_NAME} PRIVATE ${LLVM_DEFINITIONS})

if(MSVC)
    target_compile_options(${CANGJIE_EXECUTABLE_NAME} PRIVATE /W4 /EHsc)
else()
    target_compile_options(${CANGJIE_EXECUTABLE_NAME} PRIVATE -Wall -Wextra -Wpedantic)
endif()

llvm_map_components_to_libnames(LLVM_LIBS
    Core
    IRReader
    BitWriter
    Support
    AsmParser
)

target_link_libraries(${CANGJIE_EXECUTABLE_NAME} PRIVATE ${LLVM_LIBS})
