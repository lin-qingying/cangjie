// JNI bitcode 写出相关桥接实现。
#include "jni_utils.h"

#include <llvm-c/BitWriter.h>
#include <llvm-c/Core.h>
#include <llvm-c/Support.h>

extern "C" JNIEXPORT jint JNICALL Java_org_cangnova_cangjie_llvm_jni_LlvmNative_writeBitcodeToFile(
    JNIEnv* env,
    jclass,
    jlong module,
    jstring output_path
) {
    JniUtfChars path(env, output_path);
    return static_cast<jint>(
        LLVMWriteBitcodeToFile(jlong_to_ptr<LLVMOpaqueModule>(module), path.c_str())
    );
}

extern "C" JNIEXPORT jbyteArray JNICALL Java_org_cangnova_cangjie_llvm_jni_LlvmNative_writeBitcodeToMemoryBuffer(
    JNIEnv* env,
    jclass,
    jlong module
) {
    auto* buffer = LLVMWriteBitcodeToMemoryBuffer(jlong_to_ptr<LLVMOpaqueModule>(module));
    if (buffer == nullptr) {
        throw_llvm_exception(env, "failed to write bitcode to memory buffer");
        return nullptr;
    }

    const char* start = LLVMGetBufferStart(buffer);
    size_t size = LLVMGetBufferSize(buffer);
    auto result = env->NewByteArray(static_cast<jsize>(size));
    if (result != nullptr && size > 0) {
        env->SetByteArrayRegion(result, 0, static_cast<jsize>(size), reinterpret_cast<const jbyte*>(start));
    }
    LLVMDisposeMemoryBuffer(buffer);
    return result;
}
