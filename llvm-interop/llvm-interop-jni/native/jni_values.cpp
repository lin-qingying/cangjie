#include "jni_utils.h"

#include <llvm-c/Core.h>

extern "C" JNIEXPORT jlong JNICALL Java_org_cangnova_cangjie_llvm_jni_LlvmNative_constInt(
    JNIEnv*,
    jclass,
    jlong type,
    jlong value,
    jboolean sign_extend
) {
    return ptr_to_jlong(
        LLVMConstInt(
            jlong_to_ptr<LLVMOpaqueType>(type),
            static_cast<unsigned long long>(value),
            sign_extend == JNI_TRUE ? 1 : 0
        )
    );
}

extern "C" JNIEXPORT jlong JNICALL Java_org_cangnova_cangjie_llvm_jni_LlvmNative_constReal(
    JNIEnv*,
    jclass,
    jlong type,
    jdouble value
) {
    return ptr_to_jlong(LLVMConstReal(jlong_to_ptr<LLVMOpaqueType>(type), value));
}

extern "C" JNIEXPORT jlong JNICALL Java_org_cangnova_cangjie_llvm_jni_LlvmNative_constNull(
    JNIEnv*,
    jclass,
    jlong type
) {
    return ptr_to_jlong(LLVMConstNull(jlong_to_ptr<LLVMOpaqueType>(type)));
}

extern "C" JNIEXPORT jstring JNICALL Java_org_cangnova_cangjie_llvm_jni_LlvmNative_valueGetName(
    JNIEnv* env,
    jclass,
    jlong value
) {
    const char* name = LLVMGetValueName(jlong_to_ptr<LLVMOpaqueValue>(value));
    return env->NewStringUTF(name == nullptr ? "" : name);
}

extern "C" JNIEXPORT jlong JNICALL Java_org_cangnova_cangjie_llvm_jni_LlvmNative_valueGetType(
    JNIEnv*,
    jclass,
    jlong value
) {
    return ptr_to_jlong(LLVMTypeOf(jlong_to_ptr<LLVMOpaqueValue>(value)));
}
