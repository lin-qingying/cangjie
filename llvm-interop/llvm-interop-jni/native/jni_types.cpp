// JNI 类型系统相关桥接实现。
#include "jni_utils.h"

#include <llvm-c/Core.h>
#include <vector>

extern "C" JNIEXPORT jlong JNICALL Java_org_cangnova_cangjie_llvm_jni_LlvmNative_intTypeInContext(
    JNIEnv*,
    jclass,
    jlong context,
    jint bits
) {
    return ptr_to_jlong(LLVMIntTypeInContext(jlong_to_ptr<LLVMOpaqueContext>(context), static_cast<unsigned>(bits)));
}

extern "C" JNIEXPORT jlong JNICALL Java_org_cangnova_cangjie_llvm_jni_LlvmNative_floatTypeInContext(
    JNIEnv*,
    jclass,
    jlong context
) {
    return ptr_to_jlong(LLVMFloatTypeInContext(jlong_to_ptr<LLVMOpaqueContext>(context)));
}

extern "C" JNIEXPORT jlong JNICALL Java_org_cangnova_cangjie_llvm_jni_LlvmNative_doubleTypeInContext(
    JNIEnv*,
    jclass,
    jlong context
) {
    return ptr_to_jlong(LLVMDoubleTypeInContext(jlong_to_ptr<LLVMOpaqueContext>(context)));
}

extern "C" JNIEXPORT jlong JNICALL Java_org_cangnova_cangjie_llvm_jni_LlvmNative_voidTypeInContext(
    JNIEnv*,
    jclass,
    jlong context
) {
    return ptr_to_jlong(LLVMVoidTypeInContext(jlong_to_ptr<LLVMOpaqueContext>(context)));
}

extern "C" JNIEXPORT jlong JNICALL Java_org_cangnova_cangjie_llvm_jni_LlvmNative_ptrTypeInContext(
    JNIEnv*,
    jclass,
    jlong context
) {
    return ptr_to_jlong(LLVMPointerType(LLVMInt8TypeInContext(jlong_to_ptr<LLVMOpaqueContext>(context)), 0));
}

extern "C" JNIEXPORT jlong JNICALL Java_org_cangnova_cangjie_llvm_jni_LlvmNative_functionType(
    JNIEnv* env,
    jclass,
    jlong return_type,
    jlongArray param_types,
    jboolean is_var_arg
) {
    jsize size = env->GetArrayLength(param_types);
    std::vector<LLVMTypeRef> params(static_cast<size_t>(size));
    jlong* raw = env->GetLongArrayElements(param_types, nullptr);
    for (jsize i = 0; i < size; ++i) {
        params[static_cast<size_t>(i)] = jlong_to_ptr<LLVMOpaqueType>(raw[i]);
    }
    env->ReleaseLongArrayElements(param_types, raw, JNI_ABORT);

    return ptr_to_jlong(
        LLVMFunctionType(
            jlong_to_ptr<LLVMOpaqueType>(return_type),
            params.data(),
            static_cast<unsigned>(size),
            is_var_arg == JNI_TRUE ? 1 : 0
        )
    );
}

extern "C" JNIEXPORT jlong JNICALL Java_org_cangnova_cangjie_llvm_jni_LlvmNative_namedStructTypeInContext(
    JNIEnv* env,
    jclass,
    jlong context,
    jstring name
) {
    JniUtfChars struct_name(env, name);
    return ptr_to_jlong(
        LLVMStructCreateNamed(jlong_to_ptr<LLVMOpaqueContext>(context), struct_name.c_str())
    );
}

extern "C" JNIEXPORT void JNICALL Java_org_cangnova_cangjie_llvm_jni_LlvmNative_structSetBody(
    JNIEnv* env,
    jclass,
    jlong type,
    jlongArray element_types,
    jboolean is_packed
) {
    jsize size = env->GetArrayLength(element_types);
    std::vector<LLVMTypeRef> elements(static_cast<size_t>(size));
    jlong* raw = env->GetLongArrayElements(element_types, nullptr);
    for (jsize i = 0; i < size; ++i) {
        elements[static_cast<size_t>(i)] = jlong_to_ptr<LLVMOpaqueType>(raw[i]);
    }
    env->ReleaseLongArrayElements(element_types, raw, JNI_ABORT);
    LLVMStructSetBody(
        jlong_to_ptr<LLVMOpaqueType>(type),
        elements.data(),
        static_cast<unsigned>(size),
        is_packed == JNI_TRUE ? 1 : 0
    );
}

extern "C" JNIEXPORT jlong JNICALL Java_org_cangnova_cangjie_llvm_jni_LlvmNative_arrayType(
    JNIEnv*,
    jclass,
    jlong element_type,
    jint size
) {
    return ptr_to_jlong(LLVMArrayType(jlong_to_ptr<LLVMOpaqueType>(element_type), static_cast<unsigned>(size)));
}
