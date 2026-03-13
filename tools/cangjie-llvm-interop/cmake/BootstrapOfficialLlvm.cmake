function(cangjie_bootstrap_official_llvm)
    include(CMakeParseArguments)
    set(options)
    set(oneValueArgs REPOSITORY TAG SOURCE_DIR BUILD_DIR INSTALL_DIR GENERATOR TARGETS)
    cmake_parse_arguments(BOOTSTRAP "${options}" "${oneValueArgs}" "" ${ARGN})

    if(NOT BOOTSTRAP_REPOSITORY)
        message(FATAL_ERROR "Bootstrap LLVM requires REPOSITORY.")
    endif()
    if(NOT BOOTSTRAP_TAG)
        message(FATAL_ERROR "Bootstrap LLVM requires TAG.")
    endif()
    if(NOT BOOTSTRAP_BUILD_DIR)
        message(FATAL_ERROR "Bootstrap LLVM requires BUILD_DIR.")
    endif()
    if(NOT BOOTSTRAP_INSTALL_DIR)
        message(FATAL_ERROR "Bootstrap LLVM requires INSTALL_DIR.")
    endif()
    if(NOT BOOTSTRAP_GENERATOR)
        set(BOOTSTRAP_GENERATOR "Ninja")
    endif()
    if(NOT BOOTSTRAP_TARGETS)
        set(BOOTSTRAP_TARGETS "ARM;AArch64;X86")
    endif()

    set(llvm_source_dir "${BOOTSTRAP_SOURCE_DIR}")
    if(NOT llvm_source_dir)
        set(llvm_source_dir "${CMAKE_BINARY_DIR}/official-llvm-src")
    endif()

    set(llvm_config_file "${BOOTSTRAP_INSTALL_DIR}/lib/cmake/llvm/LLVMConfig.cmake")
    if(EXISTS "${llvm_config_file}")
        message(STATUS "Reusing bootstrapped official LLVM at ${BOOTSTRAP_INSTALL_DIR}")
        return()
    endif()

    find_program(GIT_EXECUTABLE git REQUIRED)
    if(NOT EXISTS "${llvm_source_dir}/llvm/CMakeLists.txt")
        file(MAKE_DIRECTORY "${llvm_source_dir}")
        execute_process(
            COMMAND "${GIT_EXECUTABLE}" clone --depth 1 --branch "${BOOTSTRAP_TAG}" "${BOOTSTRAP_REPOSITORY}" "${llvm_source_dir}"
            RESULT_VARIABLE git_clone_result
        )
        if(NOT git_clone_result EQUAL 0)
            message(FATAL_ERROR "Failed to clone official LLVM from ${BOOTSTRAP_REPOSITORY} tag ${BOOTSTRAP_TAG}")
        endif()
    else()
        message(STATUS "Using existing llvm-project source: ${llvm_source_dir}")
    endif()

    string(REPLACE ";" "\\;" llvm_targets "${BOOTSTRAP_TARGETS}")

    set(bootstrap_c_flags "${CMAKE_C_FLAGS} $ENV{CFLAGS}")
    set(bootstrap_cxx_flags "${CMAKE_CXX_FLAGS} $ENV{CXXFLAGS}")
    string(REPLACE "-Qunused-arguments" "" bootstrap_c_flags "${bootstrap_c_flags}")
    string(REPLACE "-Qunused-arguments" "" bootstrap_cxx_flags "${bootstrap_cxx_flags}")
    string(STRIP "${bootstrap_c_flags}" bootstrap_c_flags)
    string(STRIP "${bootstrap_cxx_flags}" bootstrap_cxx_flags)

    set(bootstrap_compiler_args)
    if(NOT MSVC)
        find_program(BOOTSTRAP_CLANG_EXECUTABLE NAMES clang)
        find_program(BOOTSTRAP_CLANGXX_EXECUTABLE NAMES clang++)
        if(BOOTSTRAP_CLANG_EXECUTABLE AND BOOTSTRAP_CLANGXX_EXECUTABLE)
            list(APPEND bootstrap_compiler_args
                -DCMAKE_C_COMPILER=${BOOTSTRAP_CLANG_EXECUTABLE}
                -DCMAKE_CXX_COMPILER=${BOOTSTRAP_CLANGXX_EXECUTABLE}
            )
            message(STATUS "Bootstrapping LLVM with clang toolchain: ${BOOTSTRAP_CLANG_EXECUTABLE}, ${BOOTSTRAP_CLANGXX_EXECUTABLE}")
        endif()
    endif()

    set(configure_args
        -S "${llvm_source_dir}/llvm"
        -B "${BOOTSTRAP_BUILD_DIR}"
        -G "${BOOTSTRAP_GENERATOR}"
        -DCMAKE_BUILD_TYPE=Release
        -DCMAKE_INSTALL_PREFIX=${BOOTSTRAP_INSTALL_DIR}
        -DCMAKE_C_FLAGS=${bootstrap_c_flags}
        -DCMAKE_CXX_FLAGS=${bootstrap_cxx_flags}
        -DLLVM_ENABLE_PROJECTS=
        -DLLVM_TARGETS_TO_BUILD=${llvm_targets}
        -DLLVM_INCLUDE_BENCHMARKS=OFF
        -DLLVM_INCLUDE_EXAMPLES=OFF
        -DLLVM_INCLUDE_TESTS=OFF
        -DLLVM_INCLUDE_UTILS=OFF
        -DLLVM_ENABLE_TERMINFO=OFF
        -DLLVM_ENABLE_ZLIB=OFF
        -DLLVM_ENABLE_ZSTD=OFF
        -DLLVM_BUILD_TOOLS=OFF
        -DLLVM_BUILD_LLVM_DYLIB=ON
        -DLLVM_LINK_LLVM_DYLIB=ON
    )
    execute_process(
        COMMAND "${CMAKE_COMMAND}" ${configure_args} ${bootstrap_compiler_args}
        RESULT_VARIABLE configure_result
    )
    if(NOT configure_result EQUAL 0)
        message(FATAL_ERROR "Failed to configure official LLVM build.")
    endif()

    execute_process(
        COMMAND "${CMAKE_COMMAND}" --build "${BOOTSTRAP_BUILD_DIR}" --target install --parallel
        RESULT_VARIABLE build_result
    )
    if(NOT build_result EQUAL 0)
        message(FATAL_ERROR "Failed to build/install official LLVM.")
    endif()
endfunction()
