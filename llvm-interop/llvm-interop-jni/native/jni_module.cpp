#include "jni_utils.h"

#include <llvm-c/Analysis.h>
#include <llvm-c/Core.h>

extern "C" JNIEXPORT jlong JNICALL Java_org_cangnova_cangjie_llvm_jni_LlvmNative_moduleCreateInContext(
    JNIEnv* env,
    jclass,
    jstring name,
    jlong context
) {
    JniUtfChars module_name(env, name);
    auto* llvm_context = jlong_to_ptr<LLVMOpaqueContext>(context);
    return ptr_to_jlong(LLVMModuleCreateWithNameInContext(module_name.c_str(), llvm_context));
}

extern "C" JNIEXPORT void JNICALL Java_org_cangnova_cangjie_llvm_jni_LlvmNative_moduleDispose(
    JNIEnv*,
    jclass,
    jlong module
) {
    if (module == 0) return;
    LLVMDisposeModule(jlong_to_ptr<LLVMOpaqueModule>(module));
}

extern "C" JNIEXPORT void JNICALL Java_org_cangnova_cangjie_llvm_jni_LlvmNative_moduleSetTargetTriple(
    JNIEnv* env,
    jclass,
    jlong module,
    jstring target_triple
) {
    JniUtfChars triple(env, target_triple);
    LLVMSetTarget(jlong_to_ptr<LLVMOpaqueModule>(module), triple.c_str());
}

extern "C" JNIEXPORT void JNICALL Java_org_cangnova_cangjie_llvm_jni_LlvmNative_moduleSetDataLayout(
    JNIEnv* env,
    jclass,
    jlong module,
    jstring data_layout
) {
    JniUtfChars layout(env, data_layout);
    LLVMSetDataLayout(jlong_to_ptr<LLVMOpaqueModule>(module), layout.c_str());
}

extern "C" JNIEXPORT jlong JNICALL Java_org_cangnova_cangjie_llvm_jni_LlvmNative_moduleAddFunction(
    JNIEnv* env,
    jclass,
    jlong module,
    jstring name,
    jlong function_type
) {
    JniUtfChars function_name(env, name);
    return ptr_to_jlong(
        LLVMAddFunction(
            jlong_to_ptr<LLVMOpaqueModule>(module),
            function_name.c_str(),
            jlong_to_ptr<LLVMOpaqueType>(function_type)
        )
    );
}

extern "C" JNIEXPORT jlong JNICALL Java_org_cangnova_cangjie_llvm_jni_LlvmNative_moduleAddGlobal(
    JNIEnv* env,
    jclass,
    jlong module,
    jlong type,
    jstring name
) {
    JniUtfChars global_name(env, name);
    return ptr_to_jlong(
        LLVMAddGlobal(
            jlong_to_ptr<LLVMOpaqueModule>(module),
            jlong_to_ptr<LLVMOpaqueType>(type),
            global_name.c_str()
        )
    );
}

extern "C" JNIEXPORT jstring JNICALL Java_org_cangnova_cangjie_llvm_jni_LlvmNative_modulePrintToString(
    JNIEnv* env,
    jclass,
    jlong module
) {
    char* text = LLVMPrintModuleToString(jlong_to_ptr<LLVMOpaqueModule>(module));
    jstring result = env->NewStringUTF(text == nullptr ? "" : text);
    LLVMDisposeMessage(text);
    return result;
}

extern "C" JNIEXPORT void JNICALL Java_org_cangnova_cangjie_llvm_jni_LlvmNative_moduleVerify(
    JNIEnv* env,
    jclass,
    jlong module
) {
    char* message = nullptr;
    int failed = LLVMVerifyModule(
        jlong_to_ptr<LLVMOpaqueModule>(module),
        LLVMReturnStatusAction,
        &message
    );
    if (!failed) return;
    std::string error = message == nullptr ? "module verification failed" : message;
    LLVMDisposeMessage(message);
    throw_llvm_exception(env, error);
}
