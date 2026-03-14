#include "jni_utils.h"

#include <llvm-c/Analysis.h>
#include <llvm-c/Core.h>

extern "C" JNIEXPORT void JNICALL Java_org_cangnova_cangjie_llvm_jni_LlvmNative_verifyFunction(
    JNIEnv* env,
    jclass,
    jlong function
) {
    int failed = LLVMVerifyFunction(jlong_to_ptr<LLVMOpaqueValue>(function), LLVMReturnStatusAction);
    if (!failed) return;
    throw_llvm_exception(env, "function verification failed");
}
