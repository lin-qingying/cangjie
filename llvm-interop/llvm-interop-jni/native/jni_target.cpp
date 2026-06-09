// JNI 目标机器与目标文件生成桥接实现。
#include "jni_utils.h"

#include <llvm-c/Core.h>
#include <llvm-c/Support.h>
#include <llvm-c/Target.h>
#include <llvm-c/TargetMachine.h>

extern "C" JNIEXPORT void JNICALL Java_org_cangnova_cangjie_llvm_jni_LlvmNative_targetInitializeAll(
    JNIEnv*,
    jclass
) {
    LLVMInitializeAllTargetInfos();
    LLVMInitializeAllTargets();
    LLVMInitializeAllTargetMCs();
    LLVMInitializeAllAsmParsers();
    LLVMInitializeAllAsmPrinters();
}

extern "C" JNIEXPORT jstring JNICALL Java_org_cangnova_cangjie_llvm_jni_LlvmNative_targetDefaultTriple(
    JNIEnv* env,
    jclass
) {
    char* triple = LLVMGetDefaultTargetTriple();
    jstring result = env->NewStringUTF(triple == nullptr ? "" : triple);
    LLVMDisposeMessage(triple);
    return result;
}

extern "C" JNIEXPORT jlong JNICALL Java_org_cangnova_cangjie_llvm_jni_LlvmNative_targetCreateMachine(
    JNIEnv* env,
    jclass,
    jstring target_triple,
    jstring cpu,
    jstring features,
    jint optimization_level,
    jint relocation_mode,
    jint code_model
) {
    JniUtfChars triple(env, target_triple);
    JniUtfChars cpu_name(env, cpu);
    JniUtfChars feature_list(env, features);

    LLVMTargetRef target = nullptr;
    char* message = nullptr;
    if (LLVMGetTargetFromTriple(triple.c_str(), &target, &message) != 0) {
        std::string error = message == nullptr ? "failed to resolve LLVM target triple" : message;
        if (message != nullptr) {
            LLVMDisposeMessage(message);
        }
        throw_llvm_exception(env, error);
        return 0;
    }

    LLVMTargetMachineRef machine = LLVMCreateTargetMachine(
        target,
        triple.c_str(),
        cpu_name.c_str(),
        feature_list.c_str(),
        static_cast<LLVMCodeGenOptLevel>(optimization_level),
        static_cast<LLVMRelocMode>(relocation_mode),
        static_cast<LLVMCodeModel>(code_model)
    );
    if (machine == nullptr) {
        throw_llvm_exception(env, "failed to create LLVM target machine");
        return 0;
    }
    return ptr_to_jlong(machine);
}

extern "C" JNIEXPORT void JNICALL Java_org_cangnova_cangjie_llvm_jni_LlvmNative_targetDisposeMachine(
    JNIEnv*,
    jclass,
    jlong target_machine
) {
    if (target_machine == 0) return;
    LLVMDisposeTargetMachine(jlong_to_ptr<LLVMOpaqueTargetMachine>(target_machine));
}

extern "C" JNIEXPORT void JNICALL Java_org_cangnova_cangjie_llvm_jni_LlvmNative_targetMachineEmitObjectFile(
    JNIEnv* env,
    jclass,
    jlong target_machine,
    jlong module,
    jstring output_path
) {
    JniUtfChars path(env, output_path);
    char* message = nullptr;
    const int failed = LLVMTargetMachineEmitToFile(
        jlong_to_ptr<LLVMOpaqueTargetMachine>(target_machine),
        jlong_to_ptr<LLVMOpaqueModule>(module),
        const_cast<char*>(path.c_str()),
        LLVMObjectFile,
        &message
    );
    if (!failed) return;

    std::string error = message == nullptr ? "failed to emit LLVM object file" : message;
    if (message != nullptr) {
        LLVMDisposeMessage(message);
    }
    throw_llvm_exception(env, error);
}

extern "C" JNIEXPORT jbyteArray JNICALL Java_org_cangnova_cangjie_llvm_jni_LlvmNative_targetMachineEmitObjectBytes(
    JNIEnv* env,
    jclass,
    jlong target_machine,
    jlong module
) {
    char* message = nullptr;
    LLVMMemoryBufferRef buffer = nullptr;
    const int failed = LLVMTargetMachineEmitToMemoryBuffer(
        jlong_to_ptr<LLVMOpaqueTargetMachine>(target_machine),
        jlong_to_ptr<LLVMOpaqueModule>(module),
        LLVMObjectFile,
        &message,
        &buffer
    );
    if (failed) {
        std::string error = message == nullptr ? "failed to emit LLVM object bytes" : message;
        if (message != nullptr) {
            LLVMDisposeMessage(message);
        }
        throw_llvm_exception(env, error);
        return nullptr;
    }

    const size_t size = LLVMGetBufferSize(buffer);
    const char* start = LLVMGetBufferStart(buffer);
    jbyteArray result = env->NewByteArray(static_cast<jsize>(size));
    if (result != nullptr && size > 0) {
        env->SetByteArrayRegion(
            result,
            0,
            static_cast<jsize>(size),
            reinterpret_cast<const jbyte*>(start)
        );
    }
    LLVMDisposeMemoryBuffer(buffer);
    return result;
}
