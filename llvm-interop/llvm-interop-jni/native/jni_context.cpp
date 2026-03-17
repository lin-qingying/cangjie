// JNI 上下文相关桥接实现。
#include "jni_utils.h"

#include <llvm-c/Core.h>

extern "C" JNIEXPORT jlong JNICALL Java_org_cangnova_cangjie_llvm_jni_LlvmNative_contextCreate(
    JNIEnv*,
    jclass
) {
    return ptr_to_jlong(LLVMContextCreate());
}

extern "C" JNIEXPORT void JNICALL Java_org_cangnova_cangjie_llvm_jni_LlvmNative_contextDispose(
    JNIEnv*,
    jclass,
    jlong context
) {
    if (context == 0) return;
    LLVMContextDispose(jlong_to_ptr<LLVMOpaqueContext>(context));
}
