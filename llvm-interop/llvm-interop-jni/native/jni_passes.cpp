// JNI PassBuilder 桥接实现。
#include "jni_utils.h"

#include <llvm-c/Core.h>
#include <llvm-c/Error.h>
#include <llvm-c/TargetMachine.h>
#include <llvm-c/Transforms/PassBuilder.h>

extern "C" JNIEXPORT void JNICALL Java_org_cangnova_cangjie_llvm_jni_LlvmNative_moduleRunPasses(
    JNIEnv* env,
    jclass,
    jlong module,
    jstring pass_pipeline,
    jlong target_machine
) {
    JniUtfChars pipeline(env, pass_pipeline);
    auto* options = LLVMCreatePassBuilderOptions();
    LLVMErrorRef error = LLVMRunPasses(
        jlong_to_ptr<LLVMOpaqueModule>(module),
        pipeline.c_str(),
        target_machine == 0 ? nullptr : jlong_to_ptr<LLVMOpaqueTargetMachine>(target_machine),
        options
    );
    LLVMDisposePassBuilderOptions(options);

    if (error == nullptr) return;

    char* message = LLVMGetErrorMessage(error);
    std::string error_text = message == nullptr ? "failed to run LLVM pass pipeline" : message;
    if (message != nullptr) {
        LLVMDisposeErrorMessage(message);
    }
    throw_llvm_exception(env, error_text);
}
