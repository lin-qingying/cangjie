cmake_minimum_required(VERSION 3.16)
project(cangjie_llvm_jni LANGUAGES C CXX)

# IDE-only CMake model for navigation. Actual native build is driven by Gradle nativeCompile plugin.
add_library(cangjie_llvm_jni SHARED
    jni_context.cpp
    jni_module.cpp
    jni_types.cpp
    jni_values.cpp
    jni_builder.cpp
    jni_analysis.cpp
    jni_bitwriter.cpp
)

target_compile_features(cangjie_llvm_jni PRIVATE cxx_std_17)
